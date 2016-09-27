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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

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
public class SpeedTestSocket {

    /**
     * size of the write read buffer for downloading.
     */
    private static final int READ_BUFFER_SIZE = 65535;

    /**
     * default size of each packet sent to upload server.
     */
    private static final int DEFAULT_UPLOAD_SIZE = 65535;

    /**
     * default socket timeout in milliseconds.
     */
    private static final int DEFAULT_SOCKET_TIMEOUT = 10000;

    /**
     * time to wait for task to complete when threadpool is shutdown
     */
    private static final int THREADPOOL_WAIT_COMPLETION_MS = 500;

    /**
     * http ok status code.
     */
    private static final int HTTP_OK = 200;

    /**
     * max value for percent.
     */
    private static final BigDecimal PERCENT_MAX = new BigDecimal("100");

    /**
     * millisecond divider.
     */
    private static final BigDecimal MILLIS_DIVIDER = new BigDecimal("1000");

    /**
     * bit multiplier value.
     */
    private static final BigDecimal BIT_MULTIPLIER = new BigDecimal("8");

    /**
     * parsing error message.
     */
    private static final String PARSING_ERROR = "Error occurred while parsing ";

    /**
     * parsing http error message.
     */
    private static final String PARSING_HTTP_ERROR = "Error occurred while parsing http ";

    /**
     * writing socket error message.
     */
    private static final String SOCKET_WRITE_ERROR = "Error occurred while writing to socket";

    /**
     * default scale for BigDecimal.
     */
    private static final int DEFAULT_SCALE = 4;

    /**
     * default rounding mode for BigDecimal.
     */
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;

    /**
     * BigDecimal scale used in transfer rate calculation.
     */
    private int scale = DEFAULT_SCALE;

    /**
     * BigDecimal RoundingMode used in transfer rate calculation.
     */
    private RoundingMode roundingMode = DEFAULT_ROUNDING_MODE;

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
     * speed examples listener list.
     */
    private final List<ISpeedTestListener> listenerList = new ArrayList<ISpeedTestListener>();

    /**
     * this is the size of each data sent to upload server.
     */
    private int uploadChunkSize = DEFAULT_UPLOAD_SIZE;

    /**
     * socket timeout.
     */
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

    /**
     * define if socket close error is to be expected.
     */
    private boolean forceCloseSocket;

    /**
     * max size for thread pool.
     */
    private static final int THREAD_POOL_SIZE = 1;

    /**
     * executor service.
     */
    private ScheduledExecutorService executorService;

    /**
     * force clause related error message.
     */
    private static final String FORCE_CLOSE_CAUSE_MESSAGE = " caused by socket force close";

    /**
     * these variables are not used through accessors due
     * to lack of performance of virtual accessors in Android
     * see http://developer.android.com/training/articles/perf-tips.html#GettersSetters
     */

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
     * current speed examples mode.
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

    /***************************************************
     ********* SPEED TEST DOWNLOAD REPEAT VARIABLES ****
     ***************************************************
     * these variables are not used through accessors due
     * to lack of performance of virtual accessors in Android
     * see http://developer.android.com/training/articles/perf-tips.html#GettersSetters.
     */

    /**
     * define if download should be repeated.
     */
    private boolean isRepeatDownload;

    /**
     * define if upload should be repeated.
     */
    private boolean isRepeatUpload;

    /**
     * start time for download repeat task.
     */
    private long startDateRepeat;

    /**
     * time window for download repeat task.
     */
    private int repeatWindows;

    /**
     * current number of request for download repeat task.
     */
    private int repeatRequestNum;

    /**
     * number of packet pending for download repeat task.
     */
    private BigDecimal repeatPacketSize;

    /**
     * number of packet downloaded for download/upload repeat task.
     */
    private long repeatTempPckSize;

    /**
     * define if the first download repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private boolean isFirstDownloadRepeat;

    /**
     * define if the first upload repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private boolean isFirstUploadRepeat;

    /**
     * transfer rate list.
     */
    private List<BigDecimal> repeatTransferRateList;

    /**
     * define if download repeat task is finished.
     */
    private boolean repeatFinished;

    /**
     * define if an error has been dispatched already or not. This is reset to false on start download/ upload + in
     * reading thread
     */
    private boolean errorDispatched;

    /**
     * Build Client socket.
     */
    public SpeedTestSocket() {
        initThreadPool();
    }

    /**
     * Add a speed examples listener to list.
     *
     * @param listener speed examples listener to be added
     */
    public void addSpeedTestListener(final ISpeedTestListener listener) {
        listenerList.add(listener);
    }

