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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Repeat tasks speed test wrapper : this is used to repeat download/upload requests during a fix duration.
 *
 * @author Bertrand Martel
 */
public class RepeatWrapper {

    /**
     * transfer rate list.
     */
    private List<BigDecimal> repeatTransferRateList = new ArrayList<>();

    /**
     * define if download repeat task is finished.
     */
    private boolean repeatFinished;

    /**
     * number of packet downloaded for download/upload repeat task.
     */
    private long repeatTempPckSize;

    /**
     * define if upload should be repeated.
     */
    private boolean repeatUpload;

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
     * define if download should be repeated.
     */
    private boolean repeatDownload;

    /**
     * number of packet pending for download repeat task.
     */
    private BigDecimal repeatPacketSize = BigDecimal.ZERO;

    /**
     * define if the first download repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private boolean firstDownloadRepeat;

    /**
     * define if the first upload repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private boolean firstUploadRepeat;

    /**
     * speed test socket interface.
     */
    private final ISpeedTestSocket speedTestSocket;

    /**
     * Build Speed test repeat.
     *
     * @param socket speed test socket
     */
    public RepeatWrapper(final ISpeedTestSocket socket) {
        speedTestSocket = socket;
    }

    /**
     * Build repeat download/upload report based on stats on all packets downlaoded until now.
     *
     * @param scale
     * @param roundingMode
     * @param speedTestMode     speed test mode
     * @param reportTime        time of current download
     * @param transferRateOctet transfer rate in octet/s
     * @return speed test report object
     */
    public SpeedTestReport getRepeatReport(final int scale,
                                           final RoundingMode roundingMode,
                                           final SpeedTestMode speedTestMode,
                                           final long reportTime,
                                           final BigDecimal transferRateOctet) {

        BigDecimal progressPercent = BigDecimal.ZERO;
        long temporaryPacketSize = 0;
        BigDecimal downloadRepeatRateOctet = transferRateOctet;
        long downloadRepeatReportTime = reportTime;

        if (startDateRepeat != 0) {
            if (!repeatFinished) {
                progressPercent = new BigDecimal(System.currentTimeMillis() - startDateRepeat).multiply
                        (SpeedTestConst.PERCENT_MAX)
                        .divide(new BigDecimal(repeatWindows), scale, roundingMode);
            } else {
                progressPercent = SpeedTestConst.PERCENT_MAX;
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

        final BigDecimal transferRateBit = downloadRepeatRateOctet.multiply(SpeedTestConst.BIT_MULTIPLIER);

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
     * start download for download repeat.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    private void startDownloadRepeat(final String hostname, final int port, final String uri) {
        repeatDownload = true;
        speedTestSocket.startDownload(hostname, port, uri);
    }

    /**
     * start upload for download repeat.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to upload file
     */
    private void startUploadRepeat(final String hostname, final int port, final String uri, final int fileSizeOctet) {
        speedTestSocket.startUpload(hostname, port, uri, fileSizeOctet);
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

        initRepeat(true);

        final Timer timer = new Timer();

        final ISpeedTestListener speedTestListener = new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
                repeatTransferRateList.add(report.getTransferRateOctet());
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
            public void onUploadFinished(final SpeedTestReport report) {
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

        speedTestSocket.addSpeedTestListener(speedTestListener);

        repeatWindows = repeatWindow;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                speedTestSocket.removeSpeedTestListener(speedTestListener);
                speedTestSocket.forceStopTask();
                timer.cancel();
                timer.purge();
                repeatFinished = true;
                if (repeatListener != null) {
                    repeatListener.onFinish(speedTestSocket.getLiveDownloadReport());
                }
            }
        }, repeatWindow);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (repeatListener != null) {
                    repeatListener.onReport(speedTestSocket.getLiveDownloadReport());
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

        initRepeat(false);

        final Timer timer = new Timer();

        final ISpeedTestListener speedTestListener = new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
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
            public void onUploadFinished(final SpeedTestReport report) {
                repeatTransferRateList.add(report.getTransferRateOctet());
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

        speedTestSocket.addSpeedTestListener(speedTestListener);

        repeatWindows = repeatWindow;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                speedTestSocket.removeSpeedTestListener(speedTestListener);
                speedTestSocket.forceStopTask();
                timer.cancel();
                timer.purge();
                repeatFinished = true;
                if (repeatListener != null) {
                    repeatListener.onFinish(speedTestSocket.getLiveUploadReport());
                }
            }
        }, repeatWindow);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (repeatListener != null) {
                    repeatListener.onReport(speedTestSocket.getLiveUploadReport());
                }
            }
        }, reportPeriodMillis, reportPeriodMillis);

        startUploadRepeat(hostname, port, uri, fileSizeOctet);
    }

    /**
     * Initialize download/upload repeat task variables for report + state.
     *
     * @param isDownload define if initialization is for download or upload
     */
    private void initRepeat(final boolean isDownload) {
        repeatDownload = isDownload;
        firstDownloadRepeat = isDownload;
        repeatUpload = !isDownload;
        firstUploadRepeat = !isDownload;
        initRepeatVars();
    }

    /**
     * Initialize upload/download repeat task variables for report + state.
     */
    private void initRepeatVars() {
        repeatRequestNum = 0;
        repeatPacketSize = BigDecimal.ZERO;
        repeatTempPckSize = 0;
        repeatFinished = false;
        startDateRepeat = 0;
        repeatTransferRateList = new ArrayList<>();
    }

    /**
     * clear completly download/upload repeat task.
     *
     * @param listener speed test listener
     * @param timer    finished task timer
     */
    private void clearRepeatTask(final ISpeedTestListener listener, final Timer timer) {

        speedTestSocket.removeSpeedTestListener(listener);
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        repeatFinished = true;
        speedTestSocket.closeSocket();
        speedTestSocket.shutdownAndWait();
    }

    public boolean isFirstDownload() {
        return firstDownloadRepeat && repeatDownload;
    }

    public boolean isFirstUpload() {
        return firstUploadRepeat && repeatUpload;
    }

    public void setFirstDownloadRepeat(final boolean state) {
        firstDownloadRepeat = state;
    }

    public void setStartDate(final long timeStart) {
        startDateRepeat = timeStart;
    }

    public boolean isRepeatDownload() {
        return repeatDownload;
    }

    public void updatePacketSize(final BigDecimal packetSize) {
        repeatPacketSize = repeatPacketSize.add(packetSize);
    }

    public void updateTempPacketSize(final int read) {
        repeatTempPckSize += read;
    }

    public boolean isRepeatUpload() {
        return repeatUpload;
    }

    public boolean isRepeat() {
        return repeatDownload || repeatUpload;
    }

    public void setFirstUploadRepeat(final boolean state) {
        firstUploadRepeat = state;
    }
}
