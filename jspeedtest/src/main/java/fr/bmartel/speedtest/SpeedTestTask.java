/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016-2017 Bertrand Martel
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
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.inter.ISpeedTestSocket;
import fr.bmartel.speedtest.model.FtpMode;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.SpeedTestMode;
import fr.bmartel.speedtest.model.UploadStorageType;
import fr.bmartel.speedtest.utils.RandomGen;
import fr.bmartel.speedtest.utils.SpeedTestUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * This class manage all download/upload operations.
 *
 * @author Bertrand Martel
 */
public class SpeedTestTask {

    /**
     * socket server hostname.
     */
    private String mHostname = "";

    /**
     * socket server port.
     */
    private int mPort;

    /**
     * Protocol used (http/https/ftp...).
     */
    private String mProtocol;

    /**
     * proxy URL.
     */
    private URL mProxyUrl;

    /**
     * socket object.
     */
    private Socket mSocket;

    /**
     * start time triggered in millis.
     */
    private long mTimeStart;

    /**
     * start time for the current transfer rate computation.
     */
    private long mTimeComputeStart;

    /**
     * end time triggered in millis.
     */
    private long mTimeEnd;

    /**
     * this is the number of bit uploaded at this time.
     */
    private int mUploadTempFileSize;

    /**
     * number of bit uploaded since last transfer rate computation.
     */
    private int mUlComputationTempFileSize;

    /**
     * this is the number of packet downloaded at this time.
     */
    private int mDownloadTemporaryPacketSize;

    /**
     * number of packet download since the last computation.
     */
    private int mDlComputationTempPacketSize;

    /**
     * this is the number of packet to download.
     */
    private BigDecimal mDownloadPckSize = BigDecimal.ZERO;

    /**
     * FTP inputstream.
     */
    private InputStream mFtpInputstream;

    /**
     * FTP outputstream.
     */
    private OutputStream mFtpOutputstream;

    /**
     * define if an error has been dispatched already or not. This is reset to false on start download/ upload + in
     * reading thread
     */
    private boolean mErrorDispatched;

    /**
     * define if mSocket close error is to be expected.
     */
    private boolean mForceCloseSocket;

    /**
     * size of file to upload.
     */
    private BigDecimal mUploadFileSize = BigDecimal.ZERO;

    /**
     * SpeedTestSocket interface.
     */
    private final ISpeedTestSocket mSocketInterface;

    /**
     * Speed test repeat wrapper.
     */
    private final RepeatWrapper mRepeatWrapper;

    /**
     * Listener list.
     */
    private final List<ISpeedTestListener> mListenerList;

    /**
     * define if report interval is set.
     */
    private boolean mReportInterval;

    /**
     * executor service for reading operation.
     */
    private ExecutorService mReadExecutorService;

    /**
     * executor service for writing operation.
     */
    private ExecutorService mWriteExecutorService;

    /**
     * executor service used for reporting.
     */
    private ScheduledExecutorService mReportExecutorService;

    /**
     * current speed test mode.
     */
    private SpeedTestMode mSpeedTestMode = SpeedTestMode.NONE;

    /**
     * Build socket.
     *
     * @param socketInterface interface shared between repeat wrapper and speed test socket
     */
    public SpeedTestTask(final ISpeedTestSocket socketInterface, final List<ISpeedTestListener> listenerList) {
        mSocketInterface = socketInterface;
        mRepeatWrapper = mSocketInterface.getRepeatWrapper();
        mListenerList = listenerList;
        initThreadPool();
    }

    /**
     * initialize thread pool.
     */
    private void initThreadPool() {
        mReadExecutorService = Executors.newSingleThreadExecutor();
        mReportExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_REPORT_SIZE);
        mWriteExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Set report interval state.
     *
     * @param state define if a report interval is set
     */
    public void setReportInterval(final boolean state) {
        mReportInterval = state;
    }

