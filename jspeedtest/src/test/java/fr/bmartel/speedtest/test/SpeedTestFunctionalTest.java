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

import fr.bmartel.protocol.http.HttpResponseFrame;
import fr.bmartel.protocol.http.HttpVersion;
import fr.bmartel.protocol.http.StatusCodeObject;
import fr.bmartel.protocol.http.constants.StatusCodeList;
import fr.bmartel.protocol.http.inter.IHttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.SpeedTestMode;
import fr.bmartel.speedtest.model.UploadStorageType;
import fr.bmartel.speedtest.test.server.HttpServer;
import fr.bmartel.speedtest.test.server.IHttpServerEventListener;
import fr.bmartel.speedtest.test.server.IHttpStream;
import fr.bmartel.speedtest.test.server.ServerRetryTest;
import fr.bmartel.speedtest.test.utils.SpeedTestUtils;
import fr.bmartel.speedtest.test.utils.TestCommon;
import fr.bmartel.speedtest.utils.RandomGen;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * download / upload functional test mocking a speed test mServer.
 *
 * @author Bertrand Martel
 */
public class SpeedTestFunctionalTest extends ServerRetryTest {

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
     * 301 endpoint.
     */
    private final static String SPEED_TEST_SERVER_URI_301 = "/file/301";

    /**
     * 302 endpoint.
     */
    private final static String SPEED_TEST_SERVER_URI_302 = "/file/302";

    /**
     * 307 endpoint.
     */
    private final static String SPEED_TEST_SERVER_URI_307 = "/file/307";

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
     * transfer rate reference in octet.
     */
    private BigDecimal mTransfeRateOctetRef;

    /**
     * transfer rate reference in bps.
     */
    private BigDecimal mTransferRateBpsRef;

    /**
     * transfer rate value received in ops.
     */
    private BigDecimal mExpectedTransferRateOps;

    /**
     * transfer rate value received in bps.
     */
    private BigDecimal mExpectedTransferRateBps;

    /**
     * socket timeout used.
     */
    private int mSocketTimeout;

    /**
     * Common waiter for functional test.
     */
    private Waiter mWaiter;

    /**
     * number of chain for DL/UL/DL chain requests.
     */
    private int chainCount = 1;

    @Before
    public void setup() {
        mExpectedTransferRateOps = null;
        mExpectedTransferRateBps = null;
    }

    @Test
    public void download1MTest() throws TimeoutException {
        initTask(true);
        testDownload(SPEED_TEST_SERVER_URI_DL_1MO);
        stopTask();
    }

    @Test
    public void downloadRedirectTest() throws TimeoutException {
        initTask(true);
        testDownload(SPEED_TEST_SERVER_URI_301);
        testDownload(SPEED_TEST_SERVER_URI_302);
        testDownload(SPEED_TEST_SERVER_URI_307);
        stopTask();
    }

    @Test
    public void download5MTest() throws TimeoutException {
        initTask(true);
        testDownload(SPEED_TEST_SERVER_URI_DL_5MO);
        stopTask();
    }

    @Test
    public void download10MTest() throws TimeoutException {
        initTask(true);
        testDownload(SPEED_TEST_SERVER_URI_DL_10MO);
        stopTask();
    }

    @Test
    public void downloadErrorTest() throws TimeoutException {
        initTask(true);
        testError(SpeedTestError.MALFORMED_URI, "://" + SPEED_TEST_SERVER_HOST + ":" +
                SPEED_TEST_SERVER_PORT +
                SPEED_TEST_SERVER_URI_DL_1MO, true);
        stopTask();
    }

    @Test
    public void upload1MTest() throws TimeoutException {
        initTask(false);
        testUpload(SPEED_TEST_SERVER_URI_UL, 1000000, true);
        stopTask();
    }

    @Test
    public void uploadRedirectTest() throws TimeoutException {
        initTask(false);
        testUpload(SPEED_TEST_SERVER_URI_301, 1000000, true);
        testUpload(SPEED_TEST_SERVER_URI_302, 1000000, true);
        testUpload(SPEED_TEST_SERVER_URI_307, 1000000, true);
        stopTask();
    }

