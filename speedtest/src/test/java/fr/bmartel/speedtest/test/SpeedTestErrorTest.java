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

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.constants.HttpHeader;
import fr.bmartel.protocol.http.states.HttpStates;
import fr.bmartel.speedtest.ISpeedTestListener;
import fr.bmartel.speedtest.SpeedTestError;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import net.jodah.concurrentunit.Waiter;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Error testing.
 *
 * @author Bertrand Martel
 */
public class SpeedTestErrorTest {

    /**
     * SpeedTtest socket used in all functional tests.
     */
    private SpeedTestSocket socket;

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
     * Waiter for speed test listener callback.
     */
    private static Waiter waiter;

    /**
     * Test socket connection error.
     */
    @Test
    public void socketConnectionErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {
        socket = new SpeedTestSocket();
        noCheckMessage = true;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.CONNECTION_ERROR);

        testErrorHandler(listenerList, true, true);
    }

    /**
     * An initialization for all error callback test suite.
     *
     * @param error type of error to catch
     */
    private List<ISpeedTestListener> initErrorListener(final SpeedTestError error) throws NoSuchFieldException,
            IllegalAccessException {

        waiter = new Waiter();

        final List<ISpeedTestListener> listenerList = new ArrayList<>();

        listenerList.add(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError.equals(error) && isDownload) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestErrorTest.this.errorMessage);
                    }
                    waiter.resume();
                } else if (isForceStop && isDownload && speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestErrorTest.this.errorMessage + errorMessageSuffix);
                    }
                    waiter.resume();
                } else {
                    waiter.fail("error connection error expected");
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError.equals(error) && !isDownload && !isForceStop) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestErrorTest.this.errorMessage);
                    }
                    waiter.resume();
                } else if (isForceStop && !isDownload && speedTestError == SpeedTestError.FORCE_CLOSE_SOCKET) {
                    if (noCheckMessage) {
                        waiter.assertEquals(errorMessage, SpeedTestErrorTest.this.errorMessage + errorMessageSuffix);
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

        SpeedTestUtils.setListenerList(socket, listenerList);

        return listenerList;
    }

    /**
     * Test connection error for download/upload/forceStopTask.
     */
    private void testErrorHandler(final List<ISpeedTestListener> listenerList, final boolean forceCloseStatus,
                                  final boolean dispatchError)
            throws TimeoutException {

        isForceStop = false;
        isDownload = true;

        if (dispatchError) {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchError(isForceStop, listenerList, isDownload, errorMessage);
        } else {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(isForceStop, listenerList, isDownload,
                    errorMessage);
        }
        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        isDownload = false;

        if (dispatchError) {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchError(isForceStop, listenerList, isDownload, errorMessage);
        } else {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(isForceStop, listenerList, isDownload,
                    errorMessage);
        }

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        if (forceCloseStatus) {

            socket.forceStopTask();
            isForceStop = true;
            isDownload = false;

            if (dispatchError) {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchError(isForceStop, listenerList, isDownload, errorMessage);
            } else {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(isForceStop, listenerList, isDownload,
                        errorMessage);
            }

            waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

            isDownload = true;

            if (dispatchError) {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchError(isForceStop, listenerList, isDownload, errorMessage);
            } else {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(isForceStop, listenerList, isDownload,
                        errorMessage);
            }

            waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        }
    }

    /**
     * Test socket timeout error.
     */
    @Test
    public void socketTimeoutErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        socket = new SpeedTestSocket();
        noCheckMessage = true;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.SOCKET_TIMEOUT);

        testErrorHandler(listenerList, false, false);
    }

    /**
     * Test http frame error.
     */
    @Test
    public void httpFrameErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        socket = new SpeedTestSocket();
        noCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        isForceStop = false;
        isDownload = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpFrameError(isForceStop, listenerList, state);
                waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }

        socket.forceStopTask();
        isForceStop = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpFrameError(isForceStop, listenerList, state);
                waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Test http header error.
     */
    @Test
    public void httpHeaderErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        socket = new SpeedTestSocket();
        noCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        isForceStop = false;
        isDownload = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpHeaderError(isForceStop, listenerList, state);
                waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }

        socket.forceStopTask();
        isForceStop = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpHeaderError(isForceStop, listenerList, state);
                waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Test http content length error.
     */
    @Test
    public void httpContentLengthErrorTest() throws TimeoutException, NoSuchFieldException, IllegalAccessException {
        socket = new SpeedTestSocket();
        noCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        isForceStop = false;
        isDownload = true;

        final HttpFrame frame = new HttpFrame();
        final HashMap<String, String> headers = new HashMap<>();
        headers.put(HttpHeader.CONTENT_LENGTH, String.valueOf(0));
        frame.setHeaders(headers);

        fr.bmartel.speedtest.SpeedTestUtils.checkHttpContentLengthError(isForceStop, listenerList, frame);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        isForceStop = true;
        socket.forceStopTask();

        fr.bmartel.speedtest.SpeedTestUtils.checkHttpContentLengthError(isForceStop, listenerList, frame);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Test unknown host error for download.
     */
    @Test
    public void unknownHostDownloadTest() throws TimeoutException {
        unknownHostTest(true);
    }

    /**
     * Test unknown host error for download.
     */
    @Test
    public void unknownHostUploadTest() throws TimeoutException {
        unknownHostTest(false);
    }

    /**
     * Test unknown host for download/upload.
     *
     * @param download define if download or upload is testing.
     */
    private void unknownHostTest(final boolean download) throws TimeoutException {
        socket = new SpeedTestSocket();

        waiter = new Waiter();

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {

                if (download) {
                    if (speedTestError != SpeedTestError.CONNECTION_ERROR && speedTestError != SpeedTestError
                            .FORCE_CLOSE_SOCKET) {
                        waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    } else {
                        waiter.resume();
                    }
                } else {
                    waiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {

                if (download) {
                    waiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadError");
                } else {
                    if (speedTestError != SpeedTestError.CONNECTION_ERROR && speedTestError != SpeedTestError
                            .FORCE_CLOSE_SOCKET) {
                        waiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    } else {
                        waiter.resume();
                    }
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }
        });

        if (download) {
            socket.startDownload(TestCommon.SPEED_TEST_FAKE_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                    .SPEED_TEST_SERVER_URI_DL_1MO);
        } else {
            socket.startUpload(TestCommon.SPEED_TEST_FAKE_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                            .SPEED_TEST_SERVER_URI_UL,
                    TestCommon.FILE_SIZE_MEDIUM);
        }

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        socket.forceStopTask();
    }
}