    /**
     * Set proxy URI.
     *
     * @param proxyUri proxy URI
     * @return false if malformed
     */
    public boolean setProxy(final String proxyUri) {
        try {
            mProxyUrl = (proxyUri != null) ? new URL(proxyUri) : null;
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    /**
     * start download task.
     *
     * @param uri uri to fetch to download file
     */
    public void startDownloadRequest(final String uri) {

        mSpeedTestMode = SpeedTestMode.DOWNLOAD;

        mForceCloseSocket = false;
        mErrorDispatched = false;

        try {
            final URL url = new URL(uri);

            mProtocol = url.getProtocol();

            switch (mProtocol) {
                case "http":
                case "https":
                    String downloadRequest;

                    if (mProxyUrl != null) {
                        this.mHostname = mProxyUrl.getHost();
                        this.mPort = mProxyUrl.getPort() != -1 ? mProxyUrl.getPort() : 8080;
                        downloadRequest = "GET " + uri + " HTTP/1.1\r\n" + "Host: " + url.getHost() +
                                "\r\nProxy-Connection: Keep-Alive" + "\r\n\r\n";
                    } else {
                        this.mHostname = url.getHost();
                        if (url.getProtocol().equals("http")) {
                            this.mPort = url.getPort() != -1 ? url.getPort() : 80;
                        } else {
                            this.mPort = url.getPort() != -1 ? url.getPort() : 443;
                        }
                        downloadRequest = "GET " + uri + " HTTP/1.1\r\n" + "Host: " + url.getHost() + "\r\n\r\n";
                    }
                    writeDownload(downloadRequest.getBytes());
                    break;
                case "ftp":
                    final String userInfo = url.getUserInfo();
                    String user = SpeedTestConst.FTP_DEFAULT_USER;
                    String pwd = SpeedTestConst.FTP_DEFAULT_PASSWORD;

                    if (userInfo != null && userInfo.indexOf(':') != -1) {
                        user = userInfo.substring(0, userInfo.indexOf(':'));
                        pwd = userInfo.substring(userInfo.indexOf(':') + 1);
                    }
                    startFtpDownload(uri, user, pwd);
                    break;
                default:
                    SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                            SpeedTestError.UNSUPPORTED_PROTOCOL,
                            "unsupported protocol");
                    break;
            }
        } catch (MalformedURLException e) {
            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                    SpeedTestError.MALFORMED_URI,
                    e.getMessage());
        }
    }

