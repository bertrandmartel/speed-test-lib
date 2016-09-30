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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * repeat task testing.
 *
 * @author Bertrand Martel
 */
public class SpeedTestRepeatTest {

    /**
     * speed test socket object.
     */
    private SpeedTestSocket socket;

    /**
     * Waiter for speed test listener callback.
     */
    private static Waiter waiter;

    /**
     * Waiter for speed test listener callback error.
     */
    private static Waiter waiterError;

    /**
     * Test download repeat.
     */
    @Test
    public void downloadRepeatTest() throws TimeoutException, IllegalAccessException, NoSuchFieldException {
        repeatTest(true);
    }

    /**
     * Test upload repeat.
     */
    @Test
    public void uploadRepeatTest() throws TimeoutException, IllegalAccessException, NoSuchFieldException {
        repeatTest(false);
    }

    /**
     * Test repeat for DL & UL.
     *
     * @param download define if download or upload is testing.
     */
    private void repeatTest(final boolean download) throws TimeoutException, IllegalAccessException,
            NoSuchFieldException {

        socket = new SpeedTestSocket();

        final RepeatVars repeatVars = new RepeatVars(socket);

        testRepeatVarsNoRepeat(repeatVars);

        waiter = new Waiter();
        waiterError = new Waiter();

        final Waiter finishWaiter = new Waiter();
        final List<ISpeedTestListener> listenerList = new ArrayList<>();

        SpeedTestUtils.setListenerList(socket, listenerList);

        socket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(final SpeedTestReport report) {

                if (!download) {
                    waiterError.fail("shouldnt be in onUploadFinished");
                } else {
                    SpeedTestUtils.checkSpeedTestResult(socket, waiterError, report.getTemporaryPacketSize(),
                            TestCommon.FILE_SIZE_REGULAR, report.getTransferRateBit(),
                            report.getTransferRateOctet(),
                            true, true);
                }
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {
                if (download) {
                    SpeedTestUtils.testReportNotEmpty(waiter, report, TestCommon.FILE_SIZE_REGULAR, false, true);
                    checkRepeatVarsDuringDownload(repeatVars);
                    waiterError.assertTrue(percent >= 0 && percent <= 100);
                    waiter.resume();
                } else {
                    waiterError.fail("shouldnt be in onDownloadProgress");
                }
            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (!download) {
                    waiterError.fail("shouldnt be in onUploadError");
                } else if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiterError.fail("unexpected error : " + speedTestError);
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
                if (download) {
                    waiterError.fail("shouldnt be in onUploadFinished");
                } else {
                    SpeedTestUtils.checkSpeedTestResult(socket, waiterError, report.getTemporaryPacketSize(),
                            TestCommon.FILE_SIZE_REGULAR,
                            report.getTransferRateBit(),
                            report.getTransferRateOctet(),
                            false, true);
                }
            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (download) {
                    waiterError.fail("shouldnt be in onUploadError");
                } else if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
                    waiterError.fail("unexpected error : " + speedTestError);
                }
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {
                if (!download) {
                    SpeedTestUtils.testReportNotEmpty(waiter, report, TestCommon.FILE_SIZE_REGULAR, false, true);
                    checkRepeatVarsDuringUpload(repeatVars);
                    waiterError.assertTrue(percent >= 0 && percent <= 100);
                    waiter.resume();
                } else {
                    waiterError.fail("shouldnt be in onUploadProgress");
                }
            }
        });

        Assert.assertEquals(listenerList.size(), 1);

        if (download) {
            socket.startDownloadRepeat(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT,
                    TestCommon.SPEED_TEST_SERVER_URI_DL_1MO,
                    TestCommon.SPEED_TEST_DURATION, TestCommon.REPORT_INTERVAL, new
                            IRepeatListener() {
                                @Override
                                public void onFinish(final SpeedTestReport report) {
                                    SpeedTestUtils.compareFinishReport(socket, finishWaiter, TestCommon
                                            .FILE_SIZE_REGULAR, report, true);
                                    testRepeatVarsPostResult(finishWaiter, repeatVars, true, TestCommon
                                            .SPEED_TEST_DURATION, report
                                            .getRequestNum());
                                    finishWaiter.resume();
                                }

                                @Override
                                public void onReport(final SpeedTestReport report) {
                                    if (report.getProgressPercent() > 0) {
                                        SpeedTestUtils.testReportNotEmpty(waiterError, report, TestCommon
                                                .FILE_SIZE_REGULAR, true, true);
                                    } else {
                                        SpeedTestUtils.testReportEmpty("empty report", report, true);
                                    }
                                    checkRepeatVarsDuringDownload(repeatVars);
                                    waiterError.resume();
                                }
                            });
            /*
            try {
                Assert.assertEquals(repeatVars.isFirstDownloadRepeat(), true);
            } catch (IllegalAccessException e) {
                Assert.fail(e.getMessage());
            }
            */
            listenerList.get(1).onUploadProgress(0, null);
            listenerList.get(1).onUploadFinished(null);
        } else {
            socket.startUploadRepeat(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                            .SPEED_TEST_SERVER_URI_UL,
                    TestCommon.SPEED_TEST_DURATION, TestCommon.REPORT_INTERVAL, TestCommon.FILE_SIZE_REGULAR, new
                            IRepeatListener() {
                                @Override
                                public void onFinish(final SpeedTestReport report) {
                                    SpeedTestUtils.compareFinishReport(socket, finishWaiter, TestCommon
                                                    .FILE_SIZE_REGULAR,
                                            report, false);
                                    testRepeatVarsPostResult(finishWaiter, repeatVars, false, TestCommon
                                                    .SPEED_TEST_DURATION,
                                            report
                                                    .getRequestNum());
                                    finishWaiter.resume();
                                }

                                @Override
                                public void onReport(final SpeedTestReport report) {
                                    if (report.getProgressPercent() > 0) {
                                        SpeedTestUtils.testReportNotEmpty(waiterError, report, TestCommon
                                                .FILE_SIZE_REGULAR, true, true);
                                    } else {
                                        SpeedTestUtils.testReportEmpty("empty report", report, true);
                                    }
                                    checkRepeatVarsDuringUpload(repeatVars);
                                    waiterError.resume();
                                }
                            });
            Assert.assertEquals(repeatVars.isFirstUploadRepeat(), true);
            listenerList.get(1).onDownloadProgress(0, null);
            listenerList.get(1).onDownloadFinished(null);
        }

        Assert.assertEquals(listenerList.size(), 2);

        Assert.assertEquals(repeatVars.getRepeatWindows(), TestCommon.SPEED_TEST_DURATION);

        waiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        waiterError.await(TestCommon.WAITING_TIMEOUT_LONG_OPERATION, TimeUnit.SECONDS, TestCommon.EXPECTED_REPORT);
        finishWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);

        socket.forceStopTask();
    }

    /**
     * Check repeat vars values during upload.
     *
     * @param repeatVars
     */
    private void checkRepeatVarsDuringUpload(final RepeatVars repeatVars) {
        try {
            waiter.assertEquals(repeatVars.isRepeatUpload(), true);
            waiter.assertEquals(repeatVars.isRepeatDownload(), false);
            waiter.assertEquals(repeatVars.isRepeatFinished(), false);
            waiter.assertTrue(repeatVars.getStartDateRepeat() > 0);
            //waiter.assertEquals(repeatVars.getRepeatTransferRateList().size(), report.getRequestNum());
        } catch (IllegalAccessException e) {
            waiter.fail(e.getMessage());
        }
    }

    /**
     * Check repeat vars values during download.
     *
     * @param repeatVars
     */
    private void checkRepeatVarsDuringDownload(final RepeatVars repeatVars) {
        try {
            waiter.assertEquals(repeatVars.isRepeatUpload(), false);
            waiter.assertEquals(repeatVars.isRepeatDownload(), true);
            waiter.assertEquals(repeatVars.isRepeatFinished(), false);
            //waiter.assertTrue(repeatVars.getStartDateRepeat() > 0);
            //depending being called before or after onProgress of each listener it can be requestNum or requestNum+1
            //waiter.assertEquals(repeatVars.getRepeatTransferRateList().size(), report.getRequestNum());
        } catch (IllegalAccessException e) {
            waiter.fail(e.getMessage());
        }
    }

    /**
     * Test repeat vars before the repeat task.
     *
     * @param repeatVars
     */
    private void testRepeatVarsInit(final RepeatVars repeatVars) {

        try {
            Assert.assertEquals(repeatVars.getStartDateRepeat(), 0);
            Assert.assertEquals(repeatVars.getRepeatWindows(), 0);
            Assert.assertEquals(repeatVars.getRepeatPacketSize().intValue(), 0);
            Assert.assertEquals(repeatVars.getRepeatTempPckSize(), 0);
            Assert.assertNotNull(repeatVars.getRepeatTransferRateList());
            Assert.assertEquals(repeatVars.getRepeatTransferRateList().size(), 0);
            Assert.assertEquals(repeatVars.isRepeatFinished(), false);
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test repeat vars when no call to init has been done.
     *
     * @param repeatVars
     */
    private void testRepeatVarsNoRepeat(final RepeatVars repeatVars) throws IllegalAccessException {

        Assert.assertEquals(repeatVars.isRepeatDownload(), false);
        Assert.assertEquals(repeatVars.isRepeatUpload(), false);
        Assert.assertEquals(repeatVars.isFirstDownloadRepeat(), false);
        Assert.assertEquals(repeatVars.isFirstUploadRepeat(), false);

        testRepeatVarsInit(repeatVars);
    }

    /**
     * Test repeat vars after the repeat task.
     *
     * @param repeatVars
     */
    private void testRepeatVarsPostResult(final Waiter waiter, final RepeatVars repeatVars, final boolean download,
                                          final int repeatWindow, final int requestNum) {
        try {
            waiter.assertEquals(repeatVars.isRepeatDownload(), download);
            waiter.assertEquals(repeatVars.isRepeatUpload(), !download);
            waiter.assertTrue(repeatVars.getStartDateRepeat() != 0);
            waiter.assertEquals(repeatVars.getRepeatWindows(), repeatWindow);
            waiter.assertTrue(repeatVars.getRepeatPacketSize().intValue() != 0);
            waiter.assertTrue(repeatVars.getRepeatTempPckSize() != 0);
            waiter.assertNotNull(repeatVars.getRepeatTransferRateList());
            waiter.assertEquals(repeatVars.getRepeatTransferRateList().size(), requestNum);
            waiter.assertEquals(repeatVars.isRepeatFinished(), true);
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test download repeat.
     */
    @Test
    public void initRepeatTest() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        socket = new SpeedTestSocket();

        final RepeatVars repeatVars = new RepeatVars(socket);

        testRepeatVarsNoRepeat(repeatVars);

        testInitRepeat(repeatVars, true);
        testInitRepeat(repeatVars, false);
    }

    /**
     * Test repeat initialization function.
     *
     * @param repeatVars
     * @param download
     */
    private void testInitRepeat(final RepeatVars repeatVars, final boolean download) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Class[] cArg = new Class[1];
        cArg[0] = boolean.class;

        final Method method = socket.getClass().getDeclaredMethod("initRepeat", cArg);

        method.setAccessible(true);
        Assert.assertNotNull(method);

        method.invoke(socket, download);

        Assert.assertEquals(repeatVars.isRepeatDownload(), download);
        Assert.assertEquals(repeatVars.isRepeatUpload(), !download);
        Assert.assertEquals(repeatVars.isFirstDownloadRepeat(), download);
        Assert.assertEquals(repeatVars.isFirstUploadRepeat(), !download);

        testRepeatVarsInit(repeatVars);

    }

    @Test
    public void clearRepeatTaskTest() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, NoSuchFieldException {

        socket = new SpeedTestSocket();

        final List<ISpeedTestListener> listenerList = new ArrayList<>();

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

        final RepeatVars repeatVars = new RepeatVars(socket);

        Assert.assertEquals(listenerList.size(), 1);

        final Method method;

        Class[] cArg = new Class[2];
        cArg[0] = ISpeedTestListener.class;
        cArg[1] = Timer.class;

        method = socket.getClass().getDeclaredMethod("clearRepeatTask", cArg);

        method.setAccessible(true);
        Assert.assertNotNull(method);

        method.invoke(socket, listener, new Timer());

        Assert.assertEquals(repeatVars.isRepeatFinished(), true);
        Assert.assertEquals(listenerList.size(), 0);
    }

    /*
    @Test
    public void repeatThreadCounterTest() throws InterruptedException, NoSuchFieldException, IllegalAccessException {

        socket = new SpeedTestSocket();

        testClearRepeatThreadCount(true);

        socket = new SpeedTestSocket();

        testClearRepeatThreadCount(false);
    }

    private void testClearRepeatThreadCount(final boolean download) throws InterruptedException,
            NoSuchFieldException, IllegalAccessException {

        int threadCount = Thread.activeCount();

        int threadOffset = 4;

        if (download) {
            threadOffset = 2;
        }

        final List<ISpeedTestListener> listenerList = new ArrayList<>();
        SpeedTestUtils.setListenerList(socket, listenerList);

        Assert.assertEquals(listenerList.size(), 0);

        startRepeatTask(download);

        Assert.assertEquals(listenerList.size(), 1);

        Thread.sleep(TestCommon.WAIT_THREAD_TIMEOUT);

        Assert.assertEquals(threadCount + threadOffset, Thread.activeCount());
        socket.forceStopTask();

        Thread.sleep(TestCommon.WAIT_THREAD_TIMEOUT);

        Assert.assertEquals(threadCount, Thread.activeCount());
        Assert.assertEquals(listenerList.size(), 0);

        threadCount = Thread.activeCount();

        startRepeatTask(download);

        Assert.assertEquals(listenerList.size(), 1);

        Thread.sleep(TestCommon.WAIT_THREAD_TIMEOUT);

        Assert.assertEquals(threadCount + threadOffset, Thread.activeCount());

        listenerList.get(0).onDownloadError(SpeedTestError.SOCKET_ERROR, "some error");

        Thread.sleep(TestCommon.WAIT_THREAD_TIMEOUT);

        Assert.assertEquals(threadCount, Thread.activeCount());
        Assert.assertEquals(listenerList.size(), 0);

        threadCount = Thread.activeCount();

        startRepeatTask(download);

        Assert.assertEquals(listenerList.size(), 1);

        Thread.sleep(TestCommon.WAIT_THREAD_TIMEOUT);

        Assert.assertEquals(threadCount + threadOffset, Thread.activeCount());

        listenerList.get(0).onUploadError(SpeedTestError.SOCKET_ERROR, "some error");

        Thread.sleep(TestCommon.WAIT_THREAD_TIMEOUT);

        Assert.assertEquals(threadCount, Thread.activeCount());
        Assert.assertEquals(listenerList.size(), 0);

    }

    private void startRepeatTask(final boolean download) {

        if (download) {
            socket.startDownloadRepeat(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT,
                    TestCommon.SPEED_TEST_SERVER_URI_DL_1MO,
                    TestCommon.SPEED_TEST_DURATION, TestCommon.REPORT_INTERVAL, new
                            IRepeatListener() {
                                @Override
                                public void onFinish(final SpeedTestReport report) {
                                }

                                @Override
                                public void onReport(final SpeedTestReport report) {
                                }
                            });
        } else {
            socket.startUploadRepeat(TestCommon.SPEED_TEST_SERVER_HOST, TestCommon.SPEED_TEST_SERVER_PORT, TestCommon
                            .SPEED_TEST_SERVER_URI_UL,
                    TestCommon.SPEED_TEST_DURATION, TestCommon.REPORT_INTERVAL, TestCommon.FILE_SIZE_REGULAR, new
                            IRepeatListener() {
                                @Override
                                public void onFinish(final SpeedTestReport report) {
                                }

                                @Override
                                public void onReport(final SpeedTestReport report) {
                                }
                            });
        }
    }
      */
}
