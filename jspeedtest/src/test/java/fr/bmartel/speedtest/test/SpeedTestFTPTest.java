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

package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.FtpMode;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.UploadStorageType;
import fr.bmartel.speedtest.test.utils.SpeedTestUtils;
import fr.bmartel.speedtest.test.utils.TestCommon;
import net.jodah.concurrentunit.Waiter;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * FTP Speed test.
 *
 * @author Bertrand Martel
 */
public class SpeedTestFTPTest extends AbstractTest {

    /**
     * Waiter used for tests.
     */
    private Waiter mWaiter;

    /**
     * timestamp used to measure time interval.
     */
    private long mTimestamp;

    @Before
    public void setup() {
        mSocket.setFtpMode(FtpMode.PASSIVE);
    }

    @Test
    public void uploadTest() throws TimeoutException {
        uploadFTP(false);
        uploadFTP(true);
        mSocket.setFtpMode(FtpMode.ACTIVE);
        uploadFTP(true);
    }

    private void downloadFtp() throws TimeoutException {
        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = 1048576;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(final SpeedTestReport report) {
                SpeedTestUtils.checkSpeedTestResult(mSocket, waiter2, report.getTotalPacketSize(), packetSizeExpected,
                        report.getTransferRateBit(),
                        report.getTransferRateOctet(), true,
                        false);
                waiter2.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                SpeedTestUtils.testReportNotEmpty(waiter, report, packetSizeExpected, true, false);
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError + " : " + errorMessage);
                waiter2.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError + " : " + errorMessage);
            }
        });

        mSocket.startDownload("ftp://" + TestCommon.FTP_SERVER_HOST + TestCommon.FTP_SERVER_URI);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        waiter2.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mSocket.clearListeners();
    }

    @Test
    public void downloadTest() throws TimeoutException {
        downloadFtp();
        mSocket.setFtpMode(FtpMode.ACTIVE);
        downloadFtp();
    }

    private void uploadFTP(final boolean useFileStorage) throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        if (useFileStorage) {
            mSocket.setUploadStorageType(UploadStorageType.FILE_STORAGE);
        }

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = 1000000;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
                waiter2.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
            }

            @Override
            public void onCompletion(final SpeedTestReport report) {
                SpeedTestUtils.checkSpeedTestResult(mSocket, waiter2, report.getTotalPacketSize(), packetSizeExpected,
                        report.getTransferRateBit(),
                        report.getTransferRateOctet(), false,
                        false);
                waiter2.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                SpeedTestUtils.testReportNotEmpty(waiter, report, packetSizeExpected, true, false);
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }
        });

        mSocket.startUpload("ftp://" + TestCommon.FTP_SERVER_HOST + SpeedTestUtils.getFTPUploadUri(),
                packetSizeExpected);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        waiter2.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mSocket.clearListeners();
    }

    @Test
    public void downloadWithReportIntervalTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int requestInterval = 500;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                checkReportIntervalValue(requestInterval);
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail("unexpected error in onDownloadError : " + speedTestError + " : " + errorMessage);
            }
        });

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startUpload("ftp://" + TestCommon.FTP_SERVER_HOST + SpeedTestUtils.getFTPUploadUri(),
                TestCommon.FILE_SIZE_REGULAR,
                requestInterval);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startDownload("ftp://" + TestCommon.FTP_SERVER_HOST + TestCommon.FTP_SERVER_URI,
                requestInterval);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mSocket.clearListeners();
    }

    /**
     * Compare report interval value measured to actual one.
     *
     * @param requestInterval
     */
    private void checkReportIntervalValue(final int requestInterval) {

        final long currentTimestamp = System.currentTimeMillis();
        if (mTimestamp > 0) {
            final long diff = currentTimestamp - mTimestamp;
            if (diff < (requestInterval - TestCommon.OFFSET_REPORT_INTERVAL) ||
                    diff > (requestInterval + TestCommon.OFFSET_REPORT_INTERVAL)) {
                mWaiter.fail("expected " + requestInterval + " | current val : " +
                        (currentTimestamp - mTimestamp));
            }
        }
        mTimestamp = currentTimestamp;
    }

    @Test
    public void fixDurationTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int packetSizeExpected = 50000000;

        final int duration = 2000;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //called to notify download progress
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail("unexpected error in onDownloadError : " + speedTestError + " : " + errorMessage);
            }
        });
        mWaiter = new Waiter();
        mSocket.startFixedUpload("ftp://" + TestCommon.FTP_SERVER_HOST + SpeedTestUtils.getFTPUploadUri(),
                packetSizeExpected, duration);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mWaiter = new Waiter();

        mSocket.startFixedDownload("ftp://" + TestCommon.FTP_SERVER_HOST + TestCommon.FTP_SERVER_URI_LARGE_FILE,
                duration);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mSocket.clearListeners();
    }


    @Test
    public void fixDurationWithReportIntervalTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int packetSizeExpected = 50000000;

        final int duration = 2000;
        final int requestInterval = 500;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                checkReportIntervalValue(requestInterval);
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail("unexpected error in onDownloadError : " + speedTestError + " : " + errorMessage);
            }
        });

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startFixedUpload("ftp://" + TestCommon.FTP_SERVER_HOST + SpeedTestUtils.getFTPUploadUri(),
                packetSizeExpected, duration, requestInterval);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startFixedDownload("ftp://" + TestCommon.FTP_SERVER_HOST + TestCommon.FTP_SERVER_URI_LARGE_FILE,
                duration, requestInterval);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mSocket.clearListeners();
    }
}
