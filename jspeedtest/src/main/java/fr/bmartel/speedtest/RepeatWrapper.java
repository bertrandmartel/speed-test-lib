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

import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.inter.ISpeedTestSocket;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.SpeedTestMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Repeat tasks speed test wrapper : this is used to repeat download/upload requests during a fix duration.
 *
 * @author Bertrand Martel
 */
public class RepeatWrapper {

    /**
     * transfer rate list.
     */
    private List<BigDecimal> mRepeatTransferRateList = Collections.synchronizedList(new ArrayList<BigDecimal>());

    /**
     * define if download repeat task is finished.
     */
    private boolean mRepeatFinished;

    /**
     * number of packet downloaded for download/upload repeat task.
     */
    private long mRepeatTempPckSize;

    /**
     * define if upload should be repeated.
     */
    private boolean mRepeatUpload;

    /**
     * start time for download repeat task.
     */
    private long mStartDateRepeat;

    /**
     * time window for download repeat task.
     */
    private int mRepeatWindows;

    /**
     * current number of request for download repeat task.
     */
    private int mRepeatRequestNum;

    /**
     * define if download should be repeated.
     */
    private boolean mRepeatDownload;

    /**
     * number of packet pending for download repeat task.
     */
    private BigDecimal mRepeatPacketSize = BigDecimal.ZERO;

    /**
     * define if the first download repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private boolean mFirstDownloadRepeat;

    /**
     * define if the first upload repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private boolean mFirstUploadRepeat;

    /**
     * speed test socket interface.
     */
    private final ISpeedTestSocket mSpeedTestSocket;

    /**
     * Timer used in repeat methods.
     */
    private Timer mTimer;

    /**
     * Build Speed test repeat.
     *
     * @param socket speed test socket
     */
    public RepeatWrapper(final ISpeedTestSocket socket) {
        mSpeedTestSocket = socket;
    }

    /**
     * Build repeat download/upload report based on stats on all packets downlaoded until now.
     *
     * @param scale             scale value for bigdecimal
     * @param roundingMode      rounding mode used for bigdecimal
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

        if (mStartDateRepeat != 0) {
            if (!mRepeatFinished) {
                long test = System.nanoTime() - mStartDateRepeat;
                progressPercent = new BigDecimal(test).multiply
                        (SpeedTestConst.PERCENT_MAX)
                        .divide(new BigDecimal(mRepeatWindows).multiply(new BigDecimal(1000000)), scale, roundingMode);
            } else {
                progressPercent = SpeedTestConst.PERCENT_MAX;
            }
        } else {
            //download has not started yet
            progressPercent = BigDecimal.ZERO;
        }

        BigDecimal rates = BigDecimal.ZERO;
        for (final BigDecimal rate :
                mRepeatTransferRateList) {
            rates = rates.add(rate);
        }

        if (!mRepeatTransferRateList.isEmpty()) {
            downloadRepeatRateOctet = rates.add(downloadRepeatRateOctet).divide(new BigDecimal(mRepeatTransferRateList
                    .size()).add
                    (new BigDecimal(mRepeatTempPckSize).divide(mRepeatPacketSize, scale, roundingMode)
                    ), scale, roundingMode);
        }

        final BigDecimal transferRateBit = downloadRepeatRateOctet.multiply(SpeedTestConst.BIT_MULTIPLIER);

        if (!mRepeatFinished) {
            temporaryPacketSize = mRepeatTempPckSize;
        } else {
            temporaryPacketSize = mRepeatTempPckSize;
            downloadRepeatReportTime = new BigDecimal(mStartDateRepeat).add(new BigDecimal(mRepeatWindows).multiply(new BigDecimal(1000000))).longValue();
        }

        return new SpeedTestReport(speedTestMode,
                progressPercent.floatValue(),
                mStartDateRepeat,
                downloadRepeatReportTime,
                temporaryPacketSize,
                mRepeatPacketSize.longValueExact(),
                downloadRepeatRateOctet,
                transferRateBit,
                mRepeatRequestNum);
    }

    /**
     * Start download for download repeat.
     *
     * @param uri uri to fetch to download file
     */
    private void startDownloadRepeat(final String uri) {
        mRepeatDownload = true;
        mSpeedTestSocket.startDownload(uri);
    }

    /**
     * Start upload for upload repeat.
     *
     * @param uri uri to fetch to upload file
     * @fileSizeOctet file size in octet
     */
    private void startUploadRepeat(final String uri, final int fileSizeOctet) {
        mSpeedTestSocket.startUpload(uri, fileSizeOctet);
    }