    /**
     * Relive a speed listener from list.
     *
     * @param listener speed examples listener to be removed
     */
    public void removeSpeedTestListener(final ISpeedTestListener listener) {
        listenerList.remove(listener);
    }

    /**
     * initialize thread pool.
     */
    private void initThreadPool() {
        executorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    }

    /**
     * Create and connect socket.
     *
     * @param task       task to be executed when connected to socket
     * @param isDownload define if it is a download or upload examples
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

            if (task != null) {
                task.run();
            }
        } catch (IOException e) {
            if (!errorDispatched) {
                dispatchError(isDownload, e.getMessage());
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

            if (isFirstDownloadRepeat && isRepeatDownload) {
                isFirstDownloadRepeat = false;
                startDateRepeat = timeStart;
            }

            final HttpStates httFrameState = httpFrame.decodeFrame(socket.getInputStream());
            checkHttpFrameError(httFrameState);

            final HttpStates httpHeaderState = httpFrame.parseHeader(socket.getInputStream());
            checkHttpHeaderError(httpHeaderState);

            checkHttpContentLengthError(httpFrame);

            downloadPckSize = new BigDecimal(httpFrame.getContentLength());

            if (isRepeatDownload) {
                repeatPacketSize = repeatPacketSize.add(downloadPckSize);
            }

            downloadReadingLoop();

            timeEnd = System.currentTimeMillis();

            final BigDecimal transferRateOps = downloadPckSize.divide(new BigDecimal(timeEnd -
                    timeStart).divide(MILLIS_DIVIDER), scale, roundingMode);

            final BigDecimal transferRateBps = transferRateOps.multiply(BIT_MULTIPLIER);

            closeSocket();

            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onDownloadPacketsReceived(downloadPckSize.longValueExact(), transferRateBps,
                        transferRateOps);
            }
            if (!isRepeatDownload) {
                executorService.shutdownNow();
            }
        } catch (SocketTimeoutException e) {
            dispatchSocketTimeout(true, e.getMessage());
            timeEnd = System.currentTimeMillis();
            closeSocket();
            executorService.shutdownNow();
        } catch (IOException e) {
            catchError(true, e.getMessage());
        } catch (InterruptedException e) {
            catchError(true, e.getMessage());
        }
        errorDispatched = false;
    }

    /**
     * Shutdown threadpool and wait for task completion.
     */
    private void shutdownAndWait() {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(THREADPOOL_WAIT_COMPLETION_MS, TimeUnit.MILLISECONDS);
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

        final byte[] buffer = new byte[READ_BUFFER_SIZE];
        int read = 0;

        while ((read = socket.getInputStream().read(buffer)) != -1) {

            downloadTemporaryPacketSize += read;

            if (isRepeatDownload) {
                repeatTempPckSize += read;
            }
            for (int i = 0; i < listenerList.size(); i++) {

                final SpeedTestReport report = getLiveDownloadReport();

                listenerList.get(i).onDownloadProgress(report.getProgressPercent(), getLiveDownloadReport());
            }

            if (downloadTemporaryPacketSize == downloadPckSize.longValueExact()) {
                break;
            }
        }
    }

    /**
     * check for http uri error.
     *
     * @param httFrameState http frame state to check
     */
    private void checkHttpFrameError(final HttpStates httFrameState) {

        if (httFrameState != HttpStates.HTTP_FRAME_OK) {

            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE, PARSING_ERROR +
                            "http frame");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, PARSING_HTTP_ERROR +
                            "frame" + FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * check for http header error.
     *
     * @param httpHeaderState http frame state to check
     */
    private void checkHttpHeaderError(final HttpStates httpHeaderState) {

        if (httpHeaderState != HttpStates.HTTP_FRAME_OK) {

            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE, PARSING_ERROR +
                            "http headers");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, PARSING_HTTP_ERROR +
                            "headers" + FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * check for http content length error.
     *
     * @param httpFrame http frame state to check
     */
    private void checkHttpContentLengthError(final HttpFrame httpFrame) {
        if (httpFrame.getContentLength() < 0) {

            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error content length " +
                            "is inconsistent");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, "Error content length is " +
                            "inconsistent" + FORCE_CLOSE_CAUSE_MESSAGE);
                }
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

                if (frame.getStatusCode() == HTTP_OK && frame.getReasonPhrase().equalsIgnoreCase("ok")) {

                    timeEnd = System.currentTimeMillis();

                    final BigDecimal transferRateOps = uploadFileSize.divide(new BigDecimal(timeEnd -
                                    timeStart).divide(MILLIS_DIVIDER, scale, roundingMode),
                            scale,
                            roundingMode);

                    final BigDecimal transferRateBps = transferRateOps.multiply(BIT_MULTIPLIER);

                    closeSocket();

                    for (int i = 0; i < listenerList.size(); i++) {
                        listenerList.get(i).onUploadPacketsReceived(uploadFileSize.longValueExact(), transferRateBps,
                                transferRateOps);
                    }
                } else {
                    closeSocket();
                }

                if (!isRepeatUpload) {
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
        } catch (SocketException e) {
            if (!errorDispatched) {
                catchError(false, e.getMessage());
            }
        } catch (IOException e) {
            if (!errorDispatched) {
                catchError(true, e.getMessage());
            }
        } catch (InterruptedException e) {
            if (!errorDispatched) {
                catchError(true, e.getMessage());
            }
        }
        errorDispatched = false;
    }