    @Test
    public void upload10MTest() throws TimeoutException {
        initTask(false);
        testUpload(SPEED_TEST_SERVER_URI_UL, 10000000, true);
        stopTask();
    }

    @Test
    public void uploadErrorTest() throws TimeoutException {
        initTask(false);
        testError(SpeedTestError.MALFORMED_URI, "://" + SPEED_TEST_SERVER_HOST + ":" +
                SPEED_TEST_SERVER_PORT +
                SPEED_TEST_SERVER_URI_DL_1MO, false);
        stopTask();
    }

    private void initTask(final boolean download) throws TimeoutException {

        calculateReference();

        mSocket = new SpeedTestSocket();

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
        mSocket.clearListeners();
    }

    @Test
    public void timeoutTest() throws TimeoutException {

        calculateReference();

        mSocket = new SpeedTestSocket();

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mWaiter = new Waiter();

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //called to notify download progress
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError == SpeedTestError.SOCKET_TIMEOUT) {
                    mWaiter.resume();
                } else {
                    mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                }
            }
        });

        mSocketTimeout = 2000;

        mSocket.setSocketTimeout(mSocketTimeout);

        mSocket.startDownload("http://" + SPEED_TEST_SERVER_HOST + ":" + SPEED_TEST_SERVER_PORT +
                SPEED_TEST_SERVER_URI_TIMEOUT);

        mWaiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        stopServer();

        mSocket.clearListeners();
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

        mSocket.startUpload("http://" + SPEED_TEST_SERVER_HOST + ":" + SPEED_TEST_SERVER_PORT +
                url, size);

        mWaiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        testTransferRate();

        mSocket.forceStopTask();
    }

    /**
     * Test download with given URI.
     *
     * @param uri
     */
    private void testDownload(final String uri) throws TimeoutException {

        mWaiter = new Waiter();

        mSocket.startDownload("http://" + SPEED_TEST_SERVER_HOST + ":" + SPEED_TEST_SERVER_PORT + uri);

        mWaiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        testTransferRate();

        mSocket.forceStopTask();
    }

    /**
     * Test URI malformed or unsupported.
     */
    private void testError(final SpeedTestError expectedError, final String uri, final boolean isDownload) throws
            TimeoutException {

        mWaiter = new Waiter();

        mExpectedError = expectedError;

        if (isDownload) {
            mSocket.startDownload(uri);
        } else {
            mSocket.startUpload(uri, 1000000);
        }

        mWaiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        mExpectedError = null;

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
     * start mServer & calculate transfer rate reference.
     */
    private void calculateReference() throws TimeoutException {

        startServer();

        mSocket = new SpeedTestSocket();

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final Waiter waiter = new Waiter();

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                mTransfeRateOctetRef = report.getTransferRateOctet();
                mTransferRateBpsRef = report.getTransferRateBit();
                waiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //called to notify download progress
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                waiter.fail("onDownloadError : " + speedTestError);
                waiter.resume();
            }
        });

        mSocket.startDownload("http://" + SPEED_TEST_SERVER_HOST + ":" + SPEED_TEST_SERVER_PORT +
                SPEED_TEST_SERVER_URI_DL_1MO);

        waiter.await(WAITING_TIMEOUT_LONG_OPERATION, SECONDS);

        mSocket.forceStopTask();

        Assert.assertNotNull(mTransfeRateOctetRef);
        Assert.assertNotNull(mTransferRateBpsRef);
        Assert.assertTrue(mTransfeRateOctetRef.longValue() > 0);
        Assert.assertTrue(mTransferRateBpsRef.longValue() > 0);
    }

    @Test
    @Ignore
    public void chainDownloadUploadTest() throws TimeoutException {

        mSocket = new SpeedTestSocket();

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int totalPacketSize = 1000000;

        chainCount = 0;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {

                checkResult(mWaiter, totalPacketSize, report.getTotalPacketSize(), report.getTransferRateBit(),
                        report.getTransferRateOctet(), report.getSpeedTestMode() == SpeedTestMode.DOWNLOAD, true);

                if (report.getSpeedTestMode() == SpeedTestMode.DOWNLOAD) {
                    mWaiter.resume();
                    chainCount++;
                    if (chainCount < 2) {
                        mSocket.startUpload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
                                .SPEED_TEST_SERVER_PORT + TestCommon
                                .SPEED_TEST_SERVER_URI_UL, totalPacketSize);
                    }
                } else {
                    mWaiter.resume();

                    mSocket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
                            .SPEED_TEST_SERVER_PORT + TestCommon
                            .SPEED_TEST_SERVER_URI_DL_1MO);
                }
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                mWaiter.resume();
            }
        });

        mWaiter = new Waiter();

        mSocket.startDownload("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
                .SPEED_TEST_SERVER_PORT + TestCommon
                .SPEED_TEST_SERVER_URI_DL_1MO);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_LONG_OPERATION, SECONDS, 3);

        mSocket.clearListeners();
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

        if (!isRepeat) {
            waiter.assertEquals(expectedPacketSize, actualPacketSize);
        }
        waiter.assertNotNull(transferRateBps);
        waiter.assertNotNull(transferRateOps);
        waiter.assertTrue(transferRateBps.longValue() > 0);
        waiter.assertTrue(transferRateOps.longValue() > 0);

        //check transfer rate O = 8xB
        final float check = transferRateOps.multiply(new BigDecimal("8")).floatValue();
        waiter.assertTrue(((transferRateBps.floatValue() + 0.1) >= check) &&
                ((transferRateBps.floatValue() - 0.1) <= check));

        if (isDownload) {
            SpeedTestUtils.testReportNotEmpty(waiter, mSocket.getLiveReport(), expectedPacketSize, false,
                    isRepeat);
        } else {
            SpeedTestUtils.testReportNotEmpty(waiter, mSocket.getLiveReport(), expectedPacketSize, false,
                    isRepeat);
        }
    }

    @Test
    public void chainDownloadUploadRepeatTest() throws TimeoutException {

        mSocket = new SpeedTestSocket();

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        final int totalPacketSize = 1000000;

        mSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download/upload is finished
                checkResult(mWaiter, totalPacketSize, report.getTotalPacketSize(), report.getTransferRateBit(),
                        report.getTransferRateOctet(), report.getSpeedTestMode() == SpeedTestMode.DOWNLOAD, true);
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport downloadReport) {
                //notify download progress
                mWaiter.assertTrue(percent >= 0 && percent <= 100);
            }
        });

        mWaiter = new Waiter();

        startRepeatDownload(mWaiter, 3000, 1000, 2, 1000000);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);

        mSocket.clearListeners();
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

        mSocket.startDownloadRepeat("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
                        .SPEED_TEST_SERVER_PORT + TestCommon
                        .SPEED_TEST_SERVER_URI_DL_1MO,
                duration, reportInterval, new
                        IRepeatListener() {
                            @Override
                            public void onCompletion(final SpeedTestReport report) {

                                SpeedTestUtils.compareFinishReport(mSocket, waiter, packetSize, report, true);

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

        mSocket.startUploadRepeat("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
                        .SPEED_TEST_SERVER_PORT + TestCommon
                        .SPEED_TEST_SERVER_URI_UL,
                duration, reportInterval, packetSize, new
                        IRepeatListener() {
                            @Override
                            public void onCompletion(final SpeedTestReport report) {

                                SpeedTestUtils.compareFinishReport(mSocket, waiter, packetSize, report, false);

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
     * Start Http mServer.
     */
    private void startServer() throws TimeoutException {

        // initiate HTTP mServer
        mServer = new HttpServer(SPEED_TEST_SERVER_PORT);

        // set ssl encryption
        mServer.setSsl(false);

        final Waiter waiter = new Waiter();

        mServer.addServerEventListener(new IHttpServerEventListener() {

            @Override
            public void onServerStarted() {
                waiter.resume();
            }

            @Override
            public void onHttpFrameReceived(final IHttpFrame httpFrame,
                                            final HttpStates receptionStates, final IHttpStream httpStream) {

                if (receptionStates == HttpStates.HTTP_FRAME_OK && httpFrame.isHttpRequestFrame()) {

                    byte[] body = "OK".getBytes();

                    try {
                        final URL url = new URL(httpFrame.getUri());

                        switch (httpFrame.getMethod()) {
                            case "GET":

                                switch (url.getPath()) {
                                    case SPEED_TEST_SERVER_URI_DL_1MO:
                                        body = new RandomGen().generateRandomArray(1000000);
                                        break;
                                    case SPEED_TEST_SERVER_URI_DL_5MO:
                                        body = new RandomGen().generateRandomArray(5000000);
                                        break;
                                    case SPEED_TEST_SERVER_URI_DL_10MO:
                                        body = new RandomGen().generateRandomArray(10000000);
                                        break;
                                    case SPEED_TEST_SERVER_URI_302:
                                        sendRedirect("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                                                        TestCommon.SPEED_TEST_SERVER_PORT + TestCommon
                                                        .SPEED_TEST_SERVER_URI_DL_1MO,
                                                StatusCodeList.FOUND, httpStream);
                                        return;
                                    case SPEED_TEST_SERVER_URI_307:
                                        sendRedirect("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                                                        TestCommon.SPEED_TEST_SERVER_PORT + TestCommon
                                                        .SPEED_TEST_SERVER_URI_DL_1MO,
                                                StatusCodeList.TEMPORARY_REDIRECT, httpStream);
                                        return;
                                    case SPEED_TEST_SERVER_URI_301:
                                        sendRedirect("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                                                        TestCommon.SPEED_TEST_SERVER_PORT + TestCommon
                                                        .SPEED_TEST_SERVER_URI_DL_1MO,
                                                StatusCodeList.MOVED_PERMANENTLY, httpStream);
                                        return;
                                    case SPEED_TEST_SERVER_URI_TIMEOUT:
                                        try {
                                            Thread.sleep(mSocketTimeout + 1000);
                                        } catch (InterruptedException e) {
                                            waiter.fail(e.getMessage());
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                break;

                            case "POST":

                                switch (url.getPath()) {
                                    case SPEED_TEST_SERVER_URI_302:
                                        sendRedirect("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                                                        TestCommon.SPEED_TEST_SERVER_PORT + TestCommon
                                                        .SPEED_TEST_SERVER_URI_UL,
                                                StatusCodeList.FOUND, httpStream);
                                        return;
                                    case SPEED_TEST_SERVER_URI_307:
                                        sendRedirect("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                                                        TestCommon.SPEED_TEST_SERVER_PORT + TestCommon
                                                        .SPEED_TEST_SERVER_URI_UL,
                                                StatusCodeList.TEMPORARY_REDIRECT, httpStream);
                                        return;
                                    case SPEED_TEST_SERVER_URI_301:
                                        sendRedirect("http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" +
                                                        TestCommon.SPEED_TEST_SERVER_PORT + TestCommon
                                                        .SPEED_TEST_SERVER_URI_UL,
                                                StatusCodeList.MOVED_PERMANENTLY, httpStream);
                                        return;
                                    case SPEED_TEST_SERVER_URI_UL:
                                        break;
                                    case SPEED_TEST_SERVER_URI_TIMEOUT:
                                        try {
                                            Thread.sleep(mSocketTimeout + 1000);
                                        } catch (InterruptedException e) {
                                            waiter.fail(e.getMessage());
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
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        return;
                    }

                }
            }

        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                mServer.start(); // start HTTP server => this method will block
            }
        }).start();

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, SECONDS);
    }

    private void sendRedirect(final String url, final StatusCodeObject code, final IHttpStream httpStream) {
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Location", url);
        httpStream.writeHttpFrame(new HttpResponseFrame(
                code, new HttpVersion(1, 1),
                headers, new byte[]{}).toString().getBytes());
    }
}