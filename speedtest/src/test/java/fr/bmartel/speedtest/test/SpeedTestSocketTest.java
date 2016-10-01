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

package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.*;
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
 * SpeedTest socket testing.
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocketTest {

    /**
     * speed test socket object.
     */
    private SpeedTestSocket socket;

    /**
     * unit examples message header.
     */
    private static final String HEADER = TestUtils.generateMessageHeader(SpeedTestReportTest.class);

    /**
     * Waiter for speed test listener callback.
     */
    private static Waiter waiter;

    /**
     * Waiter for speed test listener callback error.
     */
    private static Waiter waiterError;

    /**
     * define if download running or upload.
     */
    private boolean isDownload;

    /**
     * test socket timeout default value.
     */
    @Test
    public void socketTimeoutDefaultTest() {
        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + " socket timeout default value should be " + TestCommon.SOCKET_TO_DEFAULT, socket
                .getSocketTimeout(), TestCommon.SOCKET_TO_DEFAULT);
    }

    /**
     * test socket timeout setter valid test.
     */
    @Test
    public void socketTimeoutSetterValidTest() {
        socket = new SpeedTestSocket();
        socket.setSocketTimeout(TestCommon.SOCKET_TO_VALID);
        Assert.assertEquals(HEADER + "socket timeout are not equals", socket.getSocketTimeout(), TestCommon
                .SOCKET_TO_VALID);
    }

    /**
     * test socket timeout invalid value is detected.
     */
    @Test
    public void socketTimeoutSetterInvalidTest() {
        socket = new SpeedTestSocket();
        Assert.assertNotSame(HEADER + "socket timeout are equals, shouldnt be (-1)", socket.getSocketTimeout(),
                TestCommon.SOCKET_TO_INVALID);
        Assert.assertEquals(HEADER + "socket timeout should be " + TestCommon.SOCKET_TO_DEFAULT, socket.getSocketTimeout
                (), TestCommon.SOCKET_TO_DEFAULT);

    }

    /**
     * test upload chunk size default value.
     */
    @Test
    public void uploadChunkSizeDefaultTest() {
        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + "chunk size should be 65535 for default value", socket.getUploadChunkSize(),
                TestCommon.UPLOAD_CHUNK_SIZE_DEFAULT);
    }

    /**
     * test upload chunk size setter valid value.
     */
    @Test
    public void uploadChunkSizeSetterTest() {
        socket = new SpeedTestSocket();
        socket.setUploadChunkSize(TestCommon.UPLOAD_CHUNK_INVALID);
        Assert.assertEquals(HEADER + "chunk size incorrect value after set", socket.getUploadChunkSize(),
                TestCommon.UPLOAD_CHUNK_INVALID);
    }

    /**
     * test rounding mode setter valid value.
     */
    @Test
    public void defaultRoundingModeSetterTest() {
        socket = new SpeedTestSocket();
        socket.setDefaultRoundingMode(RoundingMode.HALF_UP);
        Assert.assertEquals(HEADER + "rounding mode incorrect value after set", socket.getDefaultRoundingMode(),
                RoundingMode.HALF_UP);
    }

    /**
     * test scale setter valid value.
     */
    @Test
    public void defaultScaleSetterTest() {
        socket = new SpeedTestSocket();
        socket.setDefaultScale(8);
        Assert.assertEquals(HEADER + "scale incorrect value after set", socket.getDefaultScale(),
                8);
    }

    /**
     * test speed test mode value.
     */
    @Test
    public void speedTestModeTest() throws TimeoutException {

        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + "speed test mode value after init", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);
        socket.startDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                .SPEED_TEST_SERVER_URI_DL);
        Assert.assertEquals(HEADER + "speed test mode value after startDownload", socket.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);
        socket.forceStopTask();
        Assert.assertEquals(HEADER + "speed test mode value after forceStopTask", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);

        final Waiter waiter = new Waiter();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail("onDownloadError : shoudlnt be in onDownloadError");
                waiter.resume();
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.resume();
                } else {
                    waiter.fail("onUploadError : " + TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }
        });

        initCountDown();

        socket.startUpload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                        .SPEED_TEST_SERVER_URI_UL,
                TestCommon.FILE_SIZE_MEDIUM);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertEquals(HEADER + "speed test mode value after startUpload", socket.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);
        socket.forceStopTask();

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertEquals(HEADER + "speed test mode value after forceStopTask", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);
    }

    /**
     * test listener object.
     */
    @Test
    public void listenerTest() throws NoSuchFieldException, IllegalAccessException {
        socket = new SpeedTestSocket();

        final List<ISpeedTestListener> listenerList = new ArrayList<ISpeedTestListener>();

        final ISpeedTestListener listener = new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {

            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {

            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {

            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {

            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {

            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {

            }
        };

        SpeedTestUtils.setListenerList(socket, listenerList);

        socket.addSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener add failed", listenerList.size(), 1);

        socket.removeSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener remove failed", listenerList.size(), 0);
    }

    /**
     * test socket valid value.
     */
    @Test
    public void socketTest() throws IllegalAccessException, NoSuchFieldException, TimeoutException {
        socket = new SpeedTestSocket();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiterError.resume();
                } else {
                    waiterError.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                    waiterError.resume();
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiterError.resume();
                } else {
                    waiterError.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                    waiterError.resume();
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }
        });

        initCountDown();

        testSocket(socket);
    }

    /**
     * Test the socket object.
     *
     * @param socket socket object
     */
    private void testSocket(final SpeedTestSocket socket) throws TimeoutException, NoSuchFieldException,
            IllegalAccessException {

        final Field field = socket.getClass().getDeclaredField("socket");
        Assert.assertNotNull(HEADER + "socket is null", field);
        field.setAccessible(true);

        Assert.assertNull(HEADER + "socket value at init", field.get(socket));

        socket.startDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                .SPEED_TEST_SERVER_URI_DL);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        testSocketConnected((Socket) field.get(socket));

        socket.forceStopTask();

        waiterError.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(socket)).isClosed());

        initCountDown();

        socket.startUpload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                        .SPEED_TEST_SERVER_URI_UL,
                TestCommon.FILE_SIZE_MEDIUM);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        testSocketConnected((Socket) field.get(socket));

        socket.forceStopTask();

        waiterError.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        Assert.assertTrue(HEADER + "socket closed after stop upload", ((Socket) field.get(socket)).isClosed());

        initCountDown();

        socket.startDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                .SPEED_TEST_SERVER_URI_DL);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        testSocketConnected((Socket) field.get(socket));

        socket.forceStopTask();

        waiterError.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        socket.closeSocket();
        Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(socket)).isClosed());
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
        waiter = new Waiter();
        waiterError = new Waiter();
    }

    /**
     * test download report empty.
     */
    @Test
    public void downloadReportEmptyTest() {
        socket = new SpeedTestSocket();
        final SpeedTestReport report = socket.getLiveDownloadReport();

        Assert.assertEquals(HEADER + "download report empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);
        SpeedTestUtils.testReportEmpty("download report empty - ", report, false);
    }

    /**
     * test upload report empty.
     */
    @Test
    public void uploadReportEmptyTest() {
        socket = new SpeedTestSocket();
        final SpeedTestReport report = socket.getLiveUploadReport();

        Assert.assertEquals(HEADER + "upload report empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);

        SpeedTestUtils.testReportEmpty("upload report empty - ", report, false);
    }

    /**
     * test download report not empty.
     */
    @Test
    public void downloadReportNotEmptyTest() throws TimeoutException {
        socket = new SpeedTestSocket();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        waiter = new Waiter();

        socket.startDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                .SPEED_TEST_SERVER_URI_DL);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        final SpeedTestReport report = socket.getLiveDownloadReport();

        Assert.assertEquals(HEADER + "download report not empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);

        SpeedTestUtils.testReportNotEmpty(waiter, report, TestCommon.FILE_SIZE_LARGE, false, false);

        socket.forceStopTask();
    }

    /**
     * test upload report empty.
     */
    @Test
    public void uploadReportNotEmptyTest() throws TimeoutException {
        socket = new SpeedTestSocket();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }
        });

        waiter = new Waiter();

        socket.startUpload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                        .SPEED_TEST_SERVER_URI_UL,
                TestCommon.FILE_SIZE_REGULAR);
        waiter.await(4, TimeUnit.SECONDS);

        final SpeedTestReport report = socket.getLiveUploadReport();

        Assert.assertEquals(HEADER + "upload report not empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);

        SpeedTestUtils.testReportNotEmpty(waiter, report, TestCommon.FILE_SIZE_REGULAR, false, false);

        socket.forceStopTask();
    }

    /**
     * Test listener callback for progress & result.
     */
    @Test
    public void progressResultCallbackDownloadTest() throws TimeoutException {
        socket = new SpeedTestSocket();

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = TestCommon.FILE_SIZE_REGULAR;

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
                SpeedTestUtils.checkSpeedTestResult(socket, waiter2, report.getTotalPacketSize(), packetSizeExpected,
                        report.getTransferRateBit(),
                        report.getTransferRateOctet(), true,
                        false);
                waiter2.resume();
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                SpeedTestUtils.testReportNotEmpty(waiter, report, packetSizeExpected, false, false);
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    waiter2.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
                waiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadFinished");
                waiter2.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadFinished");
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadError");
                waiter2.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadError");
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                waiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadProgress");
                waiter2.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadProgress");
            }
        });

        //final int threadCount = Thread.activeCount();

        socket.startDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                .SPEED_TEST_SERVER_URI_DL_1MO);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        //Assert.assertEquals(threadCount + 1, Thread.activeCount());
        waiter2.await(TestCommon.WAITING_TIMEOUT_LONG_OPERATION, TimeUnit.SECONDS);

        socket.forceStopTask();
    }

    /**
     * Test listener callback for progress & result for upload.
     */
    @Test
    public void progressResultCallbackUploadTest() throws TimeoutException {
        socket = new SpeedTestSocket();

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = TestCommon.FILE_SIZE_REGULAR;

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
                waiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onDownloadFinished");
                waiter2.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onDownloadFinished");

            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadProgress");
                waiter2.fail(TestCommon.DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadProgress");
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
                waiter2.fail(TestCommon.DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
                SpeedTestUtils.checkSpeedTestResult(socket, waiter2, report.getTotalPacketSize(), packetSizeExpected,
                        report.getTransferRateBit(),
                        report.getTransferRateOctet(),
                        false, false);
                waiter2.resume();
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UPLOAD_ERROR_STR + speedTestError);
                    waiter2.fail(TestCommon.UPLOAD_ERROR_STR + speedTestError);
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                SpeedTestUtils.testReportNotEmpty(waiter, report, packetSizeExpected, false, false);
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }
        });

        //final int threadCount = Thread.activeCount();

        socket.startUpload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                        .SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        //Assert.assertTrue(Thread.activeCount() == (threadCount + 2) || Thread.activeCount() == (threadCount + 3));
        waiter2.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, TimeUnit.SECONDS);

        socket.forceStopTask();
    }

    @Test
    public void fixDurationTest() throws TimeoutException {

        socket = new SpeedTestSocket();

        final int packetSizeExpected = TestCommon.FILE_SIZE_LARGE;

        final int duration = 2000;

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {

            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (isDownload && speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.resume();
                } else {
                    waiter.fail("unexpected error in onDownloadError : " + speedTestError);
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (!isDownload && speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.resume();
                } else {
                    waiter.fail("unexpected error in onUploadError : " + speedTestError);
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        waiter = new Waiter();
        isDownload = false;
        socket.startUpload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT,
                TestCommon.SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected, duration);

        waiter.await(duration, TimeUnit.MILLISECONDS);

        waiter = new Waiter();
        isDownload = true;
        socket.startDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT,
                TestCommon.SPEED_TEST_SERVER_URI_DL, duration);

        waiter.await(duration, TimeUnit.MILLISECONDS);
    }
}