    /**
     * catch an error.
     *
     * @param isDownload   downloading task or uploading task
     * @param errorMessage error message from Exception
     */
    private void catchError(final boolean isDownload, final String errorMessage) {
        dispatchError(isDownload, errorMessage);
        timeEnd = System.currentTimeMillis();
        closeSocket();
        executorService.shutdownNow();
    }

    /**
     * dispatch error listener according to errors.
     *
     * @param isDownload   downloading task or uploading task
     * @param errorMessage error message from Exception
     */
    private void dispatchError(final boolean isDownload, final String errorMessage) {

        if (!forceCloseSocket) {
            if (isDownload) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.CONNECTION_ERROR, errorMessage);
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.CONNECTION_ERROR, errorMessage);
                }
            }
        } else {
            if (isDownload) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, errorMessage +
                            FORCE_CLOSE_CAUSE_MESSAGE);
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.FORCE_CLOSE_SOCKET, errorMessage +
                            FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * dispatch socket timeout error.
     *
     * @param isDownload   define if currently downloading or uploading
     * @param errorMessage error message
     */
    private void dispatchSocketTimeout(final boolean isDownload, final String errorMessage) {

        if (!forceCloseSocket) {
            if (isDownload) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.SOCKET_TIMEOUT, errorMessage);
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.SOCKET_TIMEOUT, errorMessage);
                }
            }
        }
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
     * Start download process.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    public void startDownload(final String hostname, final int port, final String uri) {
        isRepeatDownload = false;
        errorDispatched = false;
        startDownloadRequest(hostname, port, uri);
    }

    /**
     * start download for download repeat.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    private void startDownloadRepeat(final String hostname, final int port, final String uri) {
        errorDispatched = false;
        startDownloadRequest(hostname, port, uri);
    }

    /**
     * start upload for download repeat.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to upload file
     */
    private void startUploadRepeat(final String hostname, final int port, final String uri, final int fileSizeOctet) {
        errorDispatched = false;
        startUpload(hostname, port, uri, fileSizeOctet);
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

        initDownloadRepeat();

        final Timer timer = new Timer();

        final ISpeedTestListener speedTestListener = new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBitPerSeconds,
                                                  final
                                                  BigDecimal transferRateOctetPerSeconds) {
                repeatTransferRateList.add(transferRateOctetPerSeconds);
                startDownloadRepeat(hostname, port, uri);
                repeatRequestNum++;
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                //nothing to do here for download repeat task listener
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                clearRepeatTask(this, timer);
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBitPerSeconds, final
            BigDecimal transferRateOctetPerSeconds) {
                //nothing to do here for download repeat task listener
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                clearRepeatTask(this, timer);
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                //nothing to do here for download repeat task listener
            }
        };

        addSpeedTestListener(speedTestListener);

        repeatWindows = repeatWindow;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeSpeedTestListener(speedTestListener);
                forceStopTask();
                timer.cancel();
                timer.purge();
                repeatFinished = true;
                if (repeatListener != null) {
                    repeatListener.onFinish(getLiveDownloadReport());
                }
            }
        }, repeatWindow);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (repeatListener != null) {
                    repeatListener.onReport(getLiveDownloadReport());
                }
            }
        }, reportPeriodMillis, reportPeriodMillis);

        startDownloadRepeat(hostname, port, uri);
    }

    /**
     * Start repeat upload task.
     *
     * @param hostname           server hostname
     * @param port               server port
     * @param uri                uri to fetch to download file
     * @param repeatWindow       time window for the repeated upload in milliseconds
     * @param reportPeriodMillis time interval between each report in milliseconds
     * @param repeatListener     listener for upload repeat task completion & reports
     */
    public void startUploadRepeat(final String hostname,
                                  final int port,
                                  final String uri,
                                  final int repeatWindow,
                                  final int reportPeriodMillis,
                                  final int fileSizeOctet,
                                  final IRepeatListener repeatListener) {

        initUploadRepeat();

        final Timer timer = new Timer();

        final ISpeedTestListener speedTestListener = new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBitPerSeconds,
                                                  final
                                                  BigDecimal transferRateOctetPerSeconds) {
                //nothing to do here for upload repeat task listener
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                //nothing to do here for upload repeat task listener
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                clearRepeatTask(this, timer);
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBitPerSeconds, final
            BigDecimal transferRateOctetPerSeconds) {
                repeatTransferRateList.add(transferRateOctetPerSeconds);
                startUploadRepeat(hostname, port, uri, fileSizeOctet);
                repeatRequestNum++;
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                clearRepeatTask(this, timer);
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                //nothing to do here for upload repeat task listener
            }
        };

        addSpeedTestListener(speedTestListener);

        repeatWindows = repeatWindow;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeSpeedTestListener(speedTestListener);
                forceStopTask();
                timer.cancel();
                timer.purge();
                repeatFinished = true;
                if (repeatListener != null) {
                    repeatListener.onFinish(getLiveUploadReport());
                }
            }
        }, repeatWindow);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (repeatListener != null) {
                    repeatListener.onReport(getLiveUploadReport());
                }
            }
        }, reportPeriodMillis, reportPeriodMillis);

        startUploadRepeat(hostname, port, uri, fileSizeOctet);
    }

    /**
     * intialize download repeat task variables for report + state.
     */
    private void initDownloadRepeat() {
        isRepeatDownload = true;
        isFirstDownloadRepeat = true;
        initRepeatVars();
    }

    /**
     * intialize upload repeat task variables for report + state.
     */
    private void initUploadRepeat() {
        isRepeatUpload = true;
        isFirstUploadRepeat = true;
        initRepeatVars();
    }

    /**
     * intialize upload/download repeat task variables for report + state.
     */
    private void initRepeatVars() {
        repeatRequestNum = 0;
        repeatPacketSize = BigDecimal.ZERO;
        repeatTempPckSize = 0;
        repeatFinished = false;
        startDateRepeat = 0;
        repeatTransferRateList = new ArrayList<BigDecimal>();
    }

    /**
     * clear completly download/upload repeat task.
     *
     * @param listener speed examples listener
     * @param timer    finished task timer
     */
    private void clearRepeatTask(final ISpeedTestListener listener, final Timer timer) {

        removeSpeedTestListener(listener);
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        repeatFinished = true;
        closeSocket();
        executorService.shutdownNow();
    }

    /**
     * close socket + shutdown thread pool.
     */
    public void forceStopTask() {

        forceCloseSocket = true;
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
                        dispatchSocketTimeout(true, SOCKET_WRITE_ERROR);
                        closeSocket();
                        executorService.shutdownNow();
                    } catch (IOException e) {
                        dispatchError(true, e.getMessage());
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
     */
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

        writeUpload(uploadRequest.getBytes(), fileContent);
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

                            if (isFirstUploadRepeat && isRepeatUpload) {
                                isFirstUploadRepeat = false;
                                startDateRepeat = timeStart;
                            }

                            if (isRepeatUpload) {
                                repeatPacketSize = repeatPacketSize.add(uploadFileSize);
                            }

                            for (int i = 0; i < step; i++) {

                                if (writeFlushSocket(Arrays.copyOfRange(body, uploadTempFileSize,
                                        uploadTempFileSize +
                                                uploadChunkSize)) != 0) {
                                    throw new SocketTimeoutException();
                                }

                                for (int j = 0; j < listenerList.size(); j++) {
                                    final SpeedTestReport report = getLiveUploadReport();
                                    listenerList.get(j).onUploadProgress(report.getProgressPercent(), report);
                                }

                                uploadTempFileSize += uploadChunkSize;

                                if (isRepeatUpload) {
                                    repeatTempPckSize += uploadChunkSize;
                                }
                            }
                            if (remain != 0 && writeFlushSocket(Arrays.copyOfRange(body, uploadTempFileSize,
                                    uploadTempFileSize +
                                            remain)) != 0) {
                                throw new SocketTimeoutException();
                            }
                            for (int j = 0; j < listenerList.size(); j++) {
                                listenerList.get(j).onUploadProgress(PERCENT_MAX.floatValue(), getLiveUploadReport());
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        errorDispatched = true;
                        dispatchSocketTimeout(false, SOCKET_WRITE_ERROR);
                        closeSocket();
                        executorService.shutdownNow();
                    } catch (IOException e) {
                        errorDispatched = true;
                        dispatchError(false, e.getMessage());
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
        try {
            future.get(socketTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            executor.shutdownNow();
            return -1;
        } catch (InterruptedException e) {
            executor.shutdownNow();
            return -1;
        } catch (ExecutionException e) {
            executor.shutdownNow();
            return -1;
        }
        executor.shutdownNow();
        return 0;
    }

    /**
     * get a temporary download report at this moment.
     *
     * @return speed examples download report
     */
    public SpeedTestReport getLiveDownloadReport() {
        return getReport(SpeedTestMode.DOWNLOAD);
    }

    /**
     * get a temporary upload report at this moment.
     *
     * @return speed examples upload report
     */
    public SpeedTestReport getLiveUploadReport() {
        return getReport(SpeedTestMode.UPLOAD);
    }

    /**
     * get a download/upload report.
     *
     * @param mode speed examples mode requested
     * @return speed examples report
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

        final BigDecimal transferRateOps = temporaryPacketSize.divide(new BigDecimal(currentTime - timeStart)
                .divide(MILLIS_DIVIDER, scale, roundingMode), scale, roundingMode);

        final BigDecimal transferRateBitps = transferRateOps.multiply(BIT_MULTIPLIER);

        BigDecimal percent = BigDecimal.ZERO;

        SpeedTestReport report;

        if (isRepeatDownload || isRepeatUpload) {

            report = getRepeatReport(mode, currentTime, transferRateOps);

        } else {

            if (totalPacketSize != BigDecimal.ZERO) {

                percent = temporaryPacketSize.multiply(PERCENT_MAX).divide(totalPacketSize, scale,
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
     * Build repeat download/upload report based on stats on all packets downlaoded until now.
     *
     * @param speedTestMode     speed examples mode
     * @param reportTime        time of current download
     * @param transferRateOctet transfer rate in octet/s
     * @return speed examples report object
     */
    private SpeedTestReport getRepeatReport(final SpeedTestMode speedTestMode,
                                            final long reportTime,
                                            final BigDecimal transferRateOctet) {

        BigDecimal progressPercent = BigDecimal.ZERO;
        long temporaryPacketSize = 0;
        BigDecimal downloadRepeatRateOctet = transferRateOctet;
        long downloadRepeatReportTime = reportTime;

        if (startDateRepeat != 0) {
            if (!repeatFinished) {
                progressPercent = new BigDecimal(System.currentTimeMillis() - startDateRepeat).multiply(PERCENT_MAX)
                        .divide(new BigDecimal(repeatWindows), scale, roundingMode);
            } else {
                progressPercent = PERCENT_MAX;
            }
        } else {
            //download has not started yet
            progressPercent = BigDecimal.ZERO;
        }

        BigDecimal rates = BigDecimal.ZERO;
        for (final BigDecimal rate :
                repeatTransferRateList) {
            rates = rates.add(rate);
        }

        if (!repeatTransferRateList.isEmpty()) {
            downloadRepeatRateOctet = rates.add(downloadRepeatRateOctet).divide(new BigDecimal(repeatTransferRateList
                    .size()).add
                    (new BigDecimal(repeatTempPckSize).divide(repeatPacketSize, scale, roundingMode)
                    ), scale, roundingMode);
        }

        final BigDecimal transferRateBit = downloadRepeatRateOctet.multiply(BIT_MULTIPLIER);

        if (!repeatFinished) {
            temporaryPacketSize = repeatTempPckSize;
        } else {
            temporaryPacketSize = repeatTempPckSize;
            downloadRepeatReportTime = startDateRepeat + repeatWindows;
        }

        return new SpeedTestReport(speedTestMode,
                progressPercent.floatValue(),
                startDateRepeat,
                downloadRepeatReportTime,
                temporaryPacketSize,
                repeatPacketSize.longValueExact(),
                downloadRepeatRateOctet,
                transferRateBit,
                repeatRequestNum);
    }

    /**
     * Close socket streams and socket object.
     */
    public void closeSocket() {

        if (socket != null) {
            try {
                socket.getInputStream().close();
                socket.getOutputStream().close();
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * retrieve current speed examples mode.
     *
     * @return speed examples mode (UPLOAD/DOWNLOAD/NONE)
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
