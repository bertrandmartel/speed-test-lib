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

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.constants.HttpHeader;
import fr.bmartel.protocol.http.states.HttpStates;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.test.model.ConnectionError;
import fr.bmartel.speedtest.test.utils.SpeedTestUtils;
import fr.bmartel.speedtest.test.utils.TestCommon;
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
public class SpeedTestErrorTest extends AbstractTest {

    /**
     * error message for testing error callback.
     */
    private static String errorMessage = "this is an error message";

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
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
                if (mForceStop) {
                    mWaiter.resume();
                }
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                //called to notify download progresss
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                if (speedTestError.equals(error)) {
                    if (mNoCheckMessage) {
                        mWaiter.assertEquals(errorMessage, SpeedTestErrorTest.this.errorMessage);
                    }
                    mWaiter.resume();
                } else {
                    mWaiter.fail("error " + error + " expected");
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

        if (dispatchError) {
            fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchError(mSocket, mForceStop, listenerList, errorMessage);
        } else {
            fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList,
                    errorMessage);
        }
        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        if (dispatchError) {
            fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchError(mSocket, mForceStop, listenerList, errorMessage);
        } else {
            fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList,
                    errorMessage);
        }

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        if (forceCloseStatus) {

            mSocket.forceStopTask();
            mForceStop = true;
            mWaiter = new Waiter();

            if (dispatchError) {
                fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchError(mSocket, mForceStop, listenerList,
                        errorMessage);
            } else {
                fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList,
                        errorMessage);
            }

            mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

            if (dispatchError) {
                fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchError(mSocket, mForceStop, listenerList,
                        errorMessage);
            } else {
                fr.bmartel.speedtest.utils.SpeedTestUtils.dispatchSocketTimeout(mForceStop, listenerList,
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

        mNoCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        mForceStop = false;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.utils.SpeedTestUtils.checkHttpFrameError(mForceStop, listenerList, state);
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

        mNoCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        mForceStop = false;

        for (final HttpStates state : HttpStates.values()) {
            if (state != HttpStates.HTTP_FRAME_OK) {
                fr.bmartel.speedtest.utils.SpeedTestUtils.checkHttpHeaderError(mForceStop, listenerList, state);
                mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Test http content length error.
     */
    @Test
    public void httpContentLengthErrorTest() throws TimeoutException, NoSuchFieldException, IllegalAccessException {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);
        mNoCheckMessage = false;
        final List<ISpeedTestListener> listenerList = initErrorListener(SpeedTestError.INVALID_HTTP_RESPONSE);

        mForceStop = false;

        final HttpFrame frame = new HttpFrame();
        final HashMap<String, String> headers = new HashMap<>();
        headers.put(HttpHeader.CONTENT_LENGTH, String.valueOf(0));
        frame.setHeaders(headers);

        fr.bmartel.speedtest.utils.SpeedTestUtils.checkHttpContentLengthError(mForceStop, listenerList, frame);

        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Test unknown host error for HTTP download.
     */
    @Test
    public void unknownHostHTTPDownloadTest() throws TimeoutException {
        connectionErrorTest(true, ConnectionError.HTTP_UNKNOWN_HOST);
    }

    /**
     * Test unknown host error for HTTP upload.
     */
    @Test
    public void unknownHostHTTPUploadTest() throws TimeoutException {
        connectionErrorTest(false, ConnectionError.HTTP_UNKNOWN_HOST);
    }

    /**
     * Test unknown host error for FTP download.
     */
    @Test
    public void unknownHostFTPDownloadTest() throws TimeoutException {
        connectionErrorTest(true, ConnectionError.FTP_UNKNOWN_HOST);
    }

    /**
     * Test unknown host error for FTP upload.
     */
    @Test
    public void unknownHostFTPUploadTest() throws TimeoutException {
        connectionErrorTest(false, ConnectionError.FTP_UNKNOWN_HOST);
    }

    @Test
    public void downloadBadStatusCodeTest() throws TimeoutException {
        connectionErrorTest(true, ConnectionError.BAD_STATUS);
    }

    @Test
    public void uploadBadStatusCodeTest() throws TimeoutException {
        connectionErrorTest(false, ConnectionError.BAD_STATUS);
    }

    @Test
    public void downloadFTPBadUri() throws TimeoutException {
        connectionErrorTest(true, ConnectionError.FTP_BAD_URI);
    }

    @Test
    public void uploadFTPBadUri() throws TimeoutException {
        connectionErrorTest(false, ConnectionError.FTP_BAD_URI);
    }

    /**
     * Test unknown host for mDownload/upload.
     *
     * @param download define if mDownload or upload is testing.
     */
    private void connectionErrorTest(final boolean download, final ConnectionError error) throws TimeoutException {

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
                dispatchConnectionError(error, speedTestError);
            }
        });

        switch (error) {
            case HTTP_UNKNOWN_HOST:
                if (download) {
                    mSocket.startDownload("http://" + TestCommon.SPEED_TEST_FAKE_HOST + TestCommon
                            .SPEED_TEST_SERVER_URI_DL_1MO);
                } else {
                    mSocket.startUpload("http://" + TestCommon.SPEED_TEST_FAKE_HOST + TestCommon
                                    .SPEED_TEST_SERVER_URI_UL,
                            TestCommon.FILE_SIZE_MEDIUM);
                }

                mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
                break;
            case BAD_STATUS:
                if (download) {
                    mSocket.startDownload(TestCommon.SPEED_TEST_SERVER_404_URI);
                } else {
                    mSocket.startUpload(TestCommon.SPEED_TEST_SERVER_404_URI,
                            TestCommon.FILE_SIZE_REGULAR);
                }
                mWaiter.await(TestCommon.WAITING_TIMEOUT_LONG_OPERATION, TimeUnit.SECONDS);
                break;
            case FTP_UNKNOWN_HOST:
                if (download) {
                    mSocket.startDownload("http://" + TestCommon.SPEED_TEST_FAKE_HOST + TestCommon.FTP_SERVER_URI);
                } else {
                    mSocket.startUpload("http://" + TestCommon.SPEED_TEST_FAKE_HOST + TestCommon
                                    .FTP_SERVER_UPLOAD_PREFIX_URI +
                                    "something.txt",
                            TestCommon.FILE_SIZE_REGULAR);
                }
                mWaiter.await(TestCommon.WAITING_TIMEOUT_LONG_OPERATION, TimeUnit.SECONDS);
                break;
            case FTP_BAD_URI:
                if (download) {
                    mSocket.startDownload("ftp://" + TestCommon.FTP_SERVER_HOST + TestCommon.FTP_FAKE_URI);
                } else {
                    mSocket.startUpload("ftp://" + TestCommon.FTP_SERVER_HOST + TestCommon.FTP_FAKE_URI,
                            TestCommon.FILE_SIZE_REGULAR);
                }
                mWaiter.await(TestCommon.WAITING_TIMEOUT_LONG_OPERATION, TimeUnit.SECONDS);
                break;
            default:
                break;
        }

        mSocket.forceStopTask();

        mSocket.clearListeners();
    }

    private void dispatchConnectionError(final ConnectionError error, final SpeedTestError speedTestError) {

        if (error == ConnectionError.HTTP_UNKNOWN_HOST && speedTestError != SpeedTestError.CONNECTION_ERROR) {
            mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
        } else if (error == ConnectionError.FTP_UNKNOWN_HOST && speedTestError != SpeedTestError.CONNECTION_ERROR) {
            mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
        } else if (error == ConnectionError.BAD_STATUS && speedTestError != SpeedTestError.INVALID_HTTP_RESPONSE) {
            mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
        } else if (error == ConnectionError.FTP_BAD_URI && speedTestError != SpeedTestError.CONNECTION_ERROR) {
            mWaiter.fail(TestCommon.DOWNLOAD_ERROR_STR + speedTestError);
        } else {
            mWaiter.resume();
        }
    }
}
