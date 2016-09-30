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
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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
            InvocationTargetException {
        socket = new SpeedTestSocket();
        noCheckMessage = true;
        initErrorListener(SpeedTestError.CONNECTION_ERROR);

        testErrorHandler("dispatchError", true);
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
    }

    /**
     * Test http frame error for forceClose or not.
     *
     * @param methodName method to reflect
     */
    private void testHttpFrameErrorHandler(final String methodName) throws TimeoutException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Class[] cArg = new Class[1];
        cArg[0] = HttpFrame.class;

        final Method method = socket.getClass().getDeclaredMethod(methodName, cArg);
        method.setAccessible(true);
        Assert.assertNotNull(method);

        isForceStop = false;
        isDownload = true;

        final HttpFrame frame = new HttpFrame();
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeader.CONTENT_LENGTH, String.valueOf(0));
        frame.setHeaders(headers);

        method.invoke(socket, frame);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        isForceStop = true;
        socket.forceStopTask();

        method.invoke(socket, frame);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

    }

    /**
     * Test http error for forceClose or not.
     *
     * @param methodName method to reflect
     */
    private void testHttpErrorHandler(final String methodName) throws TimeoutException,
            NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {

        Class[] cArg = new Class[1];
        cArg[0] = HttpStates.class;

        testErrorForceClose(methodName, cArg);
    }

    /**
     * Test an error handler + force close event.
     */
    private void testErrorForceClose(final String methodName, final Class... cArg) throws TimeoutException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Method method = socket.getClass().getDeclaredMethod(methodName, cArg);
        method.setAccessible(true);
        Assert.assertNotNull(method);

        isForceStop = false;
        isDownload = true;

        iterateIncorrectHeaders(method);

        socket.forceStopTask();
        isForceStop = true;

        iterateIncorrectHeaders(method);
    }

    /**
     * Iterate over http header enum to test all values except OK.
     *
     * @param method method to use to test error
     */
    private void iterateIncorrectHeaders(final Method method) throws InvocationTargetException,
            IllegalAccessException, TimeoutException {

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                method.invoke(socket, state);

                waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Test connection error for download/upload/forceStopTask.
     *
     * @param methodName method name to reflect
     */
    private void testErrorHandler(final String methodName, final boolean forceCloseStatus) throws TimeoutException,
            NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Class[] cArg = new Class[2];
        cArg[0] = boolean.class;
        cArg[1] = String.class;

        final Method method = socket.getClass().getDeclaredMethod(methodName, cArg);
        method.setAccessible(true);
        Assert.assertNotNull(method);

        isForceStop = false;
        isDownload = true;
        method.invoke(socket, isDownload, errorMessage);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        isDownload = false;
        method.invoke(socket, isDownload, errorMessage);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        if (forceCloseStatus) {

            socket.forceStopTask();
            isForceStop = true;
            isDownload = false;
            method.invoke(socket, isDownload, errorMessage);

            waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

            isDownload = true;
            method.invoke(socket, isDownload, errorMessage);

            waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        }
    }

    /**
     * Test socket timeout error.
     */
    @Test
    public void socketTimeoutErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        socket = new SpeedTestSocket();
        noCheckMessage = true;
        initErrorListener(SpeedTestError.SOCKET_TIMEOUT);

        testErrorHandler("dispatchSocketTimeout", false);
    }

    /**
     * Test http frame error.
     */
    @Test
    public void httpFrameErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        socket = new SpeedTestSocket();
        noCheckMessage = false;
        initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        testHttpErrorHandler("checkHttpFrameError");
    }

    /**
     * Test http header error.
     */
    @Test
    public void httpHeaderErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        socket = new SpeedTestSocket();
        noCheckMessage = false;
        initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        testHttpErrorHandler("checkHttpHeaderError");
    }

    /**
     * Test http content length error.
     */
    @Test
    public void httpContentLengthErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        socket = new SpeedTestSocket();
        noCheckMessage = false;
        initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        testHttpFrameErrorHandler("checkHttpContentLengthError");
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
