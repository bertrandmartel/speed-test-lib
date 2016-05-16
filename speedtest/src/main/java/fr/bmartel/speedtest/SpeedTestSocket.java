/**
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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

/**
 * Client socket main implementation
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
     * size of the write read buffer for downloading
     */
    private final static int READ_BUFFER_SIZE = 65535;

    /**
     * default size of each packet sent to upload server
     */
    private final static int DEFAULT_UPLOAD_SIZE = 65535;

    /**
     * socket server hostname
     */
    private String hostname = "";

    /**
     * socket server port
     */
    private int port;

    /**
     * socket object
     */
    private Socket socket;

    /**
     * socket inputstream
     */
    private InputStream socketIs;

    /**
     * define if reading thread is currently running
     */
    private boolean isReading;

    /**
     * speed test listener list
     */
    private final List<ISpeedTestListener> listenerList = new ArrayList<ISpeedTestListener>();

    /**
     * this is the size of each data sent to upload server
     */
    private int uploadChunkSize = DEFAULT_UPLOAD_SIZE;

    /**
     * socket timeout
     */
    private int socketTimeout;

    /**
     * define if socket close error is to be expected
     */
    private boolean forceCloseSocket;

    /**
     * max size for thread pool
     */
    private final static int THREAD_POOL_SIZE = 1;

    /**
     * executor service
     */
    private ScheduledExecutorService executorService;

    /**
     * force clause related error message
     */
    private final static String FORCE_CLOSE_CAUSE_MESSAGE = " caused by socket force close";

    /**********************************************
     ****** SPEED TEST STATS VARIABLES ************
     **********************************************
     * these variables are not used through accessors due
     * to lack of performance of virtual accessors in Android
     * see http://developer.android.com/training/articles/perf-tips.html#GettersSetters
     */
    /**
     * size of file to upload
     */
    private long uploadFileSize;

    /**
     * start time triggered in millis
     */
    private long timeStart;

    /**
     * end time triggered in millis
     */
    private long timeEnd;

    /**
     * current speed test mode
     */
    private SpeedTestMode speedTestMode = SpeedTestMode.NONE;

    /**
     * this is the number of bit uploaded at this time
     */
    private int uploadTempFileSize;

    /**
     * this is the number of packet dowloaded at this time
     */
    private int downloadTemporaryPacketSize;

    /**
     * this is the number of packet to download
     */
    private long downloadPckSize;

    /***************************************************
     ********* SPEED TEST DOWNLOAD REPEAT VARIABLES ****
     ***************************************************
     * these variables are not used through accessors due
     * to lack of performance of virtual accessors in Android
     * see http://developer.android.com/training/articles/perf-tips.html#GettersSetters
     */
    /**
     * define if download should be repeated
     */
    private boolean isRepeatDownload;

    /**
     * start time for download repeat task
     */
    private long startDateRepeat;

    /**
     * time window for download repeat task
     */
    private int repeatWindows;

    /**
     * current number of request for download repeat task
     */
    private int repeatRequestNum;

    /**
     * number of packet pending for download repeat task
     */
    private long repeatPacketSize;

    /**
     * number of packet downloaded for download repeat task
     */
    private long repeatTempPckSize;

    /**
     * current transfer rate in octet/s for download repeat task
     */
    private float repeatTransferRateBps;

    /**
     * define if the first download repeat has been sent and waiting for connection.
     * It is reset to false when the client is connected to server the first time
     */
    private boolean isFirstDownloadRepeat;

    /**
     * define if download repeat task is finished
     */
    private boolean repeatFinished;

    /**
     * Build Client socket
     */
    public SpeedTestSocket() {
        initThreadPool();
    }

    /**
     * Add a speed test listener to list
     *
     * @param listener
     */
    public void addSpeedTestListener(final ISpeedTestListener listener) {
        listenerList.add(listener);
    }

    /**
     * Relive a speed listener from list
     *
     * @param listener
     */
    public void removeSpeedTestListener(final ISpeedTestListener listener) {
        listenerList.remove(listener);
    }

    /**
     * initialize thread pool
     */
    private void initThreadPool() {
        executorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    }

    /**
     * Create and connect socket
     *
     * @param task       task to be executed when connected to socket
     * @param isDownload define if it is a download or upload test
     */
    private void connectAndExecuteTask(TimerTask task, final boolean isDownload) {

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

            socketIs = socket.getInputStream();

            isReading = true;

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
            dispatchError(isDownload, e.getMessage());
        }
    }

    /**
     * start download reading task
     */
    private void startSocketDownloadTask() {

        downloadTemporaryPacketSize = 0;

        try {
            final HttpFrame httpFrame = new HttpFrame();

            timeStart = System.currentTimeMillis();

            if (isFirstDownloadRepeat && isRepeatDownload) {
                isFirstDownloadRepeat = false;
                startDateRepeat = timeStart;
            }
            timeEnd = 0;

            final HttpStates httFrameState = httpFrame.decodeFrame(socketIs);
            checkHttpFrameError(httFrameState);

            final HttpStates httpHeaderState = httpFrame.parseHeader(socketIs);
            checkHttpHeaderError(httpHeaderState);

            checkHttpContentLengthError(httpFrame);

            downloadPckSize = httpFrame.getContentLength();

            if (isRepeatDownload) {
                repeatPacketSize += downloadPckSize;
            }

            downloadReadingLoop();

            timeEnd = System.currentTimeMillis();

            final float transferRate_Bps = downloadPckSize / ((timeEnd - timeStart) / 1000f);
            final float transferRate_bps = transferRate_Bps * 8;

            closeSocket();

            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onDownloadPacketsReceived(downloadPckSize, transferRate_bps, transferRate_Bps);
            }
            if (!isRepeatDownload) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            catchError(true, e.getMessage());
        } catch (InterruptedException e) {
            catchError(true, e.getMessage());
        }
    }

    /**
     * start download reading loop + monitor progress
     *
     * @throws IOException
     */
    private void downloadReadingLoop() throws IOException {

        final byte[] buffer = new byte[READ_BUFFER_SIZE];
        int read = 0;

        while ((read = socketIs.read(buffer)) != -1) {

            downloadTemporaryPacketSize += read;

            if (isRepeatDownload) {
                repeatTempPckSize += read;
            }
            for (int i = 0; i < listenerList.size(); i++) {
                SpeedTestReport report = getLiveDownloadReport();
                listenerList.get(i).onDownloadProgress(report.getProgressPercent(), getLiveDownloadReport());
            }
            if (downloadTemporaryPacketSize == downloadPckSize) {
                break;
            }
        }
    }

    /**
     * checl for http uri error
     *
     * @param httFrameState
     */
    private void checkHttpFrameError(HttpStates httFrameState) {
        if (httFrameState != HttpStates.HTTP_FRAME_OK) {
            System.err.println("Error while parsing http frame");
            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error while parsing http frame");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, "Error while parsing http frame" + FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * check for http header error
     *
     * @param httpHeaderState
     */
    private void checkHttpHeaderError(HttpStates httpHeaderState) {
        if (httpHeaderState != HttpStates.HTTP_FRAME_OK) {
            System.err.println("Error while parsing http headers");
            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error while parsing http headers");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, "Error while parsing http headers" + FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * check for http content length error
     *
     * @param httpFrame
     */
    private void checkHttpContentLengthError(HttpFrame httpFrame) {
        if (httpFrame.getContentLength() < 0) {
            System.err.println("Error content length is inconsistent");
            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error content length is inconsistent");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, "Error content length is inconsistent" + FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * start upload writing task
     */
    private void startSocketUploadTask() {

        while (isReading) {
            try {
                HttpFrame frame = new HttpFrame();

                HttpStates httpStates = frame.parseHttp(socketIs);

                if (httpStates == HttpStates.HTTP_FRAME_OK) {
                    if (frame.getStatusCode() == 200 && frame.getReasonPhrase().equalsIgnoreCase("ok")) {

                        timeEnd = System.currentTimeMillis();

                        float transferRate_Bps = (uploadFileSize) / ((timeEnd - timeStart) / 1000f);
                        float transferRate_bps = transferRate_Bps * 8;

                        for (int i = 0; i < listenerList.size(); i++) {
                            listenerList.get(i).onUploadPacketsReceived(uploadFileSize, transferRate_bps, transferRate_Bps);
                        }
                    }
                    speedTestMode = SpeedTestMode.NONE;

                    isReading = false;
                    closeSocket();
                    executorService.shutdown();
                    return;
                }
                isReading = false;
                closeSocket();
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.SOCKET_ERROR, "socket error");
                }
                executorService.shutdown();
            } catch (SocketException e) {
                catchError(false, e.getMessage());
            } catch (Exception e) {
                catchError(false, e.getMessage());
            }
        }
    }

    /**
     * catch an error
     *
     * @param isDownload   downloading task or uploading task
     * @param errorMessage error message from Exception
     */
    private void catchError(boolean isDownload, String errorMessage) {
        dispatchError(isDownload, errorMessage);
        timeEnd = System.currentTimeMillis();
        closeSocket();
        executorService.shutdown();
    }

    /**
     * dispatch error listener according to errors
     *
     * @param isDownload   downloading task or uploading task
     * @param errorMessage error message from Exception
     */
    private void dispatchError(boolean isDownload, String errorMessage) {
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
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, errorMessage + FORCE_CLOSE_CAUSE_MESSAGE);
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.FORCE_CLOSE_SOCKET, errorMessage + FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * start download task
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    private void startDownloadRequest(String hostname, int port, String uri) {
        forceCloseSocket = false;
        this.hostname = hostname;
        this.port = port;
        String downloadRequest = "GET " + uri + " HTTP/1.1\r\n" + "Host: " + hostname + "\r\n\r\n";
        writeDownload(downloadRequest.getBytes());
    }

    /**
     * Start download process
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    public void startDownload(String hostname, int port, String uri) {
        isRepeatDownload = false;
        startDownloadRequest(hostname, port, uri);
    }

    /**
     * start download for download repeat
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    private void startDownloadRepeat(String hostname, int port, String uri) {
        startDownloadRequest(hostname, port, uri);
    }

    /**
     * Start repeat download task
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
                                    int repeatWindow,
                                    int reportPeriodMillis,
                                    final IRepeatListener repeatListener) {

        initDownloadRepeat();

        final Timer timer = new Timer();

        final ISpeedTestListener speedTestListener = new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(long packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {
                repeatTransferRateBps = ((repeatTransferRateBps + transferRateOctetPerSeconds) / 2f);
                startDownloadRepeat(hostname, port, uri);
                repeatRequestNum++;
            }

            @Override
            public void onDownloadProgress(float percent, SpeedTestReport report) {
                //nothing to do here for download repeat task listener
            }

            @Override
            public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
                clearDownloadRepeat(this, timer);
            }

            @Override
            public void onUploadPacketsReceived(long packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {
                //nothing to do here for download repeat task listener
            }

            @Override
            public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
                clearDownloadRepeat(this, timer);
            }

            @Override
            public void onUploadProgress(float percent, SpeedTestReport report) {
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
                if (repeatListener != null)
                    repeatListener.onFinish(getLiveDownloadReport());
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
     * intialize download repeat task variables for report + state
     */
    private void initDownloadRepeat() {

        isRepeatDownload = true;
        repeatRequestNum = 0;
        repeatPacketSize = 0;
        repeatTempPckSize = 0;
        repeatTransferRateBps = 0;
        repeatFinished = false;
        startDateRepeat = 0;
        isFirstDownloadRepeat = true;
    }

    /**
     * clear completly download repeat task
     *
     * @param listener speed test listener
     * @param timer    finished task timer
     */
    private void clearDownloadRepeat(ISpeedTestListener listener, Timer timer) {

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
     * close socket + shutdown thread pool
     */
    public void forceStopTask() {

        forceCloseSocket = true;
        closeSocket();
        executorService.shutdownNow();
    }

    /**
     * Write download request to server host
     *
     * @param data HTTP request to send to initiate downwload process
     */
    private void writeDownload(final byte[] data) {

        speedTestMode = SpeedTestMode.DOWNLOAD;

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {
                if (socket != null && !socket.isClosed()) {
                    try {
                        if (socket.getOutputStream() != null) {
                            timeStart = System.currentTimeMillis();
                            socket.getOutputStream().write(data);
                            socket.getOutputStream().flush();
                        }
                    } catch (IOException e) {
                        dispatchError(true, e.getMessage());
                    }
                }
            }
        }, true);
    }

    /**
     * Start upload process
     *
     * @param hostname      server hostname
     * @param port          server port
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     */
    public void startUpload(String hostname, int port, String uri, int fileSizeOctet) {

        this.hostname = hostname;
        this.port = port;
        uploadFileSize = fileSizeOctet;
        timeEnd = 0;
        forceCloseSocket = false;
        /* generate a file with size of fileSizeOctet octet */
        RandomGen random = new RandomGen(fileSizeOctet);
        byte[] fileContent = random.nextArray();

        String uploadRequest = "POST " + uri + " HTTP/1.1\r\n" + "Host: " + hostname + "\r\nAccept: */*\r\nContent-Length: " + fileSizeOctet + "\r\n\r\n";
        writeUpload(uploadRequest.getBytes(), fileContent);
    }

    /**
     * Write upload POST request with file generated randomly
     *
     * @param head http headers
     * @param body file content to upload
     */
    private void writeUpload(final byte[] head, final byte[] body) {

        speedTestMode = SpeedTestMode.UPLOAD;

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {
                if (socket != null && !socket.isClosed()) {
                    try {

                        uploadTempFileSize = 0;

                        int step = body.length / uploadChunkSize;
                        int remain = body.length % uploadChunkSize;

                        if (socket.getOutputStream() != null) {

                            socket.getOutputStream().write(head);
                            socket.getOutputStream().flush();

                            timeStart = System.currentTimeMillis();

                            for (int i = 0; i < step; i++) {
                                socket.getOutputStream().write(Arrays.copyOfRange(body, uploadTempFileSize, uploadTempFileSize + uploadChunkSize));
                                socket.getOutputStream().flush();
                                for (int j = 0; j < listenerList.size(); j++) {
                                    SpeedTestReport report = getLiveUploadReport();
                                    listenerList.get(j).onUploadProgress(report.getProgressPercent(), report);
                                }
                                uploadTempFileSize += uploadChunkSize;
                            }
                            if (remain != 0) {
                                socket.getOutputStream().write(Arrays.copyOfRange(body, uploadTempFileSize, uploadTempFileSize + remain));
                                socket.getOutputStream().flush();
                            }
                            for (int j = 0; j < listenerList.size(); j++) {
                                listenerList.get(j).onUploadProgress(100, getLiveUploadReport());
                            }
                        }
                    } catch (IOException e) {
                        dispatchError(false, e.getMessage());
                        executorService.shutdown();
                    }
                }
            }
        }, false);
    }

    /**
     * get a temporary download report at this moment
     *
     * @return speed test download report
     */
    public SpeedTestReport getLiveDownloadReport() {
        return getReport(SpeedTestMode.DOWNLOAD);
    }

    /**
     * get a temporary upload report at this moment
     *
     * @return speed test upload report
     */
    public SpeedTestReport getLiveUploadReport() {
        return getReport(SpeedTestMode.UPLOAD);
    }

    /**
     * get a download/upload report
     *
     * @param mode speed test mode requested
     * @return speed test report
     */
    private SpeedTestReport getReport(SpeedTestMode mode) {

        long temporaryPacketSize = 0;
        long totalPacketSize = 0;
        switch (mode) {
            case DOWNLOAD:
                temporaryPacketSize = downloadTemporaryPacketSize;
                totalPacketSize = downloadPckSize;
                break;
            case UPLOAD:
                temporaryPacketSize = uploadTempFileSize;
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

        float transferRate_Bps = temporaryPacketSize / ((currentTime - timeStart) / 1000f);
        float transferRate_bps = transferRate_Bps * 8;

        float percent = 0;

        if (isRepeatDownload) {

            return getRepeatDownloadReport(mode, currentTime, transferRate_Bps);

        } else {

            if (totalPacketSize != 0) {
                percent = temporaryPacketSize * 100f / totalPacketSize;
            }

            return new SpeedTestReport(mode, percent,
                    timeStart, currentTime, temporaryPacketSize, totalPacketSize, transferRate_Bps, transferRate_bps, 1);
        }
    }

    /**
     * Build repeat download report based on stats on all packets downlaoded until now
     *
     * @param speedTestMode     speed test mode
     * @param reportTime        time of current download
     * @param transferRateOctet transfer rate in octet/s
     * @return speed test report object
     */
    private SpeedTestReport getRepeatDownloadReport(SpeedTestMode speedTestMode,
                                                    long reportTime,
                                                    float transferRateOctet) {
        float progressPercent = 0;
        long temporaryPacketSize = 0;
        float transferRateBit = 0;
        float downloadRepeatRateOctet = transferRateOctet;
        long downloadRepeatReportTime = reportTime;

        if (startDateRepeat != 0) {
            if (!repeatFinished)
                progressPercent = (System.currentTimeMillis() - startDateRepeat) * 100f / repeatWindows;
            else
                progressPercent = 100;
        } else {
            //download has not started yet
            progressPercent = 0;
        }

        if ((repeatTransferRateBps != 0) && !repeatFinished)
            downloadRepeatRateOctet = (repeatTransferRateBps + downloadRepeatRateOctet) / 2f;
        else if (repeatFinished) {
            downloadRepeatRateOctet = repeatTransferRateBps;
        }

        transferRateBit = downloadRepeatRateOctet * 8f;

        if (!repeatFinished)
            temporaryPacketSize = repeatTempPckSize;
        else
            temporaryPacketSize = repeatPacketSize;

        if (repeatFinished)
            downloadRepeatReportTime = startDateRepeat + repeatWindows;

        return new SpeedTestReport(speedTestMode,
                progressPercent,
                startDateRepeat,
                downloadRepeatReportTime,
                temporaryPacketSize,
                repeatPacketSize,
                downloadRepeatRateOctet,
                transferRateBit,
                repeatRequestNum);
    }

    /**
     * Close socket streams and socket object
     */
    public void closeSocket() {

        if (socket != null) {
            try {
                if (socketIs != null) {
                    socketIs.close();
                    socketIs.close();
                }
                socket.close();
            } catch (IOException e) {
            }
        }
        socket = null;
    }

    /**
     * Join reading thread before closing socket
     */
    public void closeSocketJoinRead() {
        isReading = false;
        executorService.shutdown();
        while (!executorService.isTerminated()) {
        }
        closeSocket();
    }

    /**
     * retrieve current speed test mode
     *
     * @return speed test mode (UPLOAD/DOWNLOAD/NONE)
     */
    public SpeedTestMode getSpeedTestMode() {
        return speedTestMode;
    }

    /**
     * set socket timeout in millisecond
     *
     * @param socketTimeoutMillis socket timeout value in milliseconds
     */
    public void setSocketTimeout(int socketTimeoutMillis) {
        if (socketTimeoutMillis >= 0)
            socketTimeout = socketTimeoutMillis;
    }

    /**
     * get socket timeout in milliseconds ( 0 if no timeout not defined)
     *
     * @return socket timeout value (0 if not defined)
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * retrieve size of each packet sent to upload server
     *
     * @return size of each packet sent to upload server
     */
    public int getUploadChunkSize() {
        return uploadChunkSize;
    }

    /**
     * set size of each packet sent to upload server
     *
     * @param uploadChunkSize new size of each packet sent to upload server
     */
    public void setUploadChunkSize(int uploadChunkSize) {
        this.uploadChunkSize = uploadChunkSize;
    }
}