    /**
     * Start repeat download task.
     *
     * @param uri                uri to fetch to download file
     * @param repeatWindow       time window for the repeated download in milliseconds
     * @param reportPeriodMillis time interval between each report in milliseconds
     * @param repeatListener     listener for download repeat task completion & reports
     */
    public void startDownloadRepeat(
            final String uri,
            final int repeatWindow,
            final int reportPeriodMillis,
            final IRepeatListener repeatListener) {

        initRepeat(true);

        mTimer = new Timer();

        final ISpeedTestListener speedTestListener = new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                mRepeatTransferRateList.add(report.getTransferRateOctet());
                startDownloadRepeat(uri);
                mRepeatRequestNum++;
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //nothing to do here for download repeat task listener
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                clearRepeatTask(this);
            }
        };

        mSpeedTestSocket.addSpeedTestListener(speedTestListener);

        mRepeatWindows = repeatWindow;

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mSpeedTestSocket.removeSpeedTestListener(speedTestListener);
                mSpeedTestSocket.forceStopTask();
                cleanTimer();
                mRepeatFinished = true;
                if (repeatListener != null) {
                    repeatListener.onCompletion(mSpeedTestSocket.getLiveReport());
                }
            }
        }, repeatWindow);

        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (repeatListener != null) {
                    repeatListener.onReport(mSpeedTestSocket.getLiveReport());
                }
            }
        }, reportPeriodMillis, reportPeriodMillis);
        startDownloadRepeat(uri);
    }

    /**
     * Start repeat upload task.
     *
     * @param uri                uri to fetch to download file
     * @param repeatWindow       time window for the repeated upload in milliseconds
     * @param reportPeriodMillis time interval between each report in milliseconds
     * @param fileSizeOctet      file size in octet
     * @param repeatListener     listener for upload repeat task completion & reports
     */
    public void startUploadRepeat(
            final String uri,
            final int repeatWindow,
            final int reportPeriodMillis,
            final int fileSizeOctet,
            final IRepeatListener repeatListener) {

        initRepeat(false);

        mTimer = new Timer();

        final ISpeedTestListener speedTestListener = new ISpeedTestListener() {

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //nothing to do here for upload repeat task listener
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                clearRepeatTask(this);
            }

            @Override
            public void onCompletion(final SpeedTestReport report) {
                mRepeatTransferRateList.add(report.getTransferRateOctet());
                startUploadRepeat(uri, fileSizeOctet);
                mRepeatRequestNum++;
            }
        };

        mSpeedTestSocket.addSpeedTestListener(speedTestListener);

        mRepeatWindows = repeatWindow;

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mSpeedTestSocket.removeSpeedTestListener(speedTestListener);
                mSpeedTestSocket.forceStopTask();
                cleanTimer();
                mRepeatFinished = true;
                if (repeatListener != null) {
                    repeatListener.onCompletion(mSpeedTestSocket.getLiveReport());
                }
            }
        }, repeatWindow);

        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (repeatListener != null) {
                    repeatListener.onReport(mSpeedTestSocket.getLiveReport());
                }
            }
        }, reportPeriodMillis, reportPeriodMillis);

        startUploadRepeat(uri, fileSizeOctet);
    }

    public void cleanTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
    }

    /**
     * Initialize download/upload repeat task variables for report + state.
     *
     * @param isDownload define if initialization is for download or upload
     */
    private void initRepeat(final boolean isDownload) {
        mRepeatDownload = isDownload;
        mFirstDownloadRepeat = isDownload;
        mRepeatUpload = !isDownload;
        mFirstUploadRepeat = !isDownload;
        initRepeatVars();
    }

    /**
     * Initialize upload/download repeat task variables for report + state.
     */
    private void initRepeatVars() {
        mRepeatRequestNum = 0;
        mRepeatPacketSize = BigDecimal.ZERO;
        mRepeatTempPckSize = 0;
        mRepeatFinished = false;
        mStartDateRepeat = 0;
        mRepeatTransferRateList = new ArrayList<>();
    }

    /**
     * Clear completly download/upload repeat task.
     *
     * @param listener speed test listener
     */
    private void clearRepeatTask(final ISpeedTestListener listener) {

        mSpeedTestSocket.removeSpeedTestListener(listener);
        cleanTimer();
        mRepeatFinished = true;
        mSpeedTestSocket.closeSocket();
        mSpeedTestSocket.shutdownAndWait();
    }

    /**
     * Check if this is the first packet to be downloaded for repeat operation.
     *
     * @return state for first download
     */
    public boolean isFirstDownload() {
        return mFirstDownloadRepeat && mRepeatDownload;
    }

    /**
     * Check if this is the first packet to be uploaded for repeat operation.
     *
     * @return state of first upload
     */
    public boolean isFirstUpload() {
        return mFirstUploadRepeat && mRepeatUpload;
    }

    /**
     * Set the first downloaded packet status.
     *
     * @param state download repeat status
     */
    public void setFirstDownloadRepeat(final boolean state) {
        mFirstDownloadRepeat = state;
    }

    /**
     * Set the start date for repeat task.
     *
     * @param timeStart start date in millis
     */
    public void setStartDate(final long timeStart) {
        mStartDateRepeat = timeStart;
    }

    /**
     * check if download repeat task is running.
     *
     * @return repeat download status
     */
    public boolean isRepeatDownload() {
        return mRepeatDownload;
    }

    /**
     * Update total packet size to be downloaded/uploaded.
     *
     * @param packetSize packet size in octet
     */
    public void updatePacketSize(final BigDecimal packetSize) {
        mRepeatPacketSize = mRepeatPacketSize.add(packetSize);
    }

    /**
     * Update temporary packet size currently downloaded/uploaded.
     *
     * @param read packet size in octet
     */
    public void updateTempPacketSize(final int read) {
        mRepeatTempPckSize += read;
    }

    /**
     * Check if upload repeat task is running.
     *
     * @return repeat upload status
     */
    public boolean isRepeatUpload() {
        return mRepeatUpload;
    }

    /**
     * Check if repeat task is running.
     *
     * @return repeat status
     */
    public boolean isRepeat() {
        return mRepeatDownload || mRepeatUpload;
    }

    /**
     * Set the first uploaded packet status.
     *
     * @param state first upload repeat status
     */
    public void setFirstUploadRepeat(final boolean state) {
        mFirstUploadRepeat = state;
    }
}
