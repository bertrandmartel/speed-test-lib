/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016 Bertrand Martel
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package fr.bmartel.speedtest;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.*;

/**
 * Client socket main implementation.
 * <p/>
 * Two modes upload and download
 * <p/>
 * upload will write a file to a specific host with given uri. The file is
 * randomly generated with a given size
 * <p/>
 * download will retrieve a content from a specific host with given uri.
 * <p/>
 * For both mode, transfer rate is calculated independently from socket initial
 * connection
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocket implements ISpeedTestSocket {

    /**
     * BigDecimal scale used in transfer rate calculation.
     */
    private int scale = SpeedTestConst.DEFAULT_SCALE;

    /**
     * BigDecimal RoundingMode used in transfer rate calculation.
     */
    private RoundingMode roundingMode = SpeedTestConst.DEFAULT_ROUNDING_MODE;

    /**
     * socket server hostname.
     */
    private String hostname = "";

    /**
     * socket server port.
     */
    private int port;

    /**
     * socket object.
     */
    private Socket socket;

    /**
     * speed test listener list.
     */
    private final List<ISpeedTestListener> listenerList = new ArrayList<>();

    /**
     * this is the size of each data sent to upload server.
     */
    private int uploadChunkSize = SpeedTestConst.DEFAULT_UPLOAD_SIZE;

    /**
     * socket timeout.
     */
    private int socketTimeout = SpeedTestConst.DEFAULT_SOCKET_TIMEOUT;

    /**
     * define if socket close error is to be expected.
     */
    private boolean forceCloseSocket;

    /**
     * executor service.
     */
    private ScheduledExecutorService executorService;

    /**
     * executor service used for reporting.
     */
    private ScheduledExecutorService reportExecutorService;

    /**
     * size of file to upload.
     */
    private BigDecimal uploadFileSize = BigDecimal.ZERO;

    /**
     * start time triggered in millis.
     */
    private long timeStart;

    /**
     * end time triggered in millis.
     */
    private long timeEnd;

    /**
     * current speed test mode.
     */
    private SpeedTestMode speedTestMode = SpeedTestMode.NONE;

    /**
     * this is the number of bit uploaded at this time.
     */
    private int uploadTempFileSize;

    /**
     * this is the number of packet dowloaded at this time.
     */
    private int downloadTemporaryPacketSize;

    /**
     * this is the number of packet to download.
     */
    private BigDecimal downloadPckSize = BigDecimal.ZERO;

    /**
     * define if an error has been dispatched already or not. This is reset to false on start download/ upload + in
     * reading thread
     */
    private boolean errorDispatched;

    /**
     * Speed test repeat wrapper.
     */
    private final RepeatWrapper repeatWrapper;

    /**
     * Build Client socket.
     */
    public SpeedTestSocket() {
        initThreadPool();
        repeatWrapper = new RepeatWrapper(this);
    }

    /**
     * Add a speed test listener to list.
     *
     * @param listener speed test listener to be added
     */
    @Override
    public void addSpeedTestListener(final ISpeedTestListener listener) {
        listenerList.add(listener);
    }

    /**
     * Relive a speed listener from list.
     *
     * @param listener speed test listener to be removed
     */
    @Override
    public void removeSpeedTestListener(final ISpeedTestListener listener) {
        listenerList.remove(listener);
    }

    /**
     * initialize thread pool.
     */
    private void initThreadPool() {
        executorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_SIZE);
        reportExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_REPORT_SIZE);
    }

    /**
     * Create and connect socket.
     *
     * @param task       task to be executed when connected to socket
     * @param isDownload define if it is a download or upload test
     */
    private void connectAndExecuteTask(final TimerTask task, final boolean isDownload) {

        // close socket before recreating it
        if (socket != null) {
            closeSocket();
        }
        try {
            /* create a basic socket connection */
            socket = new Socket();

            if (socketTimeout != 0 && isDownload) {
                socket.setSoTimeout(socketTimeout);
            }

			/* establish socket parameters */
            socket.setReuseAddress(true);

            socket.setKeepAlive(true);

            socket.connect(new InetSocketAddress(hostname, port));

            if (!executorService.isShutdown()) {
                executorService.execute(new Runnable() {

                    @Override
                    public void run() {

                        if (isDownload) {
                            startSocketDownloadTask();
                        } else {
                            startSocketUploadTask();
                        }
                        speedTestMode = SpeedTestMode.NONE;
                    }
                });
            }

            if (task != null) {
                task.run();
            }
        } catch (IOException e) {
            if (!errorDispatched) {
                SpeedTestUtils.dispatchError(forceCloseSocket, listenerList, isDownload, e.getMessage());
            }
        }
    }

    /**
     * start download reading task.
     */
    private void startSocketDownloadTask() {

        downloadTemporaryPacketSize = 0;

        try {
            final HttpFrame httpFrame = new HttpFrame();

            timeStart = System.currentTimeMillis();
            timeEnd = 0;

            if (repeatWrapper.isFirstDownload()) {
                repeatWrapper.setFirstDownloadRepeat(false);
                repeatWrapper.setStartDate(timeStart);
            }

            final HttpStates httFrameState = httpFrame.decodeFrame(socket.getInputStream());
            SpeedTestUtils.checkHttpFrameError(forceCloseSocket, listenerList, httFrameState);

            final HttpStates httpHeaderState = httpFrame.parseHeader(socket.getInputStream());
            SpeedTestUtils.checkHttpHeaderError(forceCloseSocket, listenerList, httpHeaderState);

            SpeedTestUtils.checkHttpContentLengthError(forceCloseSocket, listenerList, httpFrame);

            downloadPckSize = new BigDecimal(httpFrame.getContentLength());

            if (repeatWrapper.isRepeatDownload()) {
                repeatWrapper.updatePacketSize(downloadPckSize);
            }
            downloadReadingLoop();
            timeEnd = System.currentTimeMillis();

            closeSocket();

            final SpeedTestReport report = getLiveDownloadReport();

            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onDownloadFinished(report);
            }

            if (!repeatWrapper.isRepeatDownload()) {
                executorService.shutdownNow();
            }

        } catch (SocketTimeoutException e) {
            SpeedTestUtils.dispatchSocketTimeout(forceCloseSocket, listenerList, true, e.getMessage());
            timeEnd = System.currentTimeMillis();
            closeSocket();
            executorService.shutdownNow();
        } catch (IOException | InterruptedException e) {
            catchError(true, e.getMessage());
        }
        errorDispatched = false;
    }

    /**
     * Shutdown threadpool and wait for task completion.
     */
    @Override
    public void shutdownAndWait() {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(SpeedTestConst.THREADPOOL_WAIT_COMPLETION_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    /**
     * start download reading loop + monitor progress.
     *
     * @throws IOException socket io exception
     */
    private void downloadReadingLoop() throws IOException {

        final byte[] buffer = new byte[SpeedTestConst.READ_BUFFER_SIZE];
        int read;

        while ((read = socket.getInputStream().read(buffer)) != -1) {

            downloadTemporaryPacketSize += read;

            if (repeatWrapper.isRepeatDownload()) {
                repeatWrapper.updateTempPacketSize(read);
            }

            final SpeedTestReport report = getLiveDownloadReport();

            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onDownloadProgress(report.getProgressPercent(), report);
            }

            if (downloadTemporaryPacketSize == downloadPckSize.longValueExact()) {
                break;
            }
        }
    }

    /**
     * start upload writing task.
     */
    private void startSocketUploadTask() {

        try {
            final HttpFrame frame = new HttpFrame();

            final HttpStates httpStates = frame.parseHttp(socket.getInputStream());

            if (httpStates == HttpStates.HTTP_FRAME_OK) {

                if (frame.getStatusCode() == SpeedTestConst.HTTP_OK && frame.getReasonPhrase().equalsIgnoreCase("ok")) {

                    timeEnd = System.currentTimeMillis();

                    closeSocket();

                    final SpeedTestReport report = getLiveUploadReport();

                    for (int i = 0; i < listenerList.size(); i++) {
                        listenerList.get(i).onUploadFinished(report);
                    }

                } else {
                    closeSocket();
                }

                if (!repeatWrapper.isRepeatUpload()) {
                    executorService.shutdownNow();
                }

                return;
            }
            closeSocket();
            if (!errorDispatched && !forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.SOCKET_ERROR, "socket error");
                }
            }
            executorService.shutdownNow();

        } catch (IOException | InterruptedException e) {
            if (!errorDispatched) {
                catchError(false, e.getMessage());
            }
        }
        errorDispatched = false;
    }

    /**
     * start download task.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    private void startDownloadRequest(final String hostname, final int port, final String uri) {
        forceCloseSocket = false;
        this.hostname = hostname;
        this.port = port;
        final String downloadRequest = "GET " + uri + " HTTP/1.1\r\n" + "Host: " + hostname + "\r\n\r\n";
        writeDownload(downloadRequest.getBytes());
    }

    /**
     * Start download process with a fixed duration.
     *
     * @param hostname    server hostname
     * @param port        server port
     * @param uri         uri to fetch to download file
     * @param maxDuration maximum duration of the speed test in milliseconds
     */
    public void startDownload(final String hostname, final int port, final String uri, final int maxDuration) {

        reportExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                forceStopTask();
            }
        }, maxDuration, TimeUnit.MILLISECONDS);

        startDownload(hostname, port, uri);
    }

    /**
     * Start download process.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    @Override
    public void startDownload(final String hostname, final int port, final String uri) {
        errorDispatched = false;
        startDownloadRequest(hostname, port, uri);
    }

    /**
     * close socket + shutdown thread pool.
     */
    @Override
    public void forceStopTask() {
        forceCloseSocket = true;
        speedTestMode = SpeedTestMode.NONE;
        closeSocket();
        shutdownAndWait();
    }

    /**
     * Write download request to server host.
     *
     * @param data HTTP request to send to initiate download process
     */
    private void writeDownload(final byte[] data) {

        speedTestMode = SpeedTestMode.DOWNLOAD;

        if (executorService == null || executorService.isShutdown()) {
            initThreadPool();
        }

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {

                if (socket != null && !socket.isClosed()) {

                    try {
                        if ((socket.getOutputStream() != null) && (writeFlushSocket(data) != 0)) {
                            throw new SocketTimeoutException();
                        }
                    } catch (SocketTimeoutException e) {
                        SpeedTestUtils.dispatchSocketTimeout(forceCloseSocket, listenerList,
                                true, SpeedTestConst.SOCKET_WRITE_ERROR);
                        closeSocket();
                        executorService.shutdownNow();
                    } catch (IOException e) {
                        SpeedTestUtils.dispatchError(forceCloseSocket, listenerList, true, e.getMessage());
                        executorService.shutdownNow();
                    }
                }
            }
        }, true);
    }

    /**
     * Start upload process.
     *
     * @param hostname      server hostname
     * @param port          server port
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     * @param maxDuration   maxumum duration of speed test in milliseconds
     */
    public void startUpload(final String hostname,
                            final int port,
                            final String uri,
                            final int fileSizeOctet,
                            final int maxDuration) {

        reportExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                forceStopTask();
            }
        }, maxDuration, TimeUnit.MILLISECONDS);

        startUpload(hostname, port, uri, fileSizeOctet);
    }

    /**
     * Start upload process.
     *
     * @param hostname      server hostname
     * @param port          server port
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     */
    @Override
    public void startUpload(final String hostname, final int port, final String uri, final int fileSizeOctet) {

        this.hostname = hostname;
        this.port = port;
        uploadFileSize = new BigDecimal(fileSizeOctet);
        forceCloseSocket = false;
        errorDispatched = false;

        speedTestMode = SpeedTestMode.UPLOAD;

        /* generate a file with size of fileSizeOctet octet */
        final byte[] fileContent = new RandomGen(fileSizeOctet).nextArray();

        final String uploadRequest = "POST " + uri + " HTTP/1.1\r\n" + "Host: " + hostname + "\r\nAccept: " +
                "*/*\r\nContent-Length: " + fileSizeOctet + "\r\n\r\n";

        new Thread(new Runnable() {
            @Override
            public void run() {
                writeUpload(uploadRequest.getBytes(), fileContent);
            }
        }).start();
    }

    /**
     * Write upload POST request with file generated randomly.
     *
     * @param head http headers
     * @param body file content to upload
     */
    private void writeUpload(final byte[] head, final byte[] body) {

        if (executorService == null || executorService.isShutdown()) {
            initThreadPool();
        }

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {
                if (socket != null && !socket.isClosed()) {
                    try {

                        uploadTempFileSize = 0;

                        final int step = body.length / uploadChunkSize;
                        final int remain = body.length % uploadChunkSize;

                        if (socket.getOutputStream() != null) {

                            if (writeFlushSocket(head) != 0) {
                                throw new SocketTimeoutException();
                            }

                            timeStart = System.currentTimeMillis();
                            timeEnd = 0;

                            if (repeatWrapper.isFirstUpload()) {
                                repeatWrapper.setFirstUploadRepeat(false);
                                repeatWrapper.setStartDate(timeStart);
                            }

                            if (repeatWrapper.isRepeatUpload()) {
                                repeatWrapper.updatePacketSize(uploadFileSize);
                            }

                            for (int i = 0; i < step; i++) {

                                if (writeFlushSocket(Arrays.copyOfRange(body, uploadTempFileSize,
                                        uploadTempFileSize +
                                                uploadChunkSize)) != 0) {
                                    throw new SocketTimeoutException();
                                }

                                uploadTempFileSize += uploadChunkSize;

                                if (repeatWrapper.isRepeatUpload()) {
                                    repeatWrapper.updateTempPacketSize(uploadChunkSize);
                                }

                                final SpeedTestReport report = getLiveUploadReport();

                                for (int j = 0; j < listenerList.size(); j++) {

                                    listenerList.get(j).onUploadProgress(report.getProgressPercent(), report);
                                }
                            }
                            if (remain != 0 && writeFlushSocket(Arrays.copyOfRange(body, uploadTempFileSize,
                                    uploadTempFileSize +
                                            remain)) != 0) {
                                throw new SocketTimeoutException();
                            } else {
                                uploadTempFileSize += remain;

                                if (repeatWrapper.isRepeatUpload()) {
                                    repeatWrapper.updateTempPacketSize(remain);
                                }
                            }
                            for (int j = 0; j < listenerList.size(); j++) {
                                listenerList.get(j).onUploadProgress(SpeedTestConst.PERCENT_MAX.floatValue(),
                                        getLiveUploadReport());
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        errorDispatched = true;
                        if (!forceCloseSocket) {
                            SpeedTestUtils.dispatchSocketTimeout(forceCloseSocket, listenerList,
                                    false, SpeedTestConst.SOCKET_WRITE_ERROR);
                        } else {
                            SpeedTestUtils.dispatchError(forceCloseSocket, listenerList, false, e.getMessage());
                        }
                        closeSocket();
                        executorService.shutdownNow();
                    } catch (IOException e) {
                        errorDispatched = true;
                        SpeedTestUtils.dispatchError(forceCloseSocket, listenerList, false, e.getMessage());
                        executorService.shutdownNow();
                    }
                }
            }
        }, false);
    }

    /**
     * write and flush socket.
     *
     * @param data payload to write
     * @return error status (-1 for error)
     * @throws IOException socket io exception
     */
    private int writeFlushSocket(final byte[] data) throws IOException {

        final ExecutorService executor = Executors.newSingleThreadExecutor();

        @SuppressWarnings("unchecked")
        final Future<Integer> future = executor.submit(new Callable() {

            /**
             * execute sequential write/flush task.
             *
             * @return status
             */
            public Integer call() {
                try {
                    socket.getOutputStream().write(data);
                    socket.getOutputStream().flush();
                } catch (IOException e) {
                    return -1;
                }
                return 0;
            }
        });
        int status;
        try {
            status = future.get(socketTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            executor.shutdownNow();
            return -1;
        } catch (InterruptedException | ExecutionException e) {
            executor.shutdownNow();
            return -1;
        }
        executor.shutdownNow();
        return status;
    }

    /**
     * catch an error.
     *
     * @param isDownload   downloading task or uploading task
     * @param errorMessage error message from Exception
     */
    private void catchError(final boolean isDownload, final String errorMessage) {
        SpeedTestUtils.dispatchError(forceCloseSocket, listenerList, isDownload, errorMessage);
        timeEnd = System.currentTimeMillis();
        closeSocket();
        executorService.shutdownNow();
    }

    /**
     * Start repeat download task.
     *
     * @param hostname           server hostname
     * @param port               server port
     * @param uri                uri to fetch to download file
     * @param repeatWindow       time window for the repeated download in milliseconds
     * @param reportPeriodMillis time interval between each report in milliseconds
     * @param repeatListener     listener for download repeat task completion & reports
     */
    public void startDownloadRepeat(final String hostname,
                                    final int port,
                                    final String uri,
                                    final int repeatWindow,
                                    final int reportPeriodMillis,
                                    final IRepeatListener repeatListener) {
        repeatWrapper.startDownloadRepeat(hostname, port, uri, repeatWindow, reportPeriodMillis, repeatListener);
    }

    /**
     * Start repeat upload task.
     *
     * @param hostname           server hostname
     * @param port               server port
     * @param uri                uri to fetch to download file
     * @param repeatWindow       time window for the repeated upload in milliseconds
     * @param reportPeriodMillis time interval between each report in milliseconds
     * @param fileSizeOctet      file size in octet
     * @param repeatListener     listener for upload repeat task completion & reports
     */
    public void startUploadRepeat(final String hostname,
                                  final int port,
                                  final String uri,
                                  final int repeatWindow,
                                  final int reportPeriodMillis,
                                  final int fileSizeOctet,
                                  final IRepeatListener repeatListener) {

        repeatWrapper.startUploadRepeat(hostname,
                port,
                uri,
                repeatWindow,
                reportPeriodMillis,
                fileSizeOctet,
                repeatListener);
    }

    /**
     * get a temporary download report at this moment.
     *
     * @return speed test download report
     */
    @Override
    public SpeedTestReport getLiveDownloadReport() {
        return getReport(SpeedTestMode.DOWNLOAD);
    }

    /**
     * get a temporary upload report at this moment.
     *
     * @return speed test upload report
     */
    @Override
    public SpeedTestReport getLiveUploadReport() {
        return getReport(SpeedTestMode.UPLOAD);
    }

    /**
     * get a download/upload report.
     *
     * @param mode speed test mode requested
     * @return speed test report
     */
    private SpeedTestReport getReport(final SpeedTestMode mode) {

        BigDecimal temporaryPacketSize = BigDecimal.ZERO;
        BigDecimal totalPacketSize = BigDecimal.ZERO;

        switch (mode) {
            case DOWNLOAD:
                temporaryPacketSize = new BigDecimal(downloadTemporaryPacketSize);
                totalPacketSize = downloadPckSize;
                break;
            case UPLOAD:
                temporaryPacketSize = new BigDecimal(uploadTempFileSize);
                totalPacketSize = uploadFileSize;
                break;
            default:
                break;
        }

        long currentTime;
        if (timeEnd == 0) {
            currentTime = System.currentTimeMillis();
        } else {
            currentTime = timeEnd;
        }

        BigDecimal transferRateOps = BigDecimal.ZERO;

        if ((currentTime - timeStart) != 0) {
            transferRateOps = temporaryPacketSize.divide(new BigDecimal(currentTime - timeStart)
                    .divide(SpeedTestConst.MILLIS_DIVIDER, scale, roundingMode), scale, roundingMode);
        }

        final BigDecimal transferRateBitps = transferRateOps.multiply(SpeedTestConst.BIT_MULTIPLIER);

        BigDecimal percent = BigDecimal.ZERO;

        SpeedTestReport report;

        if (repeatWrapper.isRepeat()) {

            report = repeatWrapper.getRepeatReport(scale, roundingMode, mode, currentTime, transferRateOps);

        } else {

            if (totalPacketSize != BigDecimal.ZERO) {

                percent = temporaryPacketSize.multiply(SpeedTestConst.PERCENT_MAX).divide(totalPacketSize, scale,
                        roundingMode);
            }

            report = new SpeedTestReport(mode, percent.floatValue(),
                    timeStart, currentTime, temporaryPacketSize.longValueExact(), totalPacketSize.longValueExact(),
                    transferRateOps, transferRateBitps,
                    1);
        }
        return report;
    }

    /**
     * Close socket streams and socket object.
     */
    @Override
    public void closeSocket() {

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * retrieve current speed test mode.
     *
     * @return speed test mode (UPLOAD/DOWNLOAD/NONE)
     */
    public SpeedTestMode getSpeedTestMode() {
        return speedTestMode;
    }

    /**
     * set socket timeout in millisecond.
     *
     * @param socketTimeoutMillis socket timeout value in milliseconds
     */
    public void setSocketTimeout(final int socketTimeoutMillis) {
        if (socketTimeoutMillis >= 0) {
            socketTimeout = socketTimeoutMillis;
        }
    }

    /**
     * get socket timeout in milliseconds ( 0 if no timeout not defined).
     *
     * @return socket timeout value (0 if not defined)
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * retrieve size of each packet sent to upload server.
     *
     * @return size of each packet sent to upload server
     */
    public int getUploadChunkSize() {
        return uploadChunkSize;
    }

    /**
     * set size of each packet sent to upload server.
     *
     * @param uploadChunkSize new size of each packet sent to upload server
     */
    public void setUploadChunkSize(final int uploadChunkSize) {
        this.uploadChunkSize = uploadChunkSize;
    }

    /**
     * Set the default RoundingMode for BigDecimal.
     *
     * @param roundingMode rounding mode.
     */
    public void setDefaultRoundingMode(final RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }

    /**
     * Set the default scale for BigDecimal.
     *
     * @param scale scale value
     */
    public void setDefaultScale(final int scale) {
        this.scale = scale;
    }

    /**
     * retrieve rounding mode used for BigDecimal.
     *
     * @return rounding mode
     */
    public RoundingMode getDefaultRoundingMode() {
        return roundingMode;
    }

    /**
     * retrieve scale used for BigDecimal.
     *
     * @return scale value
     */
    public int getDefaultScale() {
        return scale;
    }
}
