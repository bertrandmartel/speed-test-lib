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

package fr.bmartel.speedtest.test.utils;

import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.SpeedTestTask;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

/**
 * Utility class for testing SpeedTest.
 *
 * @author Bertrand Martel
 */
public class SpeedTestUtils {

    private static SecureRandom random = new SecureRandom();

    public static String generateFileName() {
        return new BigInteger(130, random).toString(32);
    }

    public static String getFTPUploadUri() {
        return TestCommon.FTP_SERVER_UPLOAD_PREFIX_URI + SpeedTestUtils.generateFileName() + ".txt";
    }

    /**
     * Set listenerList private object in speedTestSocket.
     *
     * @param socket
     * @param listenerList
     */
    public static void setListenerList(final SpeedTestSocket socket, final List<ISpeedTestListener> listenerList)
            throws NoSuchFieldException, IllegalAccessException {

        final Field field = socket.getClass().getDeclaredField("mListenerList");
        Assert.assertNotNull("listenerList is null", field);
        field.setAccessible(true);
        field.set(socket, listenerList);

        final Field fieldTask = socket.getClass().getDeclaredField("mTask");
        Assert.assertNotNull("task is null", fieldTask);
        fieldTask.setAccessible(true);

        final SpeedTestTask task = (SpeedTestTask) fieldTask.get(socket);
        final Field fieldListenerList = task.getClass().getDeclaredField("mListenerList");
        Assert.assertNotNull("listenerList is null", fieldListenerList);
        fieldListenerList.setAccessible(true);
        fieldListenerList.set(task, listenerList);
    }

    /**
     * Check speed test result callback.
     *
     * @param socket
     * @param waiter          Waiter object
     * @param packetSize      packet size received in result callback
     * @param transferRateBps transfer rate in b/s from result callback
     * @param transferRateOps transfer rate in octet/ps from result callback
     */
    public static void checkSpeedTestResult(final SpeedTestSocket socket,
                                            final Waiter waiter,
                                            final long packetSize,
                                            final long packetSizeExpected,
                                            final BigDecimal transferRateBps,
                                            final BigDecimal transferRateOps,
                                            final boolean download,
                                            final boolean
                                                    isRepeat) {
        SpeedTestReport report;

        report = socket.getLiveReport();

        testReportNotEmpty(waiter, report, packetSize, false, isRepeat);

        if (!isRepeat) {
            //percent is 100% for non repeat task. Repeat task are proportional to the window duration which can
            // contains multiple DL/UL results
            waiter.assertTrue(report.getProgressPercent() == 100);
            //total size is the next step in repeat task
            waiter.assertEquals(packetSize, packetSizeExpected);
        }

        waiter.assertNotNull(transferRateBps);
        waiter.assertNotNull(transferRateOps);
        waiter.assertTrue(transferRateBps.longValue() > 0);
        waiter.assertTrue(transferRateBps.longValue() > 0);
        final float check = transferRateOps.multiply(new BigDecimal("8")).floatValue();

        waiter.assertTrue(((transferRateBps.floatValue() + 0.1) >= check) &&
                ((transferRateBps.floatValue() - 0.1) <= check));

        //for repeat task the transfer rate from onDownloadPacketReceived and onUploadPacketReceived is a transfer
        // rate value among the n other transfer rate value calculated during repeat task. getLiveDownload and
        // getLiveUpload give an average of all the values whereas the value got from the callback is the value from
        // the last period of DL/UL
        if (!isRepeat) {
            waiter.assertEquals(report.getTransferRateBit(), transferRateBps);
            waiter.assertEquals(report.getTransferRateOctet(), transferRateOps);
            waiter.assertEquals(report.getTotalPacketSize(), packetSize);
            waiter.assertEquals(report.getTemporaryPacketSize(), packetSize);
        }
    }