    /**
     * Start upload request, distinguish protocol.
     *
     * @param uri           URI
     * @param fileSizeOctet file size to upload in octet
     */
    public void startUploadRequest(final String uri, final int fileSizeOctet) {

        mSpeedTestMode = SpeedTestMode.UPLOAD;

        mForceCloseSocket = false;
        mErrorDispatched = false;

        try {
            final URL url = new URL(uri);

            switch (url.getProtocol()) {
                case "http":
                case "https":
                    writeUpload(uri, fileSizeOctet);
                    break;
                case "ftp":
                    startFtpUpload(uri, fileSizeOctet);
                    break;
                default:
                    SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                            SpeedTestError.UNSUPPORTED_PROTOCOL,
                            "unsupported protocol");
                    break;
            }
        } catch (MalformedURLException e) {
            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                    SpeedTestError.MALFORMED_URI,
                    e.getMessage());
        }
    }

    /**
     * shutdown executors to release threads.
     */
    private void closeExecutors() {
        mReadExecutorService.shutdownNow();
        mReportExecutorService.shutdownNow();
        mWriteExecutorService.shutdownNow();
    }

    /**
     * Write upload POST request with file generated randomly.
     *
     * @param uri           URI
     * @param fileSizeOctet file size to upload in octet
     */
    public void writeUpload(final String uri, final int fileSizeOctet) {

        try {
            final URL url = new URL(uri);

            mProtocol = url.getProtocol();

            if (mProxyUrl != null) {
                this.mHostname = mProxyUrl.getHost();
                this.mPort = mProxyUrl.getPort() != -1 ? mProxyUrl.getPort() : 8080;
            } else {
                this.mHostname = url.getHost();
                if ("http".equals(mProtocol)) {
                    this.mPort = url.getPort() != -1 ? url.getPort() : 80;
                } else {
                    this.mPort = url.getPort() != -1 ? url.getPort() : 443;
                }
            }
            mUploadFileSize = new BigDecimal(fileSizeOctet);

            mUploadTempFileSize = 0;
            mUlComputationTempFileSize = 0;

            mTimeStart = System.nanoTime();
            mTimeComputeStart = System.nanoTime();

            connectAndExecuteTask(new Runnable() {
                @Override
                public void run() {
                    if (mSocket != null && !mSocket.isClosed()) {

                        RandomAccessFile uploadFile = null;
                        final RandomGen randomGen = new RandomGen();

                        try {

                            byte[] body = new byte[]{};

                            if (mSocketInterface.getUploadStorageType() == UploadStorageType.RAM_STORAGE) {
                                /* generate a file with size of fileSizeOctet octet */
                                body = randomGen.generateRandomArray(fileSizeOctet);
                            } else {
                                uploadFile = randomGen.generateRandomFile(fileSizeOctet);
                                uploadFile.seek(0);
                            }

                            String head;

                            if (mProxyUrl != null) {
                                head = "POST " + uri + " HTTP/1.1\r\n" + "Host: " + url.getHost() +
                                        "\r\nAccept: " + "*/*\r\nContent-Length: " + fileSizeOctet +
                                        "\r\nProxy-Connection: Keep-Alive" + "\r\n\r\n";
                            } else {
                                head = "POST " + uri + " HTTP/1.1\r\n" + "Host: " + url.getHost() +
                                        "\r\nAccept: " + "*/*\r\nContent-Length: " + fileSizeOctet + "\r\n\r\n";
                            }
                            mUploadTempFileSize = 0;
                            mUlComputationTempFileSize = 0;

                            final int uploadChunkSize = mSocketInterface.getUploadChunkSize();

                            final int step = fileSizeOctet / uploadChunkSize;
                            final int remain = fileSizeOctet % uploadChunkSize;

                            if (mSocket.getOutputStream() != null) {

                                if (writeFlushSocket(head.getBytes()) != 0) {
                                    throw new SocketTimeoutException();
                                }

                                mTimeStart = System.nanoTime();
                                mTimeComputeStart = System.nanoTime();
                                mTimeEnd = 0;

                                if (mRepeatWrapper.isFirstUpload()) {
                                    mRepeatWrapper.setFirstUploadRepeat(false);
                                    mRepeatWrapper.setStartDate(mTimeStart);
                                }

                                if (mRepeatWrapper.isRepeatUpload()) {
                                    mRepeatWrapper.updatePacketSize(mUploadFileSize);
                                }

                                for (int i = 0; i < step; i++) {

                                    final byte[] chunk = SpeedTestUtils.readUploadData(mSocketInterface
                                                    .getUploadStorageType(),
                                            body,
                                            uploadFile,
                                            mUploadTempFileSize,
                                            uploadChunkSize);

                                    if (writeFlushSocket(chunk) != 0) {
                                        throw new SocketTimeoutException();
                                    }

                                    mUploadTempFileSize += uploadChunkSize;
                                    mUlComputationTempFileSize += uploadChunkSize;

                                    if (mRepeatWrapper.isRepeatUpload()) {
                                        mRepeatWrapper.updateTempPacketSize(uploadChunkSize);
                                    }

                                    if (!mReportInterval) {
                                        final SpeedTestReport report = getReport(SpeedTestMode.UPLOAD);

                                        for (int j = 0; j < mListenerList.size(); j++) {
                                            mListenerList.get(j).onProgress(report.getProgressPercent(), report);
                                        }
                                    }
                                }

                                final byte[] chunk = SpeedTestUtils.readUploadData(mSocketInterface
                                                .getUploadStorageType(),
                                        body,
                                        uploadFile,
                                        mUploadTempFileSize,
                                        remain);

                                if (remain != 0 && writeFlushSocket(chunk) != 0) {
                                    throw new SocketTimeoutException();
                                } else {

                                    mUploadTempFileSize += remain;
                                    mUlComputationTempFileSize += remain;

                                    if (mRepeatWrapper.isRepeatUpload()) {
                                        mRepeatWrapper.updateTempPacketSize(remain);
                                    }
                                }

                                if (!mReportInterval) {
                                    final SpeedTestReport report = getReport(SpeedTestMode.UPLOAD);

                                    for (int j = 0; j < mListenerList.size(); j++) {
                                        mListenerList.get(j).onProgress(SpeedTestConst.PERCENT_MAX.floatValue(),
                                                report);

                                    }
                                }
                            }
                        } catch (SocketTimeoutException e) {
                            mReportInterval = false;
                            mErrorDispatched = true;
                            closeSocket();
                            closeExecutors();
                            if (!mForceCloseSocket) {
                                SpeedTestUtils.dispatchSocketTimeout(mForceCloseSocket, mListenerList, SpeedTestConst
                                        .SOCKET_WRITE_ERROR);
                            } else {
                                SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                                        e.getMessage());
                            }
                        } catch (IOException e) {
                            mReportInterval = false;
                            mErrorDispatched = true;
                            closeExecutors();
                            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket,
                                    mListenerList, e.getMessage());
                        } finally {
                            if (uploadFile != null) {
                                try {
                                    uploadFile.close();
                                    randomGen.deleteFile();
                                } catch (IOException e) {
                                    //e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }, false, fileSizeOctet);
        } catch (MalformedURLException e) {
            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                    SpeedTestError.MALFORMED_URI,
                    e.getMessage());
        }
    }

    /**
     * Create and connect mSocket.
     *
     * @param task       task to be executed when connected to mSocket
     * @param download   define if it is a download or upload test
     * @param uploadSize upload package size (if !download)
     */
    private void connectAndExecuteTask(final Runnable task, final boolean download, final int uploadSize) {

        // close mSocket before recreating it
        if (mSocket != null) {
            closeSocket();
        }
        try {
            if ("https".equals(mProtocol)) {
                final SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                mSocket = ssf.createSocket();
            } else {
                mSocket = new Socket();
            }

            if (mSocketInterface.getSocketTimeout() != 0 && download) {
                mSocket.setSoTimeout(mSocketInterface.getSocketTimeout());
            }

            /* establish mSocket parameters */
            mSocket.setReuseAddress(true);

            mSocket.setKeepAlive(true);

            mSocket.connect(new InetSocketAddress(mHostname, mPort));

            if (mReadExecutorService == null || mReadExecutorService.isShutdown()) {
                mReadExecutorService = Executors.newSingleThreadExecutor();
            }

            mReadExecutorService.execute(new Runnable() {

                @Override
                public void run() {

                    if (download) {
                        startSocketDownloadTask(mProtocol, mHostname);
                    } else {
                        startSocketUploadTask(mHostname, uploadSize);
                    }
                }
            });

            if (mWriteExecutorService == null || mWriteExecutorService.isShutdown()) {
                mWriteExecutorService = Executors.newSingleThreadExecutor();
            }

            mWriteExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (task != null) {
                        task.run();
                    }
                }
            });

        } catch (IOException e) {
            if (!mErrorDispatched) {
                SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList, e.getMessage());
            }
        }
    }

    /**
     * start download reading task.
     *
     * @String hostname hostname to reach
     */
    private void startSocketDownloadTask(final String protocol, final String hostname) {

        mDownloadTemporaryPacketSize = 0;
        mDlComputationTempPacketSize = 0;

        try {
            final HttpFrame httpFrame = new HttpFrame();

            final HttpStates httFrameState = httpFrame.decodeFrame(mSocket.getInputStream());

            SpeedTestUtils.checkHttpFrameError(mForceCloseSocket, mListenerList, httFrameState);

            final HttpStates httpHeaderState = httpFrame.parseHeader(mSocket.getInputStream());
            SpeedTestUtils.checkHttpHeaderError(mForceCloseSocket, mListenerList, httpHeaderState);

            if (httpFrame.getStatusCode() == SpeedTestConst.HTTP_OK &&
                    httpFrame.getReasonPhrase().equalsIgnoreCase("ok")) {

                SpeedTestUtils.checkHttpContentLengthError(mForceCloseSocket,
                        mListenerList, httpFrame);

                mDownloadPckSize = new BigDecimal(httpFrame.getContentLength());

                if (mRepeatWrapper.isRepeatDownload()) {
                    mRepeatWrapper.updatePacketSize(mDownloadPckSize);
                }

                mTimeStart = System.nanoTime();
                mTimeComputeStart = System.nanoTime();
                mTimeEnd = 0;

                if (mRepeatWrapper.isFirstDownload()) {
                    mRepeatWrapper.setFirstDownloadRepeat(false);
                    mRepeatWrapper.setStartDate(mTimeStart);
                }

                downloadReadingLoop();
                mTimeEnd = System.nanoTime();

                closeSocket();

                mReportInterval = false;

                if (!mRepeatWrapper.isRepeatDownload()) {
                    closeExecutors();
                }

                final SpeedTestReport report = getReport(SpeedTestMode.DOWNLOAD);

                for (int i = 0; i < mListenerList.size(); i++) {
                    mListenerList.get(i).onCompletion(report);
                }

            } else if ((httpFrame.getStatusCode() == 301 ||
                    httpFrame.getStatusCode() == 302 ||
                    httpFrame.getStatusCode() == 307) &&
                    httpFrame.getHeaders().containsKey("location")) {
                // redirect to Location
                final String location = httpFrame.getHeaders().get("location");

                if (location.charAt(0) == '/') {
                    mReportInterval = false;
                    finishTask();
                    startDownloadRequest(protocol + "://" + hostname + location);
                } else {
                    mReportInterval = false;
                    finishTask();
                    startDownloadRequest(location);
                }
            } else {

                mReportInterval = false;

                for (int i = 0; i < mListenerList.size(); i++) {
                    mListenerList.get(i).onError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error status code " +
                            httpFrame.getStatusCode());
                }

                finishTask();
            }

        } catch (
                SocketTimeoutException e
                )

        {
            mReportInterval = false;
            SpeedTestUtils.dispatchSocketTimeout(mForceCloseSocket, mListenerList, e.getMessage());
            mTimeEnd = System.nanoTime();
            closeSocket();
            closeExecutors();
        } catch (IOException |
                InterruptedException e
                )

        {
            mReportInterval = false;
            catchError(e.getMessage());
        }

        mErrorDispatched = false;
    }

    private void finishTask() {
        closeSocket();
        if (!mRepeatWrapper.isRepeatDownload()) {
            closeExecutors();
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
            mDlComputationTempPacketSize += read;

            if (mRepeatWrapper.isRepeatDownload()) {
                mRepeatWrapper.updateTempPacketSize(read);
            }

            if (!mReportInterval) {
                final SpeedTestReport report = getReport(SpeedTestMode.DOWNLOAD);
                for (int i = 0; i < mListenerList.size(); i++) {
                    mListenerList.get(i).onProgress(report.getProgressPercent(), report);
                }
            }

            if (mDownloadTemporaryPacketSize == mDownloadPckSize.longValueExact()) {
                break;
            }
        }
    }

    /**
     * start upload writing task.
     *
     * @param hostname hostname to reach
     * @param size     upload packet size
     */
    private void startSocketUploadTask(final String hostname, final int size) {

        try {
            final HttpFrame frame = new HttpFrame();

            final HttpStates httpStates = frame.parseHttp(mSocket.getInputStream());

            if (httpStates == HttpStates.HTTP_FRAME_OK) {

                if (frame.getStatusCode() == SpeedTestConst.HTTP_OK && frame.getReasonPhrase().equalsIgnoreCase("ok")) {

                    mTimeEnd = System.nanoTime();
                    mReportInterval = false;

                    finishTask();

                    final SpeedTestReport report = getReport(SpeedTestMode.UPLOAD);

                    for (int i = 0; i < mListenerList.size(); i++) {
                        mListenerList.get(i).onCompletion(report);
                    }

                } else if ((frame.getStatusCode() == 301 ||
                        frame.getStatusCode() == 302 ||
                        frame.getStatusCode() == 307) &&
                        frame.getHeaders().containsKey("location")) {
                    // redirect to Location
                    final String location = frame.getHeaders().get("location");

                    if (location.charAt(0) == '/') {
                        mReportInterval = false;
                        finishTask();
                        startUploadRequest("http://" + hostname + location, size);
                    } else if (location.startsWith("https")) {
                        //unsupported protocol
                        mReportInterval = false;
                        for (int i = 0; i < mListenerList.size(); i++) {
                            mListenerList.get(i).onError(SpeedTestError.UNSUPPORTED_PROTOCOL, "unsupported protocol :" +
                                    " " +
                                    "https");
                        }
                        finishTask();
                    } else {
                        mReportInterval = false;
                        finishTask();
                        startUploadRequest(location, size);
                    }
                } else {
                    mReportInterval = false;

                    for (int i = 0; i < mListenerList.size(); i++) {
                        mListenerList.get(i).onError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error status code" +
                                " " + frame.getStatusCode());
                    }
                    finishTask();
                }
                return;
            }
            closeSocket();
            if (!mErrorDispatched && !mForceCloseSocket) {
                for (int i = 0; i < mListenerList.size(); i++) {
                    mListenerList.get(i).onError(SpeedTestError.SOCKET_ERROR, "mSocket error");
                }
            }
            closeExecutors();
        } catch (IOException | InterruptedException e) {
            mReportInterval = false;
            if (!mErrorDispatched) {
                catchError(e.getMessage());
            }
        }
        mErrorDispatched = false;
    }


    /**
     * Write download request to server host.
     *
     * @param data HTTP request to send to initiate download process
     */
    private void writeDownload(final byte[] data) {

        connectAndExecuteTask(new Runnable() {
            @Override
            public void run() {

                if (mSocket != null && !mSocket.isClosed()) {

                    try {
                        if ((mSocket.getOutputStream() != null) && (writeFlushSocket(data) != 0)) {
                            throw new SocketTimeoutException();
                        }
                    } catch (SocketTimeoutException e) {
                        SpeedTestUtils.dispatchSocketTimeout(mForceCloseSocket, mListenerList, SpeedTestConst
                                .SOCKET_WRITE_ERROR);
                        closeSocket();
                        closeExecutors();
                    } catch (IOException e) {
                        SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket,
                                mListenerList, e.getMessage());
                        closeExecutors();
                    }
                }
            }
        }, true, 0);
    }

    /**
     * logout & disconnect FTP client.
     *
     * @param ftpclient ftp client
     */
    private void disconnectFtp(final FTPClient ftpclient) {

        try {
            if (ftpclient.isConnected()) {
                ftpclient.logout();
                ftpclient.disconnect();
            }
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
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

        @SuppressWarnings("unchecked") final Future<Integer> future = executor.submit(new Callable() {

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
            status = future.get(mSocketInterface.getSocketTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            status = -1;
        } catch (InterruptedException | ExecutionException e) {
            status = -1;
        }
        executor.shutdownNow();
        return status;
    }

    /**
     * catch an error.
     *
     * @param errorMessage error message from Exception
     */
    private void catchError(final String errorMessage) {
        mTimeEnd = System.nanoTime();
        closeSocket();
        closeExecutors();
        SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList, errorMessage);
    }


    /**
     * get a download/upload report.
     *
     * @param mode speed test mode requested
     * @return speed test report
     */
    public SpeedTestReport getReport(final SpeedTestMode mode) {

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
            currentTime = System.nanoTime();
        } else {
            currentTime = mTimeEnd;
        }

        BigDecimal transferRateOps = BigDecimal.ZERO;

        final int scale = mSocketInterface.getDefaultScale();
        final RoundingMode roundingMode = mSocketInterface.getDefaultRoundingMode();

        switch (mSocketInterface.getComputationMethod()) {
            case MEDIAN_ALL_TIME:
                BigDecimal dividerAllTime = new BigDecimal(currentTime - mTimeComputeStart)
                        .divide(SpeedTestConst.NANO_DIVIDER, scale, roundingMode);

                if (shallCalculateTransferRate(currentTime) && dividerAllTime.compareTo(BigDecimal.ZERO) != 0) {
                    transferRateOps = temporaryPacketSize.divide(dividerAllTime, scale, roundingMode);
                }
                break;
            case MEDIAN_INTERVAL:

                final BigDecimal tempPacket = (mode == SpeedTestMode.DOWNLOAD) ? new BigDecimal
                        (mDlComputationTempPacketSize) : new BigDecimal(mUlComputationTempFileSize);

                BigDecimal dividerMedian = new BigDecimal(currentTime - mTimeComputeStart)
                        .divide(SpeedTestConst.NANO_DIVIDER, scale, roundingMode);

                if (shallCalculateTransferRate(currentTime) && dividerMedian.compareTo(BigDecimal.ZERO) != 0) {
                    transferRateOps = tempPacket.divide(dividerMedian, scale, roundingMode);
                }
                // reset those values for the next computation
                mDlComputationTempPacketSize = 0;
                mUlComputationTempFileSize = 0;
                mTimeComputeStart = System.nanoTime();
                break;
            default:
                break;
        }

        final BigDecimal transferRateBitps = transferRateOps.multiply(SpeedTestConst.BIT_MULTIPLIER);

        BigDecimal percent = BigDecimal.ZERO;

        SpeedTestReport report;

        if (mRepeatWrapper.isRepeat()) {
            report = mRepeatWrapper.getRepeatReport(scale, roundingMode, mode, currentTime, transferRateOps);
        } else {
            if (totalPacketSize.compareTo(BigDecimal.ZERO) != 0) {
                percent = temporaryPacketSize.multiply(SpeedTestConst.PERCENT_MAX).divide(totalPacketSize, scale,
                        roundingMode);
            }
            report = new SpeedTestReport(mode, percent.floatValue(),
                    mTimeStart, currentTime, temporaryPacketSize.longValueExact(), totalPacketSize.longValueExact(),
                    transferRateOps, transferRateBitps,
                    1);
        }
        return report;
    }

    /**
     * Check setup time depending on elapsed time.
     *
     * @param currentTime elapsed time since upload/download has started
     * @return status if transfer rate should be computed at this time
     */
    private boolean shallCalculateTransferRate(final long currentTime) {

        final long elapsedTime = currentTime - mTimeStart;

        boolean ret = true;

        switch (mSpeedTestMode) {
            case DOWNLOAD:
                ret = (elapsedTime > mSocketInterface.getDownloadSetupTime());
                break;
            case UPLOAD:
                ret = (elapsedTime > mSocketInterface.getUploadSetupTime());
                break;
            default:
        }

        return ret;
    }

    /**
     * Get FTP file size.
     *
     * @param ftpClient ftp client
     * @param filePath  remote file path
     * @return file size
     * @throws Exception file read/write IOException
     */
    private long getFileSize(final FTPClient ftpClient, final String filePath) throws IOException {

        long fileSize = 0;
        final FTPFile[] files = ftpClient.listFiles(filePath);
        if (files.length == 1 && files[0].isFile()) {
            fileSize = files[0].getSize();
        }
        return fileSize;
    }

    /**
     * start FTP download with specific port, user, password.
     *
     * @param uri      ftp uri
     * @param user     ftp username
     * @param password ftp password
     */
    public void startFtpDownload(
            final String uri,
            final String user,
            final String password) {

        mSpeedTestMode = SpeedTestMode.DOWNLOAD;

        try {
            final URL url = new URL(uri);

            mErrorDispatched = false;
            mForceCloseSocket = false;

            if (mReadExecutorService == null || mReadExecutorService.isShutdown()) {
                mReadExecutorService = Executors.newSingleThreadExecutor();
            }

            mReadExecutorService.execute(new Runnable() {

                @Override
                public void run() {

                    final FTPClient ftpclient = new FTPClient();

                    try {
                        ftpclient.connect(url.getHost(), url.getPort() != -1 ? url.getPort() : 21);
                        ftpclient.login(user, password);
                        if (mSocketInterface.getFtpMode() == FtpMode.PASSIVE) {
                            ftpclient.enterLocalPassiveMode();
                        } else {
                            ftpclient.enterLocalActiveMode();
                        }
                        ftpclient.setFileType(FTP.BINARY_FILE_TYPE);

                        mDownloadTemporaryPacketSize = 0;
                        mDlComputationTempPacketSize = 0;

                        mTimeStart = System.nanoTime();
                        mTimeComputeStart = System.nanoTime();

                        mTimeEnd = 0;

                        if (mRepeatWrapper.isFirstDownload()) {
                            mRepeatWrapper.setFirstDownloadRepeat(false);
                            mRepeatWrapper.setStartDate(mTimeStart);
                        }

                        mDownloadPckSize = new BigDecimal(getFileSize(ftpclient, url.getPath()));

                        if (mRepeatWrapper.isRepeatDownload()) {
                            mRepeatWrapper.updatePacketSize(mDownloadPckSize);
                        }

                        mFtpInputstream = ftpclient.retrieveFileStream(url.getPath());

                        if (mFtpInputstream != null) {

                            final byte[] bytesArray = new byte[SpeedTestConst.READ_BUFFER_SIZE];

                            int read;
                            while ((read = mFtpInputstream.read(bytesArray)) != -1) {

                                mDownloadTemporaryPacketSize += read;
                                mDlComputationTempPacketSize += read;

                                if (mRepeatWrapper.isRepeatDownload()) {
                                    mRepeatWrapper.updateTempPacketSize(read);
                                }

                                if (!mReportInterval) {
                                    final SpeedTestReport report = getReport(SpeedTestMode.DOWNLOAD);

                                    for (int i = 0; i < mListenerList.size(); i++) {
                                        mListenerList.get(i).onProgress(report.getProgressPercent(), report);
                                    }
                                }

                                if (mDownloadTemporaryPacketSize == mDownloadPckSize.longValueExact()) {
                                    break;
                                }
                            }

                            mFtpInputstream.close();

                            mTimeEnd = System.nanoTime();

                            mReportInterval = false;
                            final SpeedTestReport report = getReport(SpeedTestMode.DOWNLOAD);

                            for (int i = 0; i < mListenerList.size(); i++) {
                                mListenerList.get(i).onCompletion(report);
                            }

                        } else {
                            mReportInterval = false;
                            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket,
                                    mListenerList, "cant create stream " +
                                            "from uri " + uri + " with reply code : " + ftpclient.getReplyCode());
                        }

                        if (!mRepeatWrapper.isRepeatDownload()) {
                            closeExecutors();
                        }

                    } catch (IOException e) {
                        //e.printStackTrace();
                        mReportInterval = false;
                        catchError(e.getMessage());
                    } finally {
                        mErrorDispatched = false;
                        disconnectFtp(ftpclient);
                    }
                }
            });
        } catch (MalformedURLException e) {
            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                    SpeedTestError.MALFORMED_URI,
                    e.getMessage());
        }
    }


    /**
     * Start FTP upload.
     *
     * @param uri           upload uri
     * @param fileSizeOctet file size in octet
     */
    public void startFtpUpload(
            final String uri,
            final int fileSizeOctet) {

        mSpeedTestMode = SpeedTestMode.UPLOAD;

        mUploadFileSize = new BigDecimal(fileSizeOctet);
        mForceCloseSocket = false;
        mErrorDispatched = false;

        try {
            final URL url = new URL(uri);

            final String userInfo = url.getUserInfo();
            String user = SpeedTestConst.FTP_DEFAULT_USER;
            String pwd = SpeedTestConst.FTP_DEFAULT_PASSWORD;

            if (userInfo != null && userInfo.indexOf(':') != -1) {
                user = userInfo.substring(0, userInfo.indexOf(':'));
                pwd = userInfo.substring(userInfo.indexOf(':') + 1);
            }

            if (mWriteExecutorService == null || mWriteExecutorService.isShutdown()) {
                mWriteExecutorService = Executors.newSingleThreadExecutor();
            }

            final String finalUser = user;
            final String finalPwd = pwd;
            mWriteExecutorService.execute(new Runnable() {
                @Override
                public void run() {

                    final FTPClient ftpClient = new FTPClient();
                    final RandomGen randomGen = new RandomGen();

                    RandomAccessFile uploadFile = null;

                    try {
                        ftpClient.connect(url.getHost(), url.getPort() != -1 ? url.getPort() : 21);
                        ftpClient.login(finalUser, finalPwd);
                        if (mSocketInterface.getFtpMode() == FtpMode.PASSIVE) {
                            ftpClient.enterLocalPassiveMode();
                        } else {
                            ftpClient.enterLocalActiveMode();
                        }
                        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                        byte[] fileContent = new byte[]{};

                        if (mSocketInterface.getUploadStorageType() == UploadStorageType.RAM_STORAGE) {
                            /* generate a file with size of fileSizeOctet octet */
                            fileContent = randomGen.generateRandomArray(fileSizeOctet);
                        } else {
                            uploadFile = randomGen.generateRandomFile(fileSizeOctet);
                            uploadFile.seek(0);
                        }

                        mFtpOutputstream = ftpClient.storeFileStream(url.getPath());

                        if (mFtpOutputstream != null) {

                            mUploadTempFileSize = 0;
                            mUlComputationTempFileSize = 0;

                            final int uploadChunkSize = mSocketInterface.getUploadChunkSize();

                            final int step = fileSizeOctet / uploadChunkSize;
                            final int remain = fileSizeOctet % uploadChunkSize;

                            mTimeStart = System.nanoTime();
                            mTimeComputeStart = System.nanoTime();
                            mTimeEnd = 0;

                            if (mRepeatWrapper.isFirstUpload()) {
                                mRepeatWrapper.setFirstUploadRepeat(false);
                                mRepeatWrapper.setStartDate(mTimeStart);
                            }

                            if (mRepeatWrapper.isRepeatUpload()) {
                                mRepeatWrapper.updatePacketSize(mUploadFileSize);
                            }

                            if (mForceCloseSocket) {
                                mFtpOutputstream.close();
                                mReportInterval = false;
                                if (!mRepeatWrapper.isRepeatUpload()) {
                                    closeExecutors();
                                }
                                SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList, "");
                            } else {
                                for (int i = 0; i < step; i++) {


                                    final byte[] chunk = SpeedTestUtils.readUploadData(mSocketInterface
                                                    .getUploadStorageType(),
                                            fileContent,
                                            uploadFile,
                                            mUploadTempFileSize,
                                            uploadChunkSize);

                                    mFtpOutputstream.write(chunk, 0, uploadChunkSize);

                                    mUploadTempFileSize += uploadChunkSize;
                                    mUlComputationTempFileSize += uploadChunkSize;

                                    if (mRepeatWrapper.isRepeatUpload()) {
                                        mRepeatWrapper.updateTempPacketSize(uploadChunkSize);
                                    }

                                    if (!mReportInterval) {

                                        final SpeedTestReport report = getReport(SpeedTestMode.UPLOAD);

                                        for (int j = 0; j < mListenerList.size(); j++) {
                                            mListenerList.get(j).onProgress(report.getProgressPercent(), report);
                                        }
                                    }
                                }

                                if (remain != 0) {

                                    final byte[] chunk = SpeedTestUtils.readUploadData(mSocketInterface
                                                    .getUploadStorageType(),
                                            fileContent,
                                            uploadFile,
                                            mUploadTempFileSize,
                                            remain);

                                    mFtpOutputstream.write(chunk, 0, remain);

                                    mUploadTempFileSize += remain;
                                    mUlComputationTempFileSize += remain;

                                    if (mRepeatWrapper.isRepeatUpload()) {
                                        mRepeatWrapper.updateTempPacketSize(remain);
                                    }
                                }
                                if (!mReportInterval) {
                                    final SpeedTestReport report = getReport(SpeedTestMode.UPLOAD);

                                    for (int j = 0; j < mListenerList.size(); j++) {
                                        mListenerList.get(j).onProgress(SpeedTestConst.PERCENT_MAX.floatValue(),
                                                report);

                                    }
                                }
                                mTimeEnd = System.nanoTime();
                                mFtpOutputstream.close();
                                mReportInterval = false;

                                if (!mRepeatWrapper.isRepeatUpload()) {
                                    closeExecutors();
                                }

                                final SpeedTestReport report = getReport(SpeedTestMode.UPLOAD);

                                for (int i = 0; i < mListenerList.size(); i++) {
                                    mListenerList.get(i).onCompletion(report);
                                }
                            }
                        } else {
                            mReportInterval = false;
                            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket,
                                    mListenerList, "cant create stream" +
                                            " " +
                                            "from uri " + uri + " with reply code : " + ftpClient.getReplyCode());
                        }
                    } catch (SocketTimeoutException e) {
                        //e.printStackTrace();
                        mReportInterval = false;
                        mErrorDispatched = true;
                        if (!mForceCloseSocket) {
                            SpeedTestUtils.dispatchSocketTimeout(mForceCloseSocket, mListenerList, SpeedTestConst
                                    .SOCKET_WRITE_ERROR);
                        } else {
                            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket,
                                    mListenerList, e.getMessage());
                        }
                        closeSocket();
                        closeExecutors();
                    } catch (IOException e) {
                        //e.printStackTrace();
                        mReportInterval = false;
                        mErrorDispatched = true;
                        SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket,
                                mListenerList, e.getMessage());
                        closeExecutors();
                    } finally {
                        mErrorDispatched = false;
                        disconnectFtp(ftpClient);
                        if (uploadFile != null) {
                            try {
                                uploadFile.close();
                                randomGen.deleteFile();
                            } catch (IOException e) {
                                //e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } catch (MalformedURLException e) {
            SpeedTestUtils.dispatchError(mSocketInterface, mForceCloseSocket, mListenerList,
                    SpeedTestError.MALFORMED_URI,
                    e.getMessage());
        }
    }

    /**
     * Close socket streams and mSocket object.
     */
    public void closeSocket() {

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * close socket / stop download/upload operations.
     */
    public void forceStopTask() {
        mForceCloseSocket = true;
        if (mFtpInputstream != null) {
            try {
                mFtpInputstream.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
        if (mFtpOutputstream != null) {
            try {
                mFtpOutputstream.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    /**
     * Shutdown threadpool and wait for task completion.
     */
    public void shutdownAndWait() {
        closeExecutors();
        try {
            mReadExecutorService.awaitTermination(SpeedTestConst.THREADPOOL_WAIT_COMPLETION_MS, TimeUnit.MILLISECONDS);
            mWriteExecutorService.awaitTermination(SpeedTestConst.THREADPOOL_WAIT_COMPLETION_MS, TimeUnit.MILLISECONDS);
            mReportExecutorService.awaitTermination(SpeedTestConst.THREADPOOL_WAIT_COMPLETION_MS,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    /**
     * reset report threadpool if necessary.
     */
    public void renewReportThreadPool() {
        if (mReportExecutorService == null || mReportExecutorService.isShutdown()) {
            mReportExecutorService = Executors.newScheduledThreadPool(SpeedTestConst.THREAD_POOL_REPORT_SIZE);
        }
    }

    /**
     * retrieve threadpool used to publish reports.
     *
     * @return report threadpool
     */
    public ScheduledExecutorService getReportThreadPool() {
        return mReportExecutorService;
    }

    /**
     * Check if report interval is set.
     *
     * @return report interval
     */
    public boolean isReportInterval() {
        return mReportInterval;
    }

    /**
     * retrieve current speed test mode.
     *
     * @return speed test mode (UPLOAD/DOWNLOAD/NONE)
     */
    public SpeedTestMode getSpeedTestMode() {
        return mSpeedTestMode;
    }
}
