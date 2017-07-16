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

import fr.bmartel.speedtest.model.SpeedTestMode;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.test.utils.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Speed Test report testing.
 *
 * @author Bertrand Martel
 */
public class SpeedTestReportTest {

    /**
     * unit examples message header.
     */
    private static final String HEADER = TestUtils.generateMessageHeader(SpeedTestReportTest.class);

    /**
     * tested speed examples mode.
     */
    private static final SpeedTestMode SPEED_TEST_MODE = SpeedTestMode.DOWNLOAD;

    /**
     * tested progress.
     */
    private static final float PROGRESS = 98;

    /**
     * tested start time.
     */
    private static final long START_TIME = new Date().getTime();

    /**
     * tested report time.
     */
    private static final long REPORT_TIME = new Date().getTime();

    /**
     * tested temporary packet size.
     */
    private static final long TEMPORARY_PACKET_SIZE = 65535;

    /**
     * tested total packet size.
     */
    private static final long TOTAL_PACKET_SIZE = 128000;

    /**
     * tested transfer rate o/s.
     */
    private static final BigDecimal TRANSFER_RATE_OCT = new BigDecimal("20000.54");

    /**
     * tested transfer rate b/s.
     */
    private static final BigDecimal TRANSFER_RATE_BIT = TRANSFER_RATE_OCT.divide(new BigDecimal("8"));

    /**
     * tested request number.
     */
    private static final int REQUEST_NUM = 2;

    /**
     * test speed test report object.
     */
    @Test
    public void speedTestReportTest() {

        final SpeedTestReport report = new SpeedTestReport(
                SPEED_TEST_MODE,
                PROGRESS,
                START_TIME,
                REPORT_TIME,
                TEMPORARY_PACKET_SIZE,
                TOTAL_PACKET_SIZE,
                TRANSFER_RATE_OCT,
                TRANSFER_RATE_BIT,
                REQUEST_NUM);

        Assert.assertSame(HEADER + "speed examples mode are not equals", report.getSpeedTestMode(), SPEED_TEST_MODE);
        Assert.assertEquals(HEADER + "progress are not equals", report.getProgressPercent(), PROGRESS, 0);
        Assert.assertEquals(HEADER + "start time are not equals", report.getStartTime(), START_TIME);
        Assert.assertEquals(HEADER + "report time are not equals", report.getReportTime(), REPORT_TIME);
        Assert.assertEquals(HEADER + "temporary packet size are not equals", report.getTemporaryPacketSize(),
                TEMPORARY_PACKET_SIZE, 0);
        Assert.assertEquals(HEADER + "total packet size are not equals", report.getTotalPacketSize(),
                TOTAL_PACKET_SIZE);
        Assert.assertEquals(HEADER + "transfer rate in octet are not equals", report.getTransferRateOctet(),
                TRANSFER_RATE_OCT);
        Assert.assertEquals(HEADER + "transfer rate in bit are not equals", report.getTransferRateBit(),
                TRANSFER_RATE_BIT);
        Assert.assertEquals(HEADER + "request number are not equals", report.getRequestNum(), REQUEST_NUM);
    }
}