    /**
     * Test report not empty.
     *
     * @param waiter                  concurrent thread waiter
     * @param report                  reort to test
     * @param totalPacketSize         total packet size to compare to result
     * @param authorizeTemporaryEmpty define if it is authorize to have temporary packet size/transfer rate to 0 (eg
     * @param isRepeat
     */
    public static void testReportNotEmpty(final Waiter waiter,
                                          final SpeedTestReport report,
                                          final long totalPacketSize,
                                          final boolean authorizeTemporaryEmpty,
                                          final boolean isRepeat) {

        waiter.assertTrue(report.getProgressPercent() > 0);
        waiter.assertTrue(report.getReportTime() != 0);
        waiter.assertTrue(report.getRequestNum() >= 0);
        waiter.assertTrue(report.getStartTime() != 0);
        if (!authorizeTemporaryEmpty) {
            waiter.assertTrue(report.getTemporaryPacketSize() > 0);
            waiter.assertTrue(report.getTransferRateBit().longValue() > 0);
            waiter.assertTrue(report.getTransferRateOctet().longValue() > 0);
        } else {
            //temporary packet size can be 0 if DL/UL has not begun yet
            waiter.assertTrue(report.getTemporaryPacketSize() >= 0);
            waiter.assertTrue(report.getTransferRateBit().longValue() >= 0);
            waiter.assertTrue(report.getTransferRateOctet().longValue() >= 0);
        }
        if (!isRepeat) {
            //for non repeat task the total packet size is the same as the user input. For repeat task it can be >
            // total packet size multiplied by request number
            waiter.assertEquals(report.getTotalPacketSize(), totalPacketSize);
        }

        //check transfer rate O = 8xB
        final float check = report.getTransferRateOctet().multiply(new BigDecimal("8")).floatValue();
        waiter.assertTrue(((report.getTransferRateBit().floatValue() + 0.1) >= check) &&
                ((report.getTransferRateBit().floatValue() - 0.1) <= check));
    }

    /**
     * Compare finish callback report to getLiveDownload or getLiveUpload report.
     *
     * @param socket
     * @param waiter
     * @param packetSize
     * @param report
     * @param download
     */
    public static void compareFinishReport(final SpeedTestSocket socket,
                                           final Waiter waiter,
                                           final long packetSize,
                                           final SpeedTestReport report,
                                           final boolean download) {
        SpeedTestReport liveReport;

        liveReport = socket.getLiveReport();

        SpeedTestUtils.testReportNotEmpty(waiter, report, packetSize, false, true);
        SpeedTestUtils.testReportNotEmpty(waiter, liveReport, packetSize, false, true);

        waiter.assertTrue(report.getProgressPercent() == 100);
        waiter.assertTrue(liveReport.getProgressPercent() == 100);

        //report temporary packet size is not necessary equal to the total FILE_SIZE for repeat
        //waiter.assertEquals(packetSize, report.getTemporaryPacketSize());
        //waiter.assertEquals(packetSize, liveReport.getTemporaryPacketSize());

        /*
        waiter.assertEquals(packetSize * (report.getRequestNum() + 1), report.getTotalPacketSize());
        waiter.assertEquals(packetSize * (report.getRequestNum() + 1), liveReport.getTotalPacketSize());
        */

        waiter.assertNotNull(report.getTransferRateOctet());
        waiter.assertNotNull(report.getTransferRateBit());

        waiter.assertEquals(report.getTransferRateOctet(), liveReport.getTransferRateOctet());
        waiter.assertEquals(report.getTransferRateBit(), liveReport.getTransferRateBit());

        float check = report.getTransferRateOctet().multiply(new BigDecimal("8")).floatValue();

        waiter.assertTrue(((report.getTransferRateBit().floatValue() + 0.1) >= check) &&
                ((report.getTransferRateBit().floatValue() - 0.1) <= check));

        check = liveReport.getTransferRateOctet().multiply(new BigDecimal("8")).floatValue();

        waiter.assertTrue(((liveReport.getTransferRateBit().floatValue() + 0.1) >= check) &&
                ((liveReport.getTransferRateBit().floatValue() - 0.1) <= check));
    }

    /**
     * Test report empty.
     *
     * @param headerMessage test header Message
     * @param report        speed test report object
     * @param isRepeat
     */
    public static void testReportEmpty(final String headerMessage,
                                       final SpeedTestReport report,
                                       final boolean isRepeat) {

        Assert.assertEquals(headerMessage + "progress incorrect", report.getProgressPercent(), 0, 0);
        Assert.assertNotEquals(headerMessage + "time incorrect", report.getReportTime(), 0);
        if (!isRepeat) {
            Assert.assertEquals(headerMessage + "request num incorrect", report.getRequestNum(), 1);
        }
        Assert.assertEquals(headerMessage + "start time incorrect", report.getStartTime(), 0);
        Assert.assertEquals(headerMessage + "temporary packet size incorrect", report
                .getTemporaryPacketSize(), 0);
        Assert.assertEquals(headerMessage + "total packet size incorrect", report
                .getTotalPacketSize(), 0);
        Assert.assertEquals(headerMessage + "transfer rate bps incorrect", report
                .getTransferRateBit().longValue(), 0);
        Assert.assertEquals(headerMessage + "transfer rate ops incorrect", report
                .getTransferRateOctet().longValue(), 0);
    }
}
