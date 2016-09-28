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

package fr.bmartel.speedtest.examples;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.constants.HttpHeader;
import fr.bmartel.protocol.http.states.HttpStates;
import fr.bmartel.speedtest.*;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Speed examples socket examples.
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
     * value for valid socket timeout.
     */
    private static final int SOCKET_TO_VALID = 10000;

    /**
     * default value for socket timeout.
     */
    private static final int SOCKET_TO_DEFAULT = 10000;

    /**
     * value for invalid socket timeout.
     */
    private static final int SOCKET_TO_INVALID = -1;

    /**
     * default value of upload chunk size.
     */
    private static final int UPLOAD_CHUNK_SIZE_DEFAULT = 65535;

    /**
     * invalid value for upload chunk packet size.
     */
    private static final int UPLOAD_CHUNK_INVALID = 30000;

    /**
     * speed examples server host name.
     */
    private final static String SPEED_TEST_SERVER_HOST = "2.testdebit.info";

    /**
     * spedd examples server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL = "/fichiers/100Mo.dat";

    /**
     * spedd examples 1Mo server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL_1MO = "/fichiers/1Mo.dat";

    /**
     * speed examples server port.
     */
    private final static int SPEED_TEST_SERVER_PORT = 80;

    /**
     * spedd examples server uri.
     */
    private static final String SPEED_TEST_SERVER_URI_UL = "/";

    /**
     * Waiter for speed test listener callback.
     */
    private static Waiter waiter;

    /**
     * Waiter for speed test listener callback error.
     */
    private static Waiter waiterError;

    /**
     * message for unexpected error.
     */
    private final static String UNEXPECTED_ERROR_STR = "unexpected error : ";

    /**
     * message for download error.
     */
    private final static String DOWNLOAD_ERROR_STR = "download error : ";

    /**
     * message for upload error.
     */
    private final static String UPLOAD_ERROR_STR = "upload error : ";

    /**
     * error message for testing error callback.
     */
    private static String errorMessage = "this is an error message";

    /**
     * error suffix message for force close.
     */
    private static String errorMessageSuffix = " caused by socket force close";

    /**
     * download/upload mode for testing error callback.
     */
    private static boolean isDownload = true;

    /**
     * define if force stop is to be expected in error callback.
     */
    private static boolean isForceStop;

    /**
     * define if error message value should be checked.
     */
    private static boolean noCheckMessage;

    /**
     * default timeout waiting time in seconds.
     */
    private final static int WAITING_TIMEOUT_DEFAULT_SEC = 2;

    /**
     * default timeout waiting time for long operation such as DL / UL
     */
    private final static int WAITING_TIMEOUT_LONG_OPERATION = 10;

    /**
     * file size used in those tests to test a DL/UL.
     */
    private final static int FILE_SIZE_REGULAR = 1000000;

    /**
     * file size medium.
     */
    private final static int FILE_SIZE_MEDIUM = 10000000;

    /**
     * file size used for large operations.
     */
    private final static int FILE_SIZE_LARGE = 100000000;

    /**
     * test socket timeout default value.
     */
    @Test
    public void socketTimeoutDefaultTest() {
        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + " socket timeout default value should be " + SOCKET_TO_DEFAULT, socket
                .getSocketTimeout(), SOCKET_TO_DEFAULT);
    }

    /**
     * test socket timeout setter valid test.
     */
    @Test
    public void socketTimeoutSetterValidTest() {
        socket = new SpeedTestSocket();
        socket.setSocketTimeout(SOCKET_TO_VALID);
        Assert.assertEquals(HEADER + "socket timeout are not equals", socket.getSocketTimeout(), SOCKET_TO_VALID);
    }

    /**
     * test socket timeout invalid value is detected.
     */
    @Test
    public void socketTimeoutSetterInvalidTest() {
        socket = new SpeedTestSocket();
        Assert.assertNotSame(HEADER + "socket timeout are equals, shouldnt be (-1)", socket.getSocketTimeout(),
                SOCKET_TO_INVALID);
        Assert.assertEquals(HEADER + "socket timeout should be " + SOCKET_TO_DEFAULT, socket.getSocketTimeout
                (), SOCKET_TO_DEFAULT);

    }

    /**
     * test upload chunk size default value.
     */
    @Test
    public void uploadChunkSizeDefaultTest() {
        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + "chunk size should be 65535 for default value", socket.getUploadChunkSize(),
                UPLOAD_CHUNK_SIZE_DEFAULT);
    }

    /**
     * test upload chunk size setter valid value.
     */
    @Test
    public void uploadChunkSizeSetterTest() {
        socket = new SpeedTestSocket();
        socket.setUploadChunkSize(UPLOAD_CHUNK_INVALID);
        Assert.assertEquals(HEADER + "chunk size incorrect value after set", socket.getUploadChunkSize(),
                UPLOAD_CHUNK_INVALID);
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
    public void speedTestModeTest() {

        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + "speed test mode value after init", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);
        socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL);
        Assert.assertEquals(HEADER + "speed test mode value after startDownload", socket.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);
        socket.forceStopTask();
        Assert.assertEquals(HEADER + "speed test mode value after forceStopTask", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);

        final Waiter waiter = new Waiter();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
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
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.resume();
                } else {
                    waiter.fail("onUploadError : " + UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }
        });

        initCountDown();

        socket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL,
                FILE_SIZE_MEDIUM);

        try {
            waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }

        Assert.assertEquals(HEADER + "speed test mode value after startUpload", socket.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);
        socket.forceStopTask();

        try {
            waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }

        Assert.assertEquals(HEADER + "speed test mode value after forceStopTask", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);
    }

    /**
     * test listener object.
     */
    @Test
    public void listenerTest() {
        socket = new SpeedTestSocket();

        final List<ISpeedTestListener> listenerList = new ArrayList<ISpeedTestListener>();

        final ISpeedTestListener listener = new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {

            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {

            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {

            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {

            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {

            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {

            }
        };

        try {
            final Field field = socket.getClass().getDeclaredField("listenerList");
            Assert.assertNotNull(HEADER + "listenerList is null", field);
            field.setAccessible(true);
            field.set(socket, listenerList);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

        socket.addSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener add failed", listenerList.size(), 1);

        socket.removeSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener remove failed", listenerList.size(), 0);
    }

    /**
     * test socket valid value.
     */
    @Test
    public void socketTest() {
        socket = new SpeedTestSocket();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
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
                    waiterError.fail(UNEXPECTED_ERROR_STR + speedTestError);
                    waiterError.resume();
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiterError.resume();
                } else {
                    waiterError.fail(UNEXPECTED_ERROR_STR + speedTestError);
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
    private void testSocket(final SpeedTestSocket socket) {

        try {
            final Field field = socket.getClass().getDeclaredField("socket");
            Assert.assertNotNull(HEADER + "socket is null", field);
            field.setAccessible(true);

            Assert.assertNull(HEADER + "socket value at init", field.get(socket));

            socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL);

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            testSocketConnected((Socket) field.get(socket));

            socket.forceStopTask();

            try {
                waiterError.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(socket)).isClosed());

            initCountDown();

            socket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL,
                    FILE_SIZE_MEDIUM);

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            testSocketConnected((Socket) field.get(socket));

            socket.forceStopTask();

            try {
                waiterError.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            Assert.assertTrue(HEADER + "socket closed after stop upload", ((Socket) field.get(socket)).isClosed());

            initCountDown();

            socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL);

            try {
                waiterError.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            testSocketConnected((Socket) field.get(socket));
            socket.closeSocket();
            Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(socket)).isClosed());

        } catch (NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
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
        testReportEmpty("download report empty - ", report);
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

        testReportEmpty("upload report empty - ", report);
    }

    /**
     * test download report not empty.
     */
    @Test
    public void downloadReportNotEmptyTest() {
        socket = new SpeedTestSocket();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        waiter = new Waiter();

        socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL);

        try {
            waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }

        final SpeedTestReport report = socket.getLiveDownloadReport();

        Assert.assertEquals(HEADER + "download report not empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);

        testReportNotEmpty(waiter, report, FILE_SIZE_LARGE);

        socket.forceStopTask();
    }

    /**
     * test upload report empty.
     */
    @Test
    public void uploadReportNotEmptyTest() {
        socket = new SpeedTestSocket();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }
        });

        waiter = new Waiter();

        socket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL,
                FILE_SIZE_REGULAR);
        try {
            waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }

        final SpeedTestReport report = socket.getLiveUploadReport();

        Assert.assertEquals(HEADER + "upload report not empty - mode incorrect", report.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);

        testReportNotEmpty(waiter, report, FILE_SIZE_REGULAR);

        socket.forceStopTask();
    }

    /**
     * Test report empty.
     *
     * @param headerMessage test header Message
     * @param report        speed test report object
     */
    private void testReportEmpty(final String headerMessage, final SpeedTestReport report) {

        Assert.assertEquals(HEADER + headerMessage + "progress incorrect", report.getProgressPercent(),
                0, 0);
        Assert.assertNotEquals(HEADER + headerMessage + "time incorrect", report.getReportTime(),
                0);
        Assert.assertEquals(HEADER + headerMessage + "request num incorrect", report.getRequestNum(),
                1);
        Assert.assertEquals(HEADER + headerMessage + "start time incorrect", report.getStartTime(),
                0);
        Assert.assertEquals(HEADER + headerMessage + "temporary packet size incorrect", report
                        .getTemporaryPacketSize(),
                0);
        Assert.assertEquals(HEADER + headerMessage + "total packet size incorrect", report
                        .getTotalPacketSize(),
                0);
        Assert.assertEquals(HEADER + headerMessage + "transfer rate bps incorrect", report
                        .getTransferRateBit
                                ().intValue(),
                0);
        Assert.assertEquals(HEADER + headerMessage + "transfer rate ops incorrect", report
                        .getTransferRateOctet().intValue(),
                0);
    }

    /**
     * Test report not empty.
     *
     * @param report speed test report object
     */
    private void testReportNotEmpty(final Waiter waiter, final SpeedTestReport report, final long
            totalPacketSize) {

        waiter.assertTrue(report.getProgressPercent() > 0);
        waiter.assertTrue(report.getReportTime() != 0);
        waiter.assertTrue(report.getRequestNum() >= 1);
        waiter.assertTrue(report.getStartTime() != 0);
        waiter.assertTrue(report.getTemporaryPacketSize() > 0);
        waiter.assertEquals(report.getTotalPacketSize(), totalPacketSize);
        waiter.assertTrue(report.getTransferRateBit().intValue() > 0);
        waiter.assertTrue(report.getTransferRateOctet().intValue() > 0);

        //check transfer rate O = 8xB
        final float check = report.getTransferRateOctet().multiply(new BigDecimal("8")).floatValue();
        waiter.assertTrue(((report.getTransferRateBit().floatValue() + 0.1) >= check) &&
                ((report.getTransferRateBit().floatValue() - 0.1) <= check));
    }

    /**
     * Test listener callback for progress & result.
     */
    @Test
    public void progressResultCallbackDownloadTest() {
        socket = new SpeedTestSocket();

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = FILE_SIZE_REGULAR;

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
                checkSpeedTestResult(waiter2, packetSize, packetSizeExpected, transferRateBps, transferRateOps, true);
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                testReportNotEmpty(waiter, report, packetSizeExpected);
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(DOWNLOAD_ERROR_STR + speedTestError);
                    waiter2.fail(DOWNLOAD_ERROR_STR + speedTestError);
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
                waiter.fail(UPLOAD_ERROR_STR + " : shouldnt be in onUploadPacketsReceived");
                waiter2.fail(UPLOAD_ERROR_STR + " : shouldnt be in onUploadPacketsReceived");
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(UPLOAD_ERROR_STR + " : shouldnt be in onUploadError");
                waiter2.fail(UPLOAD_ERROR_STR + " : shouldnt be in onUploadError");
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                waiter.fail(UPLOAD_ERROR_STR + " : shouldnt be in onUploadProgress");
                waiter2.fail(UPLOAD_ERROR_STR + " : shouldnt be in onUploadProgress");
            }
        });

        socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL_1MO);

        try {
            waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }
        try {
            waiter2.await(WAITING_TIMEOUT_LONG_OPERATION, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }

        socket.forceStopTask();
    }

    /**
     * Test listener callback for progress & result for upload.
     */
    @Test
    public void progressResultCallbackUploadTest() {
        socket = new SpeedTestSocket();

        final Waiter waiter = new Waiter();
        final Waiter waiter2 = new Waiter();

        final int packetSizeExpected = FILE_SIZE_REGULAR;

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
                waiter.fail(UPLOAD_ERROR_STR + " : shouldnt be in onDownloadPacketsReceived");
                waiter2.fail(UPLOAD_ERROR_STR + " : shouldnt be in onDownloadPacketsReceived");

            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                waiter.fail(DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadProgress");
                waiter2.fail(DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadProgress");
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
                waiter2.fail(DOWNLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
                checkSpeedTestResult(waiter2, packetSize, packetSizeExpected, transferRateBps, transferRateOps, false);
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(UPLOAD_ERROR_STR + speedTestError);
                    waiter2.fail(UPLOAD_ERROR_STR + speedTestError);
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                testReportNotEmpty(waiter, report, packetSizeExpected);
                waiter.assertTrue(percent >= 0 && percent <= 100);
                waiter.resume();
            }
        });

        socket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL,
                packetSizeExpected);

        try {
            waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }
        try {
            waiter2.await(WAITING_TIMEOUT_LONG_OPERATION, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }

        socket.forceStopTask();
    }

    /**
     * Check speed test result callback.
     *
     * @param waiter          Waiter object
     * @param packetSize      packet size received in result callback
     * @param transferRateBps transfer rate in b/s from result callback
     * @param transferRateOps transfer rate in octet/ps from result callback
     */
    private void checkSpeedTestResult(final Waiter waiter, final long packetSize, final long packetSizeExpected,
                                      final BigDecimal transferRateBps,
                                      final BigDecimal transferRateOps, final boolean isDownload) {

        SpeedTestReport report;

        if (isDownload) {
            report = socket.getLiveDownloadReport();
        } else {
            report = socket.getLiveUploadReport();
        }
        testReportNotEmpty(waiter, report, packetSize);

        waiter.assertTrue(report.getProgressPercent() ==
                100);
        waiter.assertEquals(packetSize, packetSizeExpected);
        waiter.assertNotNull(transferRateBps);
        waiter.assertNotNull(transferRateOps);
        waiter.assertTrue(transferRateBps.intValue()
                > 0);
        waiter.assertTrue(transferRateBps.intValue()
                > 0);

        final float check = transferRateOps.multiply(new BigDecimal("8")).floatValue();

        waiter.assertTrue(((transferRateBps.floatValue() + 0.1) >= check) &&
                ((transferRateBps.floatValue() - 0.1) <= check));

        waiter.assertEquals(report.getTransferRateBit(), transferRateBps);
        waiter.assertEquals(report.getTransferRateOctet(), transferRateOps);
        waiter.assertEquals(report.getTotalPacketSize(), packetSize);
        waiter.assertEquals(report.getTemporaryPacketSize(), packetSize);

        waiter.resume();
    }

    /**
     * Test socket connection error.
     */
    @Test
    public void socketConnectionErrorTest() {
        socket = new SpeedTestSocket();
        noCheckMessage = true;
        initErrorListener(SpeedTestError.CONNECTION_ERROR);

        testErrorHandler("dispatchError");
    }

    /**
     * An initialization for all error callback test suite.
     *
     * @param error type of error to catch
     */
    private void initErrorListener(final SpeedTestError error) {

        waiter = new Waiter();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError.equals(error) && isDownload) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestSocketTest.this.errorMessage);
                    }
                    waiter.resume();
                } else if (isForceStop && isDownload && speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestSocketTest.this.errorMessage + errorMessageSuffix);
                    }
                    waiter.resume();
                } else {
                    waiter.fail("error connection error expected");
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError.equals(error) && !isDownload && !isForceStop) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestSocketTest.this.errorMessage);
                    }
                    waiter.resume();
                } else if (isForceStop && !isDownload && speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestSocketTest.this.errorMessage + errorMessageSuffix);
                    }
                    waiter.resume();
                } else {
                    waiter.fail("error connection error expected");
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });
    }

    /**
     * Test http frame error for forceClose or not.
     *
     * @param methodName method to reflect
     */
    private void testHttpFrameErrorHandler(final String methodName) {

        Class[] cArg = new Class[1];
        cArg[0] = HttpFrame.class;

        try {
            final Method method = socket.getClass().getDeclaredMethod(methodName, cArg);
            method.setAccessible(true);
            Assert.assertNotNull(method);

            isForceStop = false;
            isDownload = true;

            final HttpFrame frame = new HttpFrame();
            final HashMap<String, String> headers = new HashMap<String, String>();
            headers.put(HttpHeader.CONTENT_LENGTH, String.valueOf(10));
            frame.setHeaders(headers);

            method.invoke(socket, frame);

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            socket.forceStopTask();
            isForceStop = true;

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

        } catch (NoSuchMethodException e) {
            Assert.fail(e.getMessage());
        } catch (InvocationTargetException e) {
            Assert.fail(e.getMessage());
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test http error for forceClose or not.
     *
     * @param methodName method to reflect
     */
    private void testHttpErrorHandler(final String methodName) {

        Class[] cArg = new Class[1];
        cArg[0] = HttpStates.class;

        testErrorForceClose(methodName, cArg);
    }

    /**
     * Test an error handler + force close event.
     */
    private void testErrorForceClose(final String methodName, final Class... cArg) {

        try {
            final Method method = socket.getClass().getDeclaredMethod(methodName, cArg);
            method.setAccessible(true);
            Assert.assertNotNull(method);

            isForceStop = false;
            isDownload = true;

            iterateIncorrectHeaders(method);

            socket.forceStopTask();
            isForceStop = true;

            iterateIncorrectHeaders(method);

        } catch (NoSuchMethodException e) {
            Assert.fail(e.getMessage());
        } catch (InvocationTargetException e) {
            Assert.fail(e.getMessage());
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Iterate over http header enum to test all values except OK.
     *
     * @param method method to use to test error
     */
    private void iterateIncorrectHeaders(final Method method) throws InvocationTargetException, IllegalAccessException {

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                method.invoke(socket, state);

                try {
                    waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                }
            }
        }
    }

    /**
     * Test connection error for download/upload/forceStopTask.
     *
     * @param methodName method name to reflect
     */
    private void testErrorHandler(final String methodName) {

        Class[] cArg = new Class[2];
        cArg[0] = boolean.class;
        cArg[1] = String.class;

        try {
            final Method method = socket.getClass().getDeclaredMethod(methodName, cArg);
            method.setAccessible(true);
            Assert.assertNotNull(method);

            isForceStop = false;
            isDownload = true;
            method.invoke(socket, isDownload, errorMessage);

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }
            isDownload = false;
            method.invoke(socket, isDownload, errorMessage);

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            socket.forceStopTask();
            isForceStop = true;
            isDownload = false;
            method.invoke(socket, isDownload, errorMessage);

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

            isDownload = true;
            method.invoke(socket, isDownload, errorMessage);

            try {
                waiter.await(WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
            }

        } catch (NoSuchMethodException e) {
            Assert.fail(e.getMessage());
        } catch (InvocationTargetException e) {
            Assert.fail(e.getMessage());
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test socket timeout error.
     */
    @Test
    public void socketTimeoutErrorTest() {

        socket = new SpeedTestSocket();
        noCheckMessage = true;
        initErrorListener(SpeedTestError.SOCKET_TIMEOUT);

        testErrorHandler("dispatchSocketTimeout");
    }

    /**
     * Test http frame error.
     */
    @Test
    public void httpFrameErrorTest() {

        socket = new SpeedTestSocket();
        noCheckMessage = false;
        initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        testHttpErrorHandler("checkHttpFrameError");
    }

    /**
     * Test http header error.
     */
    @Test
    public void httpHeaderErrorTest() {

        socket = new SpeedTestSocket();
        noCheckMessage = false;
        initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        testHttpErrorHandler("checkHttpHeaderError");
    }

    /**
     * Test http content length error.
     */
    @Test
    public void httpContentLengthErrorTest() {
        socket = new SpeedTestSocket();
        noCheckMessage = false;
        initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        testHttpFrameErrorHandler("checkHttpContentLengthError");
    }
}
