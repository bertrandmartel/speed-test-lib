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

import fr.bmartel.speedtest.*;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.SpeedTestMode;
import fr.bmartel.speedtest.test.utils.SpeedTestUtils;
import fr.bmartel.speedtest.test.utils.TestCommon;
import fr.bmartel.speedtest.test.utils.TestUtils;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.RoundingMode;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * SpeedTest mSocket testing.
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocketTest extends AbstractTest {

    /**
     * unit examples message header.
     */
    private static final String HEADER = TestUtils.generateMessageHeader(SpeedTestReportTest.class);

    /**
     * Waiter for speed test listener callback.
     */
    private static Waiter mWaiter;

    /**
     * Waiter for speed test listener callback error.
     */
    private static Waiter mWaiterError;

    /**
     * timestamp used to measure time interval.
     */
    private long mTimestamp;

    /**
     * test socket timeout default value.
     */
    @Test
    public void socketTimeoutDefaultTest() {
        Assert.assertEquals(HEADER + " socket timeout default value should be " + TestCommon.SOCKET_TO_DEFAULT, mSocket
                .getSocketTimeout(), TestCommon.SOCKET_TO_DEFAULT);
    }

    /**
     * test socket timeout setter valid test.
     */
    @Test
    public void socketTimeoutSetterValidTest() {
        mSocket.setSocketTimeout(TestCommon.SOCKET_TO_VALID);
        Assert.assertEquals(HEADER + "socket timeout are not equals", mSocket.getSocketTimeout(), TestCommon
                .SOCKET_TO_VALID);
    }

    /**
     * test socket timeout invalid value is detected.
     */
    @Test
    public void socketTimeoutSetterInvalidTest() {
        Assert.assertNotSame(HEADER + "socket timeout are equals, shouldnt be (-1)", mSocket.getSocketTimeout(),
                TestCommon.SOCKET_TO_INVALID);
        Assert.assertEquals(HEADER + "socket timeout should be " + TestCommon.SOCKET_TO_DEFAULT, mSocket
                .getSocketTimeout
                        (), TestCommon.SOCKET_TO_DEFAULT);

    }

    /**
     * test upload chunk size default value.
     */
    @Test
    public void uploadChunkSizeDefaultTest() {
        Assert.assertEquals(HEADER + "chunk size should be 65535 for default value", mSocket.getUploadChunkSize(),
                TestCommon.UPLOAD_CHUNK_SIZE_DEFAULT);
    }

    /**
     * test upload chunk size setter valid value.
     */
    @Test
    public void uploadChunkSizeSetterTest() {
        mSocket.setUploadChunkSize(TestCommon.UPLOAD_CHUNK_INVALID);
        Assert.assertEquals(HEADER + "chunk size incorrect value after set", mSocket.getUploadChunkSize(),
                TestCommon.UPLOAD_CHUNK_INVALID);
    }

    /**
     * test rounding mode setter valid value.
     */
    @Test
    public void defaultRoundingModeSetterTest() {
        mSocket.setDefaultRoundingMode(RoundingMode.HALF_UP);
        Assert.assertEquals(HEADER + "rounding mode incorrect value after set", mSocket.getDefaultRoundingMode(),
                RoundingMode.HALF_UP);
    }

    /**
     * test scale setter valid value.
     */
    @Test
    public void defaultScaleSetterTest() {
        mSocket.setDefaultScale(8);
        Assert.assertEquals(HEADER + "scale incorrect value after set", mSocket.getDefaultScale(),
                8);
    }

    /**
     * test speed test mode value.
     */
    @Test
    public void speedTestModeTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);
        Assert.assertEquals(HEADER + "speed test mode value after init", mSocket.getSpeedTestMode(),
                SpeedTestMode.NONE);

        mWaiter = new Waiter();

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail("onDownloadError : shoudlnt be in onDownloadError");
                mWaiter.resume();
            }
        });

        mSocket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                TestCommon.SPEED_TEST_SERVER_URI_DL);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertEquals(HEADER + "speed test mode value after startDownload", mSocket.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);

        mWaiter = new Waiter();

        mSocket.forceStopTask();

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        mWaiter = new Waiter();

        mSocket.startUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                        TestCommon.SPEED_TEST_SERVER_URI_UL,
                TestCommon.FILE_SIZE_MEDIUM);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertEquals(HEADER + "speed test mode value after startUpload", mSocket.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);

        mWaiter = new Waiter();

        mSocket.forceStopTask();

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        mSocket.clearListeners();
    }

    /**
     * test listener object.
     */
    @Test
    public void listenerTest() throws NoSuchFieldException, IllegalAccessException {

        final List<ISpeedTestListener> listenerList = new ArrayList<ISpeedTestListener>();

        final ISpeedTestListener listener = new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //called to notify progress
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                //called when download error occur
            }
        };

        SpeedTestUtils.setListenerList(mSocket, listenerList);

        mSocket.addSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener add failed", listenerList.size(), 1);

        mSocket.removeSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener remove failed", listenerList.size(), 0);

        mSocket.addSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener add failed", listenerList.size(), 1);

        mSocket.clearListeners();

        Assert.assertEquals(HEADER + "listener remove failed", listenerList.size(), 0);
    }

    /**
     * test mSocket valid value.
     */
    @Test
    public void socketTest() throws IllegalAccessException, NoSuchFieldException, TimeoutException {
        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiterError.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                mWaiterError.resume();
            }
        });

        initCountDown();

        testSocket(mSocket);

        mSocket.clearListeners();
    }

    /**
     * Test the mSocket object.
     *
     * @param socket mSocket object
     */
    private void testSocket(final SpeedTestSocket socket) throws TimeoutException, NoSuchFieldException,
            IllegalAccessException {

        final Field fieldTask = socket.getClass().getDeclaredField("mTask");
        Assert.assertNotNull(HEADER + "socket is null", fieldTask);
        fieldTask.setAccessible(true);

        final SpeedTestTask task = (SpeedTestTask) fieldTask.get(socket);
        final Field field = task.getClass().getDeclaredField("mSocket");
        Assert.assertNotNull(HEADER + "socket is null", field);
        field.setAccessible(true);

        Assert.assertNull(HEADER + "socket value at init", field.get(task));

        socket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                TestCommon.SPEED_TEST_SERVER_URI_DL);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        testSocketConnected((Socket) field.get(task));

        socket.forceStopTask();

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(task)).isClosed());

        initCountDown();

        socket.startUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + TestCommon.SPEED_TEST_SERVER_URI_UL,
                TestCommon.FILE_SIZE_MEDIUM);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        testSocketConnected((Socket) field.get(task));

        socket.forceStopTask();

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertTrue(HEADER + "socket closed after stop upload", ((Socket) field.get(task)).isClosed());

        initCountDown();

        socket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + TestCommon.SPEED_TEST_SERVER_URI_DL);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        testSocketConnected((Socket) field.get(task));

        socket.forceStopTask();

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        socket.closeSocket();
        Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(task)).isClosed());
    }

    /**
     * test socket closed.
     */
    private void testSocketConnected(final Socket socket) {
        Assert.assertNotNull(HEADER + "socket value after download", socket);
        Assert.assertTrue(HEADER + "socket connected after download", socket.isConnected());
        Assert.assertFalse(HEADER + "socket closed after download", socket.isClosed());
    }

    /**
     * initialize both lock & lock error CountDown.
     */
    private void initCountDown() {
        mWaiter = new Waiter();
        mWaiterError = new Waiter();
    }

    /**
     * test upload report empty.
     */
    @Test
    public void reportEmptyTest() {
        final SpeedTestReport report = mSocket.getLiveReport();
        SpeedTestUtils.testReportEmpty("upload report empty - ", report, false);
    }

    /**
     * test download report not empty.
     */
    @Test
    public void downloadReportNotEmptyTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                mWaiter.resume();
            }
        });

        mWaiter = new Waiter();

        mSocket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                TestCommon.SPEED_TEST_SERVER_URI_DL);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        final SpeedTestReport report = mSocket.getLiveReport();

        Assert.assertEquals(HEADER + "download report not empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);

        SpeedTestUtils.testReportNotEmpty(mWaiter, report, TestCommon.FILE_SIZE_LARGE, false, false);

        mSocket.forceStopTask();

        mSocket.clearListeners();
    }

    /**
     * test upload report empty.
     */
    @Test
    public void uploadReportNotEmptyTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                mWaiter.resume();
            }
        });

        mWaiter = new Waiter();

        mSocket.startUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                        TestCommon.SPEED_TEST_SERVER_URI_UL,
                TestCommon.FILE_SIZE_REGULAR);
        mWaiter.await(4, TimeUnit.SECONDS);

        final SpeedTestReport report = mSocket.getLiveReport();

        Assert.assertEquals(HEADER + "upload report not empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);

        SpeedTestUtils.testReportNotEmpty(mWaiter, report, TestCommon.FILE_SIZE_REGULAR, false, false);

        mSocket.forceStopTask();

        mSocket.clearListeners();
    }

    /**
     * Test listener callback for progress & result.
     */
    @Test
    public void progressResultCallbackDownloadTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = TestCommon.FILE_SIZE_REGULAR;

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
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                waiter2.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
            }
        });

        //final int threadCount = Thread.activeCount();

        mSocket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                TestCommon.SPEED_TEST_SERVER_URI_DL_1MO);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        //Assert.assertEquals(threadCount + 1, Thread.activeCount());
        waiter2.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mSocket.forceStopTask();

        mSocket.clearListeners();
    }

    /**
     * Test listener callback for progress & result for upload.
     */
    @Test
    public void progressResultCallbackUploadTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = TestCommon.FILE_SIZE_REGULAR;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
                waiter2.fail(TestCommon.DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
            }

            @Override
            public void onCompletion(final SpeedTestReport report) {
                SpeedTestUtils.checkSpeedTestResult(mSocket, waiter2, report.getTotalPacketSize(), packetSizeExpected,
                        report.getTransferRateBit(),
                        report.getTransferRateOctet(),
                        false, false);
                waiter2.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //SpeedTestUtils.testReportNotEmpty(waiter, report, packetSizeExpected, false, false);
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }
        });

        //final int threadCount = Thread.activeCount();

        mSocket.startUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                        TestCommon.SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        //Assert.assertTrue(Thread.activeCount() == (threadCount + 2) || Thread.activeCount() == (threadCount + 3));
        waiter2.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mSocket.forceStopTask();

        mSocket.clearListeners();
    }

    @Test
    public void fixDurationTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int packetSizeExpected = TestCommon.FILE_SIZE_LARGE;

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
                mWaiter.fail("unexpected error in onDownloadError : " + speedTestError);
            }
        });

        mWaiter = new Waiter();
        mSocket.startFixedUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
                        .SPEED_TEST_SERVER_PORT + TestCommon.SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected, duration);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mWaiter = new Waiter();
        mSocket.startFixedDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
                .SPEED_TEST_SERVER_PORT + TestCommon.SPEED_TEST_SERVER_URI_DL, duration);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mSocket.clearListeners();
    }

    /*
    @Test
    public void fixDurationWithReportIntervalTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int packetSizeExpected = TestCommon.FILE_SIZE_LARGE;

        final int duration = 2000;
        final int requestInterval = 500;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                checkReportIntervalValue(requestInterval);
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail("unexpected error in onDownloadError : " + speedTestError);
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail("unexpected error in onUploadError : " + speedTestError);
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                checkReportIntervalValue(requestInterval);
            }

            @Override
            public void onInterruption() {
                mWaiter.resume();
            }
        });

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startFixedUpload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT,
                TestCommon.SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected, duration, requestInterval);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startFixedDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT,
                TestCommon.SPEED_TEST_SERVER_URI_DL, duration, requestInterval);

        mWaiter.await(duration + TestCommon.FIXED_DURATION_OFFSET, TimeUnit.MILLISECONDS);

        mSocket.clearListeners();
    }
    */

    @Test
    public void downloadWithReportIntervalTest() throws TimeoutException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int packetSizeExpected = TestCommon.FILE_SIZE_REGULAR;

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
                mWaiter.fail("unexpected error in onDownloadError : " + speedTestError);
            }
        });

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                        TestCommon.SPEED_TEST_SERVER_PORT +
                        TestCommon.SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected, requestInterval);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
                TestCommon.SPEED_TEST_SERVER_URI_DL_1MO, requestInterval);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                        TestCommon.SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected, requestInterval);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        mWaiter = new Waiter();
        mTimestamp = 0;
        mSocket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST +
                TestCommon.SPEED_TEST_SERVER_URI_DL_1MO, requestInterval);

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
}
