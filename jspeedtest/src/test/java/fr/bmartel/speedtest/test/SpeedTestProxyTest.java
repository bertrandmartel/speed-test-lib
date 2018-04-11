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
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.UploadStorageType;
import fr.bmartel.speedtest.test.server.ServerRetryTest;
import fr.bmartel.speedtest.test.utils.TestCommon;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * download / upload functional test mocking a speed test mServer.
 *
 * @author Bertrand Martel
 */
public class SpeedTestProxyTest extends ServerRetryTest {

    /**
     * default timeout waiting time for long operation such as DL / UL
     */
    private final static int WAITING_TIMEOUT_LONG_OPERATION = 10;

    /**
     * transfer rate value received in ops.
     */
    private BigDecimal mExpectedTransferRateOps;

    /**
     * transfer rate value received in bps.
     */
    private BigDecimal mExpectedTransferRateBps;

    /**
     * Common waiter for functional test.
     */
    private Waiter mWaiter;

    @Test
    @Ignore
    public void downloadProxy1MTest() throws TimeoutException {
        initTask(true);
        testDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + TestCommon.SPEED_TEST_SERVER_URI_DL_1MO);
        stopTask();
    }

    @Test
    @Ignore
    public void downloadProxyRedirectTest() throws TimeoutException {
        initTask(true);
        testDownload(TestCommon.SPEED_TEST_REDIRECT_SERVER);
        stopTask();
    }

    @Test
    @Ignore
    public void uploadProxy1MTest() throws TimeoutException {
        initTask(false);
        testUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + TestCommon.SPEED_TEST_SERVER_URI_UL, 1000000, true);
        stopTask();
    }

    private void initTask(final boolean download) throws TimeoutException {
        mSocket = new SpeedTestSocket();

        mSocket.setProxyServer(TestCommon.SPEED_TEST_PROXY_SERVER);

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mWaiter = new Waiter();

        mSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //called to notify download progress
            }

            @Override
            public void onCompletion(final SpeedTestReport report) {
                mExpectedTransferRateOps = report.getTransferRateOctet();
                mExpectedTransferRateBps = report.getTransferRateBit();
                mWaiter.resume();
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                if (mExpectedError != null && speedTestError == mExpectedError) {
                    mWaiter.resume();
                } else {
                    if (download) {
                        mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    } else {
                        mWaiter.fail(TestCommon.UPLOAD_ERROR_STR + speedTestError);
                    }
                    mWaiter.resume();
                }
            }
        });
    }

    private void stopTask() {
        stopServer();
        mSocket.setProxyServer(null);
        mSocket.clearListeners();
    }


    /**
     * Test download with given URI.
     *
     * @param uri
     */
    private void testDownload(final String uri) throws TimeoutException {

        mWaiter = new Waiter();

        mSocket.startDownload(uri);

        mWaiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        testTransferRate();

        mSocket.forceStopTask();
    }

    /**
     * Compare transfer rate calculated to expected value.
     */
    private void testTransferRate() {
        Assert.assertNotNull(mExpectedTransferRateOps);
        Assert.assertNotNull(mExpectedTransferRateBps);

        Assert.assertTrue(mExpectedTransferRateBps.longValue() > 0);
        Assert.assertTrue(mExpectedTransferRateOps.longValue() > 0);
    }

    /**
     * Test upload with given packet size.
     *
     * @param size
     */
    private void testUpload(final String url, final int size, final boolean useFileStorage) throws TimeoutException {

        mWaiter = new Waiter();

        if (useFileStorage) {
            mSocket.setUploadStorageType(UploadStorageType.FILE_STORAGE);
        }

        mSocket.startUpload(url, size);

        mWaiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        testTransferRate();

        mSocket.forceStopTask();
    }
}