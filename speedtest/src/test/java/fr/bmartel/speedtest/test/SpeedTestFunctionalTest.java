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

import fr.bmartel.protocol.http.HttpResponseFrame;
import fr.bmartel.protocol.http.HttpVersion;
import fr.bmartel.protocol.http.constants.StatusCodeList;
import fr.bmartel.protocol.http.inter.IHttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;
import fr.bmartel.speedtest.*;
import fr.bmartel.speedtest.test.server.HttpServer;
import fr.bmartel.speedtest.test.server.IHttpServerEventListener;
import fr.bmartel.speedtest.test.server.IHttpStream;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * download / upload functional test mocking a speed test server.
 *
 * @author Bertrand Martel
 */
public class SpeedTestFunctionalTest {

    /**
     * speed examples server host name.
     */
    private final static String SPEED_TEST_SERVER_HOST = "127.0.0.1";

    /**
     * speed examples server port.
     */
    private final static int SPEED_TEST_SERVER_PORT = 4242;

    /**
     * speed examples 1Mo server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL_1MO = "/fichiers/1Mo.dat";

    /**
     * speed examples 5Mo server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL_5MO = "/fichiers/5Mo.dat";

    /**
     * speed examples 10Mo server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL_10MO = "/fichiers/10Mo.dat";

    /**
     * socket timeout uri.
     */
    private final static String SPEED_TEST_SERVER_URI_TIMEOUT = "/timeout";

    /**
     * upload server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_UL = "/upload";

    /**
     * default timeout waiting time for long operation such as DL / UL
     */
    private final static int WAITING_TIMEOUT_LONG_OPERATION = 10;

    /**
     * http server.
     */
    private static HttpServer server;

    /**
     * transfer rate reference in octet.
     */
    private BigDecimal transfeRateOctetRef;

    /**
     * transfer rate reference in bps.
     */
    private BigDecimal transferRateBpsRef;

    /**
     * SpeedTtest socket used in all functional tests.
     */
    private SpeedTestSocket socket;

    /**
     * transfer rate value received in ops.
     */
    private BigDecimal expectedTransferRateOps;

    /**
     * transfer rate value reiceved in bps.
     */
    private BigDecimal expectedTransferRateBps;

    /**
     * socketTimeout used.
     */
    private int socketTimeout;

    /**
     * Common waiter for functional test.
     */
    private Waiter waiter;

    /**
     * Define if download mode enabled.
     */
    private boolean isDownload;

    /**
     * default socket timeout.
     */
    private final static int DEFAULT_SOCKET_TIMEOUT = 10000;

    @Test
    public void downloadTest() throws TimeoutException {

        calculateReference();

        socket = new SpeedTestSocket();

        socket.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
                expectedTransferRateOps = transferRateOps;
                expectedTransferRateBps = transferRateBps;
                waiter.resume();
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.UPLOAD_ERROR_STR + speedTestError);
                waiter.resume();
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        testDownload(SPEED_TEST_SERVER_URI_DL_1MO);
        testDownload(SPEED_TEST_SERVER_URI_DL_5MO);
        testDownload(SPEED_TEST_SERVER_URI_DL_10MO);

        stopServer();
    }

    @Test
    public void uploadTest() throws TimeoutException {

        calculateReference();

        socket = new SpeedTestSocket();

        socket.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);

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
                waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                waiter.resume();
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
                expectedTransferRateOps = transferRateOps;
                expectedTransferRateBps = transferRateBps;
                waiter.resume();
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UPLOAD_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        testUpload(1000000);
        testUpload(10000000);

        stopServer();
    }

    @Test
    public void timeoutTest() throws TimeoutException {

        calculateReference();

        socket = new SpeedTestSocket();

        socket.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);

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
                if (isDownload) {
                    if (speedTestError == SpeedTestError.SOCKET_TIMEOUT) {
                        waiter.resume();
                    } else {
                        waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    }
                } else {
                    waiter.fail("onDownloadError : shouldnt be in that cb" + speedTestError);
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (!isDownload) {
                    if (speedTestError == SpeedTestError.SOCKET_TIMEOUT) {
                        waiter.resume();
                    } else {
                        waiter.fail(TestCommon.UPLOAD_ERROR_STR + speedTestError);
                    }
                } else {
                    waiter.fail("onUploadError : shouldnt be in that cb" + speedTestError);
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        socketTimeout = 2000;

        socket.setSocketTimeout(socketTimeout);

        isDownload = true;
        socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_TIMEOUT);

        waiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        stopServer();
    }

    /**
     * Test upload with given packet size.
     *
     * @param size
     */
    private void testUpload(final int size) throws TimeoutException {

        waiter = new Waiter();

        socket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL, size);

        waiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        testTransferRate();

        socket.forceStopTask();
    }

    /**
     * Test download with given URI.
     *
     * @param uri
     */
    private void testDownload(final String uri) throws TimeoutException {

        waiter = new Waiter();

        socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, uri);

        waiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        testTransferRate();

        socket.forceStopTask();
    }

    /**
     * Compare transfer rate calculated to expected value.
     */
    private void testTransferRate() {

        Assert.assertNotNull(expectedTransferRateOps);
        Assert.assertNotNull(expectedTransferRateBps);

        Assert.assertTrue(expectedTransferRateBps.intValue() > 0);
        Assert.assertTrue(expectedTransferRateOps.intValue() > 0);
    }

    /**
     * start server & calculate transfer rate reference.
     */
    private void calculateReference() throws TimeoutException {

        startServer();

        socket = new SpeedTestSocket();

        socket.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);

        final Waiter waiter = new Waiter();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {
                SpeedTestFunctionalTest.this.transfeRateOctetRef = transferRateOps;
                SpeedTestFunctionalTest.this.transferRateBpsRef = transferRateBps;
                waiter.resume();
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail("onDownloadError : " + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail("onUploadError : " + speedTestError);
                waiter.resume();
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL_1MO);

        waiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        socket.forceStopTask();

        Assert.assertNotNull(transfeRateOctetRef);
        Assert.assertNotNull(transferRateBpsRef);
        Assert.assertTrue(transfeRateOctetRef.intValue() > 0);
        Assert.assertTrue(transferRateBpsRef.intValue() > 0);
    }

    @Test
    public void chainDownloadUploadTest() throws TimeoutException {

        socket = new SpeedTestSocket();

        socket.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);

        final int totalPacketSize = 1000000;

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {

                checkResult(waiter, totalPacketSize, packetSize, transferRateBps, transferRateOps, true, true);
                waiter.resume();

                socket.startUpload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                        .SPEED_TEST_SERVER_URI_UL, totalPacketSize);

            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                waiter.resume();
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                waiter.resume();
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {
                checkResult(waiter, totalPacketSize, packetSize, transferRateBps, transferRateOps, false, true);
                waiter.resume();
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                waiter.resume();
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        waiter = new Waiter();

        socket.startDownload(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                .SPEED_TEST_SERVER_URI_DL_1MO);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, SECONDS, 2);
    }

    /**
     * Check speed test result.
     *
     * @param waiter
     * @param actualPacketSize
     * @param expectedPacketSize
     * @param transferRateBps
     * @param transferRateOps
     * @param isDownload
     * @param isRepeat
     */
    private void checkResult(final Waiter waiter,
                             final long actualPacketSize,
                             final long expectedPacketSize,
                             final BigDecimal transferRateBps,
                             final BigDecimal transferRateOps,
                             final boolean isDownload,
                             final boolean isRepeat) {

        waiter.assertEquals(expectedPacketSize, actualPacketSize);
        waiter.assertNotNull(transferRateBps);
        waiter.assertNotNull(transferRateOps);
        waiter.assertTrue(transferRateBps.intValue() > 0);
        waiter.assertTrue(transferRateOps.intValue() > 0);

        //check transfer rate O = 8xB
        final float check = transferRateOps.multiply(new BigDecimal("8")).floatValue();
        waiter.assertTrue(((transferRateBps.floatValue() + 0.1) >= check) &&
                ((transferRateBps.floatValue() - 0.1) <= check));

        if (isDownload) {
            SpeedTestUtils.testReportNotEmpty(waiter, socket.getLiveDownloadReport(), expectedPacketSize, false,
                    isRepeat);
        } else {
            SpeedTestUtils.testReportNotEmpty(waiter, socket.getLiveUploadReport(), expectedPacketSize, false,
                    isRepeat);
        }
    }

    @Test
    public void chainDownloadUploadRepeatTest() throws TimeoutException {

        socket = new SpeedTestSocket();

        socket.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);

        final int totalPacketSize = 1000000;

        socket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBitPerSeconds,
                                                  final BigDecimal transferRateOctetPerSeconds) {
                //called when download is finished
                checkResult(waiter, totalPacketSize, packetSize, transferRateBitPerSeconds,
                        transferRateOctetPerSeconds, true, true);
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBitPerSeconds, final
            BigDecimal transferRateOctetPerSeconds) {
                //called when upload is finished
                checkResult(waiter, totalPacketSize, packetSize, transferRateBitPerSeconds,
                        transferRateOctetPerSeconds, false, true);
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                    waiter.resume();
                }
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport downloadReport) {
                //notify download progress
                waiter.assertTrue(percent >= 0 && percent <= 100);
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport uploadReport) {
                //notify upload progress
                waiter.assertTrue(percent >= 0 && percent <= 100);
            }
        });

        waiter = new Waiter();

        startRepeatDownload(waiter, 3000, 1000, 2, 1000000);

        waiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
    }

    /**
     * Start repeat downloading.
     *
     * @param waiter
     * @param duration
     * @param reportInterval
     * @param chainCount
     * @param packetSize
     */
    private void startRepeatDownload(final Waiter waiter,
                                     final int duration,
                                     final int reportInterval,
                                     final int chainCount,
                                     final int packetSize) {

        socket.startDownloadRepeat(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                        .SPEED_TEST_SERVER_URI_DL_1MO,
                duration, reportInterval, new
                        IRepeatListener() {
                            @Override
                            public void onFinish(final SpeedTestReport report) {

                                SpeedTestUtils.compareFinishReport(socket, waiter, packetSize, report, true);

                                startRepeatUpload(waiter, duration, reportInterval, chainCount, packetSize);
                            }

                            @Override
                            public void onReport(final SpeedTestReport report) {
                                if (report.getProgressPercent() > 0) {
                                    SpeedTestUtils.testReportNotEmpty(waiter, report, packetSize, true, true);
                                } else {
                                    SpeedTestUtils.testReportEmpty("empty report", report, true);
                                }
                            }
                        });
    }

    /**
     * Start repeat uploading.
     *
     * @param waiter
     * @param duration
     * @param reportInterval
     * @param chainCount
     * @param packetSize
     */
    private void startRepeatUpload(final Waiter waiter,
                                   final int duration,
                                   final int reportInterval,
                                   final int chainCount,
                                   final int packetSize) {

        socket.startUploadRepeat(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                        .SPEED_TEST_SERVER_URI_UL,
                duration, reportInterval, packetSize, new
                        IRepeatListener() {
                            @Override
                            public void onFinish(final SpeedTestReport report) {

                                SpeedTestUtils.compareFinishReport(socket, waiter, packetSize, report, false);

                                final int count = chainCount - 1;

                                if (count > 0) {
                                    startRepeatDownload(waiter, duration, reportInterval, count, packetSize);
                                } else {
                                    waiter.resume();
                                }
                            }

                            @Override
                            public void onReport(final SpeedTestReport report) {
                                if (report.getProgressPercent() > 0) {
                                    SpeedTestUtils.testReportNotEmpty(waiter, report, packetSize, true, true);
                                } else {
                                    SpeedTestUtils.testReportEmpty("empty report", report, true);
                                }
                            }
                        });
    }

    /**
     * Start Http server.
     */
    private void startServer() throws TimeoutException {

        // initiate HTTP server
        server = new HttpServer(SPEED_TEST_SERVER_PORT);

        // set ssl encryption
        server.setSsl(false);

        final Waiter waiter = new Waiter();

        server.addServerEventListener(new IHttpServerEventListener() {

            @Override
            public void onServerStarted() {
                waiter.resume();
            }

            @Override
            public void onHttpFrameReceived(final IHttpFrame httpFrame,
                                            final HttpStates receptionStates, final IHttpStream httpStream) {

                if (receptionStates == HttpStates.HTTP_FRAME_OK && httpFrame.isHttpRequestFrame()) {

                    byte[] body = "OK".getBytes();

                    switch (httpFrame.getMethod()) {
                        case "GET":

                            switch (httpFrame.getUri()) {
                                case SPEED_TEST_SERVER_URI_DL_1MO:
                                    body = new RandomGen(1000000).nextArray();
                                    break;
                                case SPEED_TEST_SERVER_URI_DL_5MO:
                                    body = new RandomGen(5000000).nextArray();
                                    break;
                                case SPEED_TEST_SERVER_URI_DL_10MO:
                                    body = new RandomGen(10000000).nextArray();
                                    break;
                                case SPEED_TEST_SERVER_URI_TIMEOUT:
                                    try {
                                        Thread.sleep(socketTimeout + 1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                default:
                                    break;
                            }
                            break;

                        case "POST":

                            switch (httpFrame.getUri()) {
                                case SPEED_TEST_SERVER_URI_UL:
                                    break;
                                case SPEED_TEST_SERVER_URI_TIMEOUT:
                                    try {
                                        Thread.sleep(socketTimeout + 1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                default:
                                    break;
                            }
                            break;

                        default:
                            break;
                    }
                    httpStream.writeHttpFrame(new HttpResponseFrame(
                            StatusCodeList.OK, new HttpVersion(1, 1),
                            new HashMap<String, String>(), body).toString().getBytes());
                }
            }

        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                server.start(); // start HTTP server => this method will block
            }
        }).start();

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, SECONDS);
    }

    private void stopServer() {
        server.closeServer();
    }
}
