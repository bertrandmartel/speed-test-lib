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
 * For both mode, transfer rate is calculated independently from mSocket initial
 * connection
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocket implements ISpeedTestSocket {

    /**
     * BigDecimal scale used in transfer rate calculation.
     */
    private int mScale = SpeedTestConst.DEFAULT_SCALE;

    /**
     * BigDecimal RoundingMode used in transfer rate calculation.
     */
    private RoundingMode mRoundingMode = SpeedTestConst.DEFAULT_ROUNDING_MODE;

    /**
     * socket server hostname.
     */
    private String mHostname = "";

    /**
     * socket server port.
     */
    private int mPort;

    /**
     * socket object.
     */
    private Socket mSocket;

    /**
     * speed test listener list.
     */
    private final List<ISpeedTestListener> mListenerList = new ArrayList<>();

    /**
     * this is the size of each data sent to upload server.
     */
    private int mUploadChunkSize = SpeedTestConst.DEFAULT_UPLOAD_SIZE;

    /**
     * mSocket timeout.
     */
    private int mSocketTimeout = SpeedTestConst.DEFAULT_SOCKET_TIMEOUT;

    /**
     * define if mSocket close error is to be expected.
     */
    private boolean mForceCloseSocket;

    /**
     * executor service.
     */
    private ScheduledExecutorService mExecutorService;

    /**
     * executor service used for reporting.
     */
    private ScheduledExecutorService mReportExecutorService;

    /**
     * size of file to upload.
     */
    private BigDecimal mUploadFileSize = BigDecimal.ZERO;

    /**
     * start time triggered in millis.
     */
    private long mTimeStart;

    /**
     * end time triggered in millis.
     */
    private long mTimeEnd;

    /**
     * current speed test mode.
     */
    private SpeedTestMode mSpeedTestMode = SpeedTestMode.NONE;

    /**
     * this is the number of bit uploaded at this time.
     */
    private int mUploadTempFileSize;

    /**
     * this is the number of packet downloaded at this time.
     */
    private int mDownloadTemporaryPacketSize;

    /**
     * this is the number of packet to download.
     */
    private BigDecimal mDownloadPckSize = BigDecimal.ZERO;

    /**
     * define if an error has been dispatched already or not. This is reset to false on start download/ upload + in
     * reading thread
     */
    private boolean mErrorDispatched;

    /**
     * Speed test repeat wrapper.
     */
    private final RepeatWrapper mRepeatWrapper;

    /**
     * define if report interval is set.
     */
    private boolean mReportInterval;

    /**
     * Build Client mSocket.
     */
    public SpeedTestSocket() {
        initThreadPool();
        mRepeatWrapper = new RepeatWrapper(this);
    }

    /**
     * Add a speed test listener to list.
     *
     * @param listener speed test listener to be added
     */
    @Override
    public void addSpeedTestListener(final ISpeedTestListener listener) {
        mListenerList.add(listener);
    }

    /**
     * Relive a speed listener from list.
     *
     * @param listener speed test listener to be removed
     */
    @Override
    public void removeSpeedTestListener(final ISpeedTestListener listener) {
        mListenerList.remove(listener);
    }

    /**
     * initialize thread pool.
     */
    private void initThreadPool() {
        mExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_SIZE);
        mReportExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_REPORT_SIZE);
    }

    /**
     * Create and connect mSocket.
     *
     * @param task       task to be executed when connected to mSocket
     * @param isDownload define if it is a download or upload test
     */
    private void connectAndExecuteTask(final TimerTask task, final boolean isDownload) {

        // close mSocket before recreating it
        if (mSocket != null) {
            closeSocket();
        }
        try {
            /* create a basic mSocket connection */
            mSocket = new Socket();

            if (mSocketTimeout != 0 && isDownload) {
                mSocket.setSoTimeout(mSocketTimeout);
            }

			/* establish mSocket parameters */
            mSocket.setReuseAddress(true);

            mSocket.setKeepAlive(true);

            mSocket.connect(new InetSocketAddress(mHostname, mPort));

            if (!mExecutorService.isShutdown()) {
                mExecutorService.execute(new Runnable() {

                    @Override
                    public void run() {

                        if (isDownload) {
                            startSocketDownloadTask();
                        } else {
                            startSocketUploadTask();
                        }
                        mSpeedTestMode = SpeedTestMode.NONE;
                    }
                });
            }

            if (task != null) {
                task.run();
            }
        } catch (IOException e) {
            if (!mErrorDispatched) {
                SpeedTestUtils.dispatchError(mForceCloseSocket, mListenerList, isDownload, e.getMessage());
            }
        }
    }

    /**
     * start download reading task.
     */
    private void startSocketDownloadTask() {

        mDownloadTemporaryPacketSize = 0;

        try {
            final HttpFrame httpFrame = new HttpFrame();

            mTimeStart = System.currentTimeMillis();
            mTimeEnd = 0;

            if (mRepeatWrapper.isFirstDownload()) {
                mRepeatWrapper.setFirstDownloadRepeat(false);
                mRepeatWrapper.setStartDate(mTimeStart);
            }

            final HttpStates httFrameState = httpFrame.decodeFrame(mSocket.getInputStream());
            SpeedTestUtils.checkHttpFrameError(mForceCloseSocket, mListenerList, httFrameState);

            final HttpStates httpHeaderState = httpFrame.parseHeader(mSocket.getInputStream());
            SpeedTestUtils.checkHttpHeaderError(mForceCloseSocket, mListenerList, httpHeaderState);

            SpeedTestUtils.checkHttpContentLengthError(mForceCloseSocket, mListenerList, httpFrame);

            mDownloadPckSize = new BigDecimal(httpFrame.getContentLength());

            if (mRepeatWrapper.isRepeatDownload()) {
                mRepeatWrapper.updatePacketSize(mDownloadPckSize);
            }
            downloadReadingLoop();
            mTimeEnd = System.currentTimeMillis();

            closeSocket();

            final SpeedTestReport report = getLiveDownloadReport();

            for (int i = 0; i < mListenerList.size(); i++) {
                mListenerList.get(i).onDownloadFinished(report);
            }

            if (!mRepeatWrapper.isRepeatDownload()) {
                mExecutorService.shutdownNow();
                mReportExecutorService.shutdownNow();
            }

        } catch (SocketTimeoutException e) {
            SpeedTestUtils.dispatchSocketTimeout(mForceCloseSocket, mListenerList, true, e.getMessage());
            mTimeEnd = System.currentTimeMillis();
            closeSocket();
            mExecutorService.shutdownNow();
            mReportExecutorService.shutdownNow();
        } catch (IOException | InterruptedException e) {
            catchError(true, e.getMessage());
        }
        mReportInterval = false;
        mErrorDispatched = false;
    }

    /**
     * Shutdown threadpool and wait for task completion.
     */
    @Override
    public void shutdownAndWait() {
        mExecutorService.shutdownNow();
        mReportExecutorService.shutdownNow();
        try {
            mExecutorService.awaitTermination(SpeedTestConst.THREADPOOL_WAIT_COMPLETION_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    /**
     * start download reading loop + monitor progress.
     *
     * @throws IOException mSocket io exception
     */
    private void downloadReadingLoop() throws IOException {

        final byte[] buffer = new byte[SpeedTestConst.READ_BUFFER_SIZE];
        int read;

        while ((read = mSocket.getInputStream().read(buffer)) != -1) {

            mDownloadTemporaryPacketSize += read;

            if (mRepeatWrapper.isRepeatDownload()) {
                mRepeatWrapper.updateTempPacketSize(read);
            }

            if (!mReportInterval) {
                final SpeedTestReport report = getLiveDownloadReport();

                for (int i = 0; i < mListenerList.size(); i++) {
                    mListenerList.get(i).onDownloadProgress(report.getProgressPercent(), report);
                }
            }

            if (mDownloadTemporaryPacketSize == mDownloadPckSize.longValueExact()) {
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

            final HttpStates httpStates = frame.parseHttp(mSocket.getInputStream());

            if (httpStates == HttpStates.HTTP_FRAME_OK) {

                if (frame.getStatusCode() == SpeedTestConst.HTTP_OK && frame.getReasonPhrase().equalsIgnoreCase("ok")) {

                    mTimeEnd = System.currentTimeMillis();

                    closeSocket();

                    final SpeedTestReport report = getLiveUploadReport();

                    for (int i = 0; i < mListenerList.size(); i++) {
                        mListenerList.get(i).onUploadFinished(report);
                    }

                } else {
                    closeSocket();
                }

                if (!mRepeatWrapper.isRepeatUpload()) {
                    mExecutorService.shutdownNow();
                    mReportExecutorService.shutdownNow();
                }

                return;
            }
            closeSocket();
            if (!mErrorDispatched && !mForceCloseSocket) {
                for (int i = 0; i < mListenerList.size(); i++) {
                    mListenerList.get(i).onUploadError(SpeedTestError.SOCKET_ERROR, "mSocket error");
                }
            }
            mExecutorService.shutdownNow();
            mReportExecutorService.shutdownNow();
        } catch (IOException | InterruptedException e) {
            if (!mErrorDispatched) {
                catchError(false, e.getMessage());
            }
        }
        mErrorDispatched = false;
    }

    /**
     * start download task.
     *
     * @param hostname server mHostname
     * @param port     server mPort
     * @param uri      uri to fetch to download file
     */
    private void startDownloadRequest(final String hostname, final int port, final String uri) {
        mForceCloseSocket = false;
        this.mHostname = hostname;
        this.mPort = port;
        final String downloadRequest = "GET " + uri + " HTTP/1.1\r\n" + "Host: " + hostname + "\r\n\r\n";
        writeDownload(downloadRequest.getBytes());
    }

    /**
     * Start download process with a fixed duration.
     *
     * @param hostname    server mHostname
     * @param port        server mPort
     * @param uri         uri to fetch to download file
     * @param maxDuration maximum duration of the speed test in milliseconds
     */
    public void startFixedDownload(final String hostname,
                                   final int port,
                                   final String uri,
                                   final int maxDuration) {

        if (mReportExecutorService == null || mReportExecutorService.isShutdown()) {
            mReportExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_REPORT_SIZE);
        }

        mReportExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                forceStopTask();
            }
        }, maxDuration, TimeUnit.MILLISECONDS);

        startDownload(hostname, port, uri);
    }

    /**
     * Start download process with a fixed duration.
     *
     * @param hostname       server mHostname
     * @param port           server mPort
     * @param uri            uri to fetch to download file
     * @param maxDuration    maximum duration of the speed test in milliseconds
     * @param reportInterval report interval in milliseconds
     */
    public void startFixedDownload(final String hostname,
                                   final int port,
                                   final String uri,
                                   final int maxDuration,
                                   final int reportInterval) {
        initReportTask(reportInterval, false);
        mReportInterval = true;
        startFixedDownload(hostname, port, uri, maxDuration);
    }

    /**
     * Start download process.
     *
     * @param hostname       server mHostname
     * @param port           server mPort
     * @param uri            uri to fetch to download file
     * @param reportInterval report interval in milliseconds
     */
    public void startDownload(final String hostname,
                              final int port,
                              final String uri,
                              final int reportInterval) {
        initReportTask(reportInterval, false);
        mReportInterval = true;
        startDownload(hostname, port, uri);
    }

    /**
     * Start download process.
     *
     * @param hostname server mHostname
     * @param port     server mPort
     * @param uri      uri to fetch to download file
     */
    @Override
    public void startDownload(final String hostname, final int port, final String uri) {
        mErrorDispatched = false;
        startDownloadRequest(hostname, port, uri);
    }

    /**
     * close mSocket + shutdown thread pool.
     */
    @Override
    public void forceStopTask() {
        mForceCloseSocket = true;
        mSpeedTestMode = SpeedTestMode.NONE;
        closeSocket();
        shutdownAndWait();
    }

    /**
     * Write download request to server host.
     *
     * @param data HTTP request to send to initiate download process
     */
    private void writeDownload(final byte[] data) {

        mSpeedTestMode = SpeedTestMode.DOWNLOAD;

        if (mExecutorService == null || mExecutorService.isShutdown()) {
            initThreadPool();
        }

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {

                if (mSocket != null && !mSocket.isClosed()) {

                    try {
                        if ((mSocket.getOutputStream() != null) && (writeFlushSocket(data) != 0)) {
                            throw new SocketTimeoutException();
                        }
                    } catch (SocketTimeoutException e) {
                        SpeedTestUtils.dispatchSocketTimeout(mForceCloseSocket, mListenerList,
                                true, SpeedTestConst.SOCKET_WRITE_ERROR);
                        closeSocket();
                        mExecutorService.shutdownNow();
                        mReportExecutorService.shutdownNow();
                    } catch (IOException e) {
                        SpeedTestUtils.dispatchError(mForceCloseSocket, mListenerList, true, e.getMessage());
                        mExecutorService.shutdownNow();
                        mReportExecutorService.shutdownNow();
                    }
                }
            }
        }, true);
    }

    /**
     * Start upload process.
     *
     * @param hostname      server mHostname
     * @param port          server mPort
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     * @param maxDuration   maximum duration of speed test in milliseconds
     */
    public void startFixedUpload(final String hostname,
                                 final int port,
                                 final String uri,
                                 final int fileSizeOctet,
                                 final int maxDuration) {

        if (mReportExecutorService == null || mReportExecutorService.isShutdown()) {
            mReportExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_REPORT_SIZE);
        }
        mReportExecutorService.schedule(new Runnable() {
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
     * @param hostname       server mHostname
     * @param port           server mPort
     * @param uri            uri to fetch
     * @param fileSizeOctet  size of file to upload
     * @param maxDuration    maximum duration of speed test in milliseconds
     * @param reportInterval report interval in milliseconds
     */
    public void startFixedUpload(final String hostname,
                                 final int port,
                                 final String uri,
                                 final int fileSizeOctet,
                                 final int maxDuration,
                                 final int reportInterval) {

        initReportTask(reportInterval, false);

        mReportInterval = true;
        startFixedUpload(hostname, port, uri, fileSizeOctet, maxDuration);
    }

    /**
     * initialize report task.
     *
     * @param reportInterval report interval in milliseconds
     * @param download       define if download or upload report should be dispatched
     */
    private void initReportTask(final int reportInterval, final boolean download) {

        if (mReportExecutorService == null || mReportExecutorService.isShutdown()) {
            mReportExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_REPORT_SIZE);
        }

        mReportExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                final SpeedTestReport report;

                if (!download) {

                    report = getLiveUploadReport();

                    for (final ISpeedTestListener listener : mListenerList) {
                        listener.onUploadProgress(report.getProgressPercent(), report);
                    }
                } else {

                    report = getLiveDownloadReport();

                    for (final ISpeedTestListener listener : mListenerList) {
                        listener.onDownloadProgress(report.getProgressPercent(), report);
                    }
                }
            }
        }, reportInterval, reportInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Start upload process.
     *
     * @param hostname       server mHostname
     * @param port           server mPort
     * @param uri            uri to fetch
     * @param fileSizeOctet  size of file to upload
     * @param reportInterval report interval in milliseconds
     */
    public void startUpload(final String hostname,
                            final int port,
                            final String uri,
                            final int fileSizeOctet,
                            final int reportInterval) {

        initReportTask(reportInterval, false);
        mReportInterval = true;
        startUpload(hostname, port, uri, fileSizeOctet);
    }

    /**
     * Start upload process.
     *
     * @param hostname      server mHostname
     * @param port          server mPort
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     */
    @Override
    public void startUpload(final String hostname, final int port, final String uri, final int fileSizeOctet) {

        this.mHostname = hostname;
        this.mPort = port;
        mUploadFileSize = new BigDecimal(fileSizeOctet);
        mForceCloseSocket = false;
        mErrorDispatched = false;

        mSpeedTestMode = SpeedTestMode.UPLOAD;

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

        if (mExecutorService == null || mExecutorService.isShutdown()) {
            initThreadPool();
        }

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {
                if (mSocket != null && !mSocket.isClosed()) {
                    try {

                        mUploadTempFileSize = 0;

                        final int step = body.length / mUploadChunkSize;
                        final int remain = body.length % mUploadChunkSize;

                        if (mSocket.getOutputStream() != null) {

                            if (writeFlushSocket(head) != 0) {
                                throw new SocketTimeoutException();
                            }

                            mTimeStart = System.currentTimeMillis();
                            mTimeEnd = 0;

                            if (mRepeatWrapper.isFirstUpload()) {
                                mRepeatWrapper.setFirstUploadRepeat(false);
                                mRepeatWrapper.setStartDate(mTimeStart);
                            }

                            if (mRepeatWrapper.isRepeatUpload()) {
                                mRepeatWrapper.updatePacketSize(mUploadFileSize);
                            }

                            for (int i = 0; i < step; i++) {

                                if (writeFlushSocket(Arrays.copyOfRange(body, mUploadTempFileSize,
                                        mUploadTempFileSize +
                                                mUploadChunkSize)) != 0) {
                                    throw new SocketTimeoutException();
                                }

                                mUploadTempFileSize += mUploadChunkSize;

                                if (mRepeatWrapper.isRepeatUpload()) {
                                    mRepeatWrapper.updateTempPacketSize(mUploadChunkSize);
                                }

                                if (!mReportInterval) {
                                    final SpeedTestReport report = getLiveUploadReport();

                                    for (int j = 0; j < mListenerList.size(); j++) {
                                        mListenerList.get(j).onUploadProgress(report.getProgressPercent(), report);
                                    }
                                }
                            }
                            if (remain != 0 && writeFlushSocket(Arrays.copyOfRange(body, mUploadTempFileSize,
                                    mUploadTempFileSize +
                                            remain)) != 0) {
                                throw new SocketTimeoutException();
                            } else {
                                mUploadTempFileSize += remain;

                                if (mRepeatWrapper.isRepeatUpload()) {
                                    mRepeatWrapper.updateTempPacketSize(remain);
                                }
                            }

                            if (!mReportInterval) {
                                final SpeedTestReport report = getLiveUploadReport();

                                for (int j = 0; j < mListenerList.size(); j++) {
                                    mListenerList.get(j).onUploadProgress(SpeedTestConst.PERCENT_MAX.floatValue(),
                                            report);

                                }
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        mErrorDispatched = true;
                        if (!mForceCloseSocket) {
                            SpeedTestUtils.dispatchSocketTimeout(mForceCloseSocket, mListenerList,
                                    false, SpeedTestConst.SOCKET_WRITE_ERROR);
                        } else {
                            SpeedTestUtils.dispatchError(mForceCloseSocket, mListenerList, false, e.getMessage());
                        }
                        closeSocket();
                        mExecutorService.shutdownNow();
                        mReportExecutorService.shutdownNow();
                    } catch (IOException e) {
                        mErrorDispatched = true;
                        SpeedTestUtils.dispatchError(mForceCloseSocket, mListenerList, false, e.getMessage());
                        mExecutorService.shutdownNow();
                        mReportExecutorService.shutdownNow();
                    } finally {
                        mReportInterval = false;
                    }
                }
            }
        }, false);
    }

    /**
     * write and flush mSocket.
     *
     * @param data payload to write
     * @return error status (-1 for error)
     * @throws IOException mSocket io exception
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
                    mSocket.getOutputStream().write(data);
                    mSocket.getOutputStream().flush();
                } catch (IOException e) {
                    return -1;
                }
                return 0;
            }
        });
        int status;
        try {
            status = future.get(mSocketTimeout, TimeUnit.MILLISECONDS);
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
        SpeedTestUtils.dispatchError(mForceCloseSocket, mListenerList, isDownload, errorMessage);
        mTimeEnd = System.currentTimeMillis();
        closeSocket();
        mExecutorService.shutdownNow();
        mReportExecutorService.shutdownNow();
    }

    /**
     * Start repeat download task.
     *
     * @param hostname           server mHostname
     * @param port               server mPort
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
        mRepeatWrapper.startDownloadRepeat(hostname, port, uri, repeatWindow, reportPeriodMillis, repeatListener);
    }

    /**
     * Start repeat upload task.
     *
     * @param hostname           server mHostname
     * @param port               server mPort
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

        mRepeatWrapper.startUploadRepeat(hostname,
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
                temporaryPacketSize = new BigDecimal(mDownloadTemporaryPacketSize);
                totalPacketSize = mDownloadPckSize;
                break;
            case UPLOAD:
                temporaryPacketSize = new BigDecimal(mUploadTempFileSize);
                totalPacketSize = mUploadFileSize;
                break;
            default:
                break;
        }

        long currentTime;
        if (mTimeEnd == 0) {
            currentTime = System.currentTimeMillis();
        } else {
            currentTime = mTimeEnd;
        }

        BigDecimal transferRateOps = BigDecimal.ZERO;

        if ((currentTime - mTimeStart) != 0) {
            transferRateOps = temporaryPacketSize.divide(new BigDecimal(currentTime - mTimeStart)
                    .divide(SpeedTestConst.MILLIS_DIVIDER, mScale, mRoundingMode), mScale, mRoundingMode);
        }

        final BigDecimal transferRateBitps = transferRateOps.multiply(SpeedTestConst.BIT_MULTIPLIER);

        BigDecimal percent = BigDecimal.ZERO;

        SpeedTestReport report;

        if (mRepeatWrapper.isRepeat()) {

            report = mRepeatWrapper.getRepeatReport(mScale, mRoundingMode, mode, currentTime, transferRateOps);

        } else {

            if (totalPacketSize != BigDecimal.ZERO) {

                percent = temporaryPacketSize.multiply(SpeedTestConst.PERCENT_MAX).divide(totalPacketSize, mScale,
                        mRoundingMode);
            }

            report = new SpeedTestReport(mode, percent.floatValue(),
                    mTimeStart, currentTime, temporaryPacketSize.longValueExact(), totalPacketSize.longValueExact(),
                    transferRateOps, transferRateBitps,
                    1);
        }
        return report;
    }

    /**
     * Close socket streams and mSocket object.
     */
    @Override
    public void closeSocket() {

        if (mSocket != null) {
            try {
                mSocket.close();
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
        return mSpeedTestMode;
    }

    /**
     * set socket timeout in millisecond.
     *
     * @param socketTimeoutMillis mSocket timeout value in milliseconds
     */
    public void setSocketTimeout(final int socketTimeoutMillis) {
        if (socketTimeoutMillis >= 0) {
            mSocketTimeout = socketTimeoutMillis;
        }
    }

    /**
     * get socket timeout in milliseconds ( 0 if no timeout not defined).
     *
     * @return mSocket timeout value (0 if not defined)
     */
    public int getSocketTimeout() {
        return mSocketTimeout;
    }

    /**
     * retrieve size of each packet sent to upload server.
     *
     * @return size of each packet sent to upload server
     */
    public int getUploadChunkSize() {
        return mUploadChunkSize;
    }

    /**
     * set size of each packet sent to upload server.
     *
     * @param uploadChunkSize new size of each packet sent to upload server
     */
    public void setUploadChunkSize(final int uploadChunkSize) {
        this.mUploadChunkSize = uploadChunkSize;
    }

    /**
     * Set the default RoundingMode for BigDecimal.
     *
     * @param roundingMode rounding mode.
     */
    public void setDefaultRoundingMode(final RoundingMode roundingMode) {
        this.mRoundingMode = roundingMode;
    }

    /**
     * Set the default scale for BigDecimal.
     *
     * @param scale mScale value
     */
    public void setDefaultScale(final int scale) {
        this.mScale = scale;
    }

    /**
     * retrieve rounding mode used for BigDecimal.
     *
     * @return rounding mode
     */
    public RoundingMode getDefaultRoundingMode() {
        return mRoundingMode;
    }

    /**
     * retrieve scale used for BigDecimal.
     *
     * @return mScale value
     */
    public int getDefaultScale() {
        return mScale;
    }
}
