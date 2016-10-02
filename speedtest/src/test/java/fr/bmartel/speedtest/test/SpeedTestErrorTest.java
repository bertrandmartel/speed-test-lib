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
    private SpeedTestSocket mSocket;

    /**
     * error message for testing error callback.
     */
    private static String errorMessage = "this is an error message";

    /**
     * download/upload mode for testing error callback.
     */
    private static boolean mDownload = true;

    /**
     * define if force stop is to be expected in error callback.
     */
    private static boolean mForceStop;

    /**
     * define if error message value should be checked.
     */
    private static boolean mNoCheckMessage;

    /**
     * Waiter for speed test listener callback.
     */
    private static Waiter mWaiter;

    /**
     * Test socket connection error.
     */
    @Test
    public void socketConnectionErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {
        mSocket = new SpeedTestSocket();
        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);
        mNoCheckMessage = true;
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

        mWaiter = new Waiter();

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
                if (speedTestError.equals(error) && mDownload) {
                    if (mNoCheckMessage) {
                        mWaiter.assertEquals(errorMessage, SpeedTestErrorTest.this.errorMessage);
                    }
                    mWaiter.resume();
                } else {
                    mWaiter.fail("error " + error + " expected");
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError.equals(error) && !mDownload && !mForceStop) {
                    if (mNoCheckMessage) {
                        mWaiter.assertEquals(errorMessage, SpeedTestErrorTest.this.errorMessage);
                    }
                    mWaiter.resume();
                } else {
                    mWaiter.fail("error " + error + " expected");
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onInterruption() {

                if (mForceStop) {
                    mWaiter.resume();
                } else {
                    mWaiter.fail("error in onInterruption");
                }
            }
        });

        SpeedTestUtils.setListenerList(mSocket, listenerList);

        return listenerList;
    }

    /**
     * Test connection error for mDownload/upload/forceStopTask.
     */
    private void testErrorHandler(final List<ISpeedTestListener> listenerList, final boolean forceCloseStatus,
                                  final boolean dispatchError)
            throws TimeoutException {

        mForceStop = false;
        mDownload = true;

        if (dispatchError) {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchError(mForceStop, listenerList, mDownload, errorMessage);
        } else {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList, mDownload,
                    errorMessage);
        }
        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        mDownload = false;

        if (dispatchError) {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchError(mForceStop, listenerList, mDownload, errorMessage);
        } else {
            fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList, mDownload,
                    errorMessage);
        }

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        if (forceCloseStatus) {

            mSocket.forceStopTask();
            mForceStop = true;
            mDownload = false;
            mWaiter = new Waiter();

            if (dispatchError) {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchError(mForceStop, listenerList, mDownload, errorMessage);
            } else {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList, mDownload,
                        errorMessage);
            }

            mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

            mDownload = true;

            if (dispatchError) {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchError(mForceStop, listenerList, mDownload, errorMessage);
            } else {
                fr.bmartel.speedtest.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList, mDownload,
                        errorMessage);
            }

            mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        }
    }

    /**
     * Test socket timeout error.
     */
    @Test
    public void socketTimeoutErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        mSocket = new SpeedTestSocket();
        mNoCheckMessage = true;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.SOCKET_TIMEOUT);

        testErrorHandler(listenerList, false, false);
    }

    /**
     * Test http frame error.
     */
    @Test
    public void httpFrameErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        mSocket = new SpeedTestSocket();
        mNoCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        mForceStop = false;
        mDownload = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpFrameError(mForceStop, listenerList, state);
                mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }

        mSocket.forceStopTask();
        mForceStop = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpFrameError(mForceStop, listenerList, state);
                mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Test http header error.
     */
    @Test
    public void httpHeaderErrorTest() throws TimeoutException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        mSocket = new SpeedTestSocket();
        mNoCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        mForceStop = false;
        mDownload = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpHeaderError(mForceStop, listenerList, state);
                mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }

        mSocket.forceStopTask();
        mForceStop = true;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.SpeedTestUtils.checkHttpHeaderError(mForceStop, listenerList, state);
                mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Test http content length error.
     */
    @Test
    public void httpContentLengthErrorTest() throws TimeoutException, NoSuchFieldException, IllegalAccessException {
        mSocket = new SpeedTestSocket();
        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);
        mNoCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        mForceStop = false;
        mDownload = true;

        final HttpFrame frame = new HttpFrame();
        final HashMap<String, String> headers = new HashMap<>();
        headers.put(HttpHeader.CONTENT_LENGTH, String.valueOf(0));
        frame.setHeaders(headers);

        fr.bmartel.speedtest.SpeedTestUtils.checkHttpContentLengthError(mForceStop, listenerList, frame);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        mForceStop = true;
        mSocket.forceStopTask();

        fr.bmartel.speedtest.SpeedTestUtils.checkHttpContentLengthError(mForceStop, listenerList, frame);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Test unknown host error for mDownload.
     */
    @Test
    public void unknownHostDownloadTest() throws TimeoutException {
        unknownHostTest(true);
    }

    /**
     * Test unknown host error for mDownload.
     */
    @Test
    public void unknownHostUploadTest() throws TimeoutException {
        unknownHostTest(false);
    }

    /**
     * Test unknown host for mDownload/upload.
     *
     * @param download define if mDownload or upload is testing.
     */
    private void unknownHostTest(final boolean download) throws TimeoutException {
        mSocket = new SpeedTestSocket();
        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);
        mWaiter = new Waiter();

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {

                if (download) {
                    if (speedTestError != SpeedTestError.CONNECTION_ERROR) {
                        mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    } else {
                        mWaiter.resume();
                    }
                } else {
                    mWaiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onDownloadError");
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {

                if (download) {
                    mWaiter.fail(TestCommon.UPLOAD_ERROR_STR + " : shouldnt be in onUploadError");
                } else {
                    if (speedTestError != SpeedTestError.CONNECTION_ERROR) {
                        mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
                    } else {
                        mWaiter.resume();
                    }
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onInterruption() {

            }
        });

        if (download) {
            mSocket.startDownload(TestCommon.SPEED_TEST_FAKE_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                    .SPEED_TEST_SERVER_URI_DL_1MO);
        } else {
            mSocket.startUpload(TestCommon.SPEED_TEST_FAKE_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                            .SPEED_TEST_SERVER_URI_UL,
                    TestCommon.FILE_SIZE_MEDIUM);
        }

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        mSocket.forceStopTask();

        mSocket.clearListeners();
    }
}
