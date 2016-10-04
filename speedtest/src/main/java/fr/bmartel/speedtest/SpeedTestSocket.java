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

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
     * Speed test repeat wrapper.
     */
    private final RepeatWrapper mRepeatWrapper = new RepeatWrapper(this);

    /**
     * Speed tets task object used to manage download/upload operations.
     */
    private final SpeedTestTask mTask = new SpeedTestTask(this, mListenerList);

    /**
     * initialize report task.
     *
     * @param reportInterval report interval in milliseconds
     * @param download       define if download or upload report should be dispatched
     */
    private void initReportTask(final int reportInterval, final boolean download) {

        mTask.renewReportThreadPool();

        mTask.getReportThreadPool().scheduleAtFixedRate(new Runnable() {
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
     * Shutdown threadpool and wait for task completion.
     */
    @Override
    public void shutdownAndWait() {
        mTask.shutdownAndWait();
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

        mTask.renewReportThreadPool();

        mTask.getReportThreadPool().schedule(new Runnable() {
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
        initReportTask(reportInterval, true);
        mTask.setReportInterval(true);
        startFixedDownload(hostname, port, uri, maxDuration);
    }

    /**
     * Start download process with default to port 80.
     *
     * @param hostname server mHostname
     * @param uri      uri to fetch to download file
     */
    public void startDownload(final String hostname,
                              final String uri) {
        startDownload(hostname, SpeedTestConst.HTTP_DEFAULT_PORT, uri);
    }

    /**
     * Start download process with default to port 80 with specified report interval.
     *
     * @param hostname       server mHostname
     * @param uri            uri to fetch to download file
     * @param reportInterval report interval in milliseconds
     */
    public void startDownload(final String hostname,
                              final String uri,
                              final int reportInterval) {
        startDownload(hostname, SpeedTestConst.HTTP_DEFAULT_PORT, uri, reportInterval);
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
        initReportTask(reportInterval, true);
        mTask.setReportInterval(true);
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
        mTask.startDownloadRequest(hostname, port, uri);
    }

    /**
     * start FTP download on default port 21.
     *
     * @param hostname       ftp host
     * @param uri            ftp uri
     * @param reportInterval report interval in milliseconds
     */
    public void startFtpDownload(final String hostname,
                                 final String uri,
                                 final int reportInterval) {

        initReportTask(reportInterval, true);
        mTask.setReportInterval(true);

        startFtpDownload(hostname,
                SpeedTestConst.FTP_DEFAULT_PORT,
                uri,
                SpeedTestConst.FTP_DEFAULT_USER,
                SpeedTestConst.FTP_DEFAULT_PASSWORD);
    }

    /**
     * start fixed FTP download on default port 21.
     *
     * @param hostname       ftp host
     * @param uri            ftp uri
     * @param reportInterval report interval in milliseconds
     */
    public void startFtpFixedDownload(final String hostname,
                                      final String uri,
                                      final int maxDuration,
                                      final int reportInterval) {
        initReportTask(reportInterval, true);
        mTask.setReportInterval(true);
        startFtpFixedDownload(hostname, uri, maxDuration);
    }

    /**
     * start FTP download on default port 21.
     *
     * @param hostname ftp host
     * @param uri      ftp uri
     */
    public void startFtpFixedDownload(final String hostname,
                                      final String uri,
                                      final int maxDuration) {

        mTask.renewReportThreadPool();

        mTask.getReportThreadPool().schedule(new Runnable() {
            @Override
            public void run() {
                forceStopTask();
            }
        }, maxDuration, TimeUnit.MILLISECONDS);

        startFtpDownload(hostname,
                SpeedTestConst.FTP_DEFAULT_PORT,
                uri,
                SpeedTestConst.FTP_DEFAULT_USER,
                SpeedTestConst.FTP_DEFAULT_PASSWORD);
    }

    /**
     * start FTP download on default port 21.
     *
     * @param hostname ftp host
     * @param uri      ftp uri
     */
    public void startFtpDownload(final String hostname,
                                 final String uri) {
        startFtpDownload(hostname,
                SpeedTestConst.FTP_DEFAULT_PORT,
                uri,
                SpeedTestConst.FTP_DEFAULT_USER,
                SpeedTestConst.FTP_DEFAULT_PASSWORD);
    }

    /**
     * start FTP download with specific port, user, password.
     *
     * @param hostname ftp host
     * @param uri      ftp uri
     * @param user     ftp username
     * @param password ftp password
     */
    public void startFtpDownload(final String hostname,
                                 final int port,
                                 final String uri,
                                 final String user,
                                 final String password) {

        mTask.startFtpDownload(hostname, port, uri, user, password);
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

        mTask.renewReportThreadPool();

        mTask.getReportThreadPool().schedule(new Runnable() {
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
     * @param hostname       server hostname
     * @param port           server port
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

        mTask.setReportInterval(true);
        startFixedUpload(hostname, port, uri, fileSizeOctet, maxDuration);
    }

    /**
     * Start FTP upload for a fixed duration.
     *
     * @param hostname       server hostname
     * @param uri            ftp uri
     * @param fileSizeOctet  file size to upload in octet
     * @param maxDuration    max duration of upload in milliseconds
     * @param reportInterval report interval in milliseconds
     */
    public void startFtpFixedUpload(final String hostname,
                                    final String uri,
                                    final int fileSizeOctet,
                                    final int maxDuration,
                                    final int reportInterval) {

        initReportTask(reportInterval, false);
        mTask.setReportInterval(true);

        startFtpFixedUpload(hostname, uri, fileSizeOctet, maxDuration);
    }

    /**
     * Start FTP upload for a fixed duration.
     *
     * @param hostname      server hostname
     * @param uri           ftp uri
     * @param fileSizeOctet file size to upload in octet
     * @param maxDuration   max duration of upload in milliseconds
     */
    public void startFtpFixedUpload(final String hostname,
                                    final String uri,
                                    final int fileSizeOctet,
                                    final int maxDuration) {

        mTask.renewReportThreadPool();

        mTask.getReportThreadPool().schedule(new Runnable() {
            @Override
            public void run() {
                forceStopTask();
            }
        }, maxDuration, TimeUnit.MILLISECONDS);

        startFtpUpload(hostname, SpeedTestConst.FTP_DEFAULT_PORT, uri, fileSizeOctet,
                SpeedTestConst.FTP_DEFAULT_USER, SpeedTestConst.FTP_DEFAULT_PASSWORD);
    }

    /**
     * Start FTP upload.
     *
     * @param hostname       server hostname
     * @param uri            ftp uri
     * @param fileSizeOctet  file size to upload in octet
     * @param reportInterval report interval in milliseconds
     */
    public void startFtpUpload(final String hostname,
                               final String uri,
                               final int fileSizeOctet,
                               final int reportInterval) {

        initReportTask(reportInterval, false);
        mTask.setReportInterval(true);

        startFtpUpload(hostname, SpeedTestConst.FTP_DEFAULT_PORT, uri, fileSizeOctet,
                SpeedTestConst.FTP_DEFAULT_USER, SpeedTestConst.FTP_DEFAULT_PASSWORD);
    }

    /**
     * Start FTP upload.
     *
     * @param hostname      server hostname
     * @param uri           ftp uri
     * @param fileSizeOctet file size to upload in octet
     */
    public void startFtpUpload(final String hostname,
                               final String uri,
                               final int fileSizeOctet) {

        startFtpUpload(hostname, SpeedTestConst.FTP_DEFAULT_PORT, uri, fileSizeOctet,
                SpeedTestConst.FTP_DEFAULT_USER, SpeedTestConst.FTP_DEFAULT_PASSWORD);
    }

    /**
     * Start FTP upload.
     *
     * @param hostname      ftp host
     * @param port          ftp port
     * @param uri           upload uri
     * @param fileSizeOctet file size in octet
     * @param user          username
     * @param password      password
     */
    public void startFtpUpload(final String hostname,
                               final int port,
                               final String uri,
                               final int fileSizeOctet,
                               final String user,
                               final String password) {
        mTask.startFtpUpload(hostname, port, uri, fileSizeOctet, user, password);
    }

    /**
     * Start upload process with default port 80.
     *
     * @param hostname      server mHostname
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     */
    public void startUpload(final String hostname,
                            final String uri,
                            final int fileSizeOctet) {
        startUpload(hostname, SpeedTestConst.HTTP_DEFAULT_PORT, uri, fileSizeOctet);
    }

    /**
     * Start upload process with default port 80 & specified report interval.
     *
     * @param hostname       server mHostname
     * @param uri            uri to fetch
     * @param fileSizeOctet  size of file to upload
     * @param reportInterval report interval in milliseconds
     */
    public void startUpload(final String hostname,
                            final String uri,
                            final int fileSizeOctet,
                            final int reportInterval) {
        startUpload(hostname, SpeedTestConst.HTTP_DEFAULT_PORT, uri, fileSizeOctet, reportInterval);
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
        mTask.setReportInterval(true);
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

        mTask.writeUpload(hostname, port, uri, fileSizeOctet);
    }

    /**
     * Start repeat download task.
     *
     * @param hostname           server mHostname
     * @param uri                uri to fetch to download file
     * @param repeatWindow       time window for the repeated download in milliseconds
     * @param reportPeriodMillis time interval between each report in milliseconds
     * @param repeatListener     listener for download repeat task completion & reports
     */
    public void startDownloadRepeat(final String hostname,
                                    final String uri,
                                    final int repeatWindow,
                                    final int reportPeriodMillis,
                                    final IRepeatListener repeatListener) {
        startDownloadRepeat(hostname, SpeedTestConst.HTTP_DEFAULT_PORT, uri, repeatWindow,
                reportPeriodMillis, repeatListener);
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
     * @param uri                uri to fetch to download file
     * @param repeatWindow       time window for the repeated upload in milliseconds
     * @param reportPeriodMillis time interval between each report in milliseconds
     * @param fileSizeOctet      file size in octet
     * @param repeatListener     listener for upload repeat task completion & reports
     */
    public void startUploadRepeat(final String hostname,
                                  final String uri,
                                  final int repeatWindow,
                                  final int reportPeriodMillis,
                                  final int fileSizeOctet,
                                  final IRepeatListener repeatListener) {

        startUploadRepeat(hostname,
                SpeedTestConst.HTTP_DEFAULT_PORT,
                uri,
                repeatWindow,
                reportPeriodMillis,
                fileSizeOctet,
                repeatListener);
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
     * close mSocket + shutdown thread pool.
     */
    @Override
    public void forceStopTask() {
        mTask.forceStopTask();
        mTask.closeSocket();
        shutdownAndWait();
    }

    /**
     * get a temporary download report at this moment.
     *
     * @return speed test download report
     */
    @Override
    public SpeedTestReport getLiveDownloadReport() {
        return mTask.getReport(SpeedTestMode.DOWNLOAD);
    }

    /**
     * get a temporary upload report at this moment.
     *
     * @return speed test upload report
     */
    @Override
    public SpeedTestReport getLiveUploadReport() {
        return mTask.getReport(SpeedTestMode.UPLOAD);
    }

    @Override
    public void closeSocket() {
        mTask.closeSocket();
    }

    /**
     * retrieve current speed test mode.
     *
     * @return speed test mode (UPLOAD/DOWNLOAD/NONE)
     */
    public SpeedTestMode getSpeedTestMode() {
        return mTask.getSpeedTestMode();
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
    @Override
    public int getSocketTimeout() {
        return mSocketTimeout;
    }

    /**
     * retrieve size of each packet sent to upload server.
     *
     * @return size of each packet sent to upload server
     */
    @Override
    public int getUploadChunkSize() {
        return mUploadChunkSize;
    }

    @Override
    public RepeatWrapper getRepeatWrapper() {
        return mRepeatWrapper;
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
    @Override
    public RoundingMode getDefaultRoundingMode() {
        return mRoundingMode;
    }

    /**
     * retrieve scale used for BigDecimal.
     *
     * @return mScale value
     */
    @Override
    public int getDefaultScale() {
        return mScale;
    }

    /**
     * Clear all listeners.
     */
    public void clearListeners() {
        mListenerList.clear();
    }
}
