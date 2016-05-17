/**
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

import fr.bmartel.speedtest.SpeedTestMode;
import fr.bmartel.speedtest.SpeedTestReport;

import org.junit.Assert;

import org.junit.Test;

import java.util.Date;

/**
 * Speed test report object test.
 *
 * @author Bertrand Martel
 */
public class SpeedTestReportTest {

    /**
     * unit test message header.
     */
    private static final String HEADER = TestUtils.generateMessageHeader(SpeedTestReportTest.class);

    /**
     * tested speed test mode.
     */
    private static final SpeedTestMode speedTestMode = SpeedTestMode.DOWNLOAD;

    /**
     * tested progress.
     */
    private static final float progressPercent = 98;

    /**
     * tested start time.
     */
    private static final long startTime = new Date().getTime();

    /**
     * tested report time.
     */
    private static final long reportTime = new Date().getTime();

    /**
     * tested temporary packet size.
     */
    private static final long temporaryPacketSize = 65535;

    /**
     * tested total packet size.
     */
    private static final long totalPacketSize = 128000;

    /**
     * tested transfer rate o/s.
     */
    private static final float transferRateOctet = 20000.54f;

    /**
     * tested transfer rate b/s.
     */
    private static final float transferRateBit = 20000.54f / 8;

    /**
     * tested request number.
     */
    private static final int requestNum = 2;

    @Test
    public void speedTestReportTest() {

        final SpeedTestReport report = new SpeedTestReport(
                speedTestMode,
                progressPercent,
                startTime,
                reportTime,
                temporaryPacketSize,
                totalPacketSize,
                transferRateOctet,
                transferRateBit,
                requestNum);

        Assert.assertSame(HEADER + "speed test mode are not equals", report.getSpeedTestMode(), speedTestMode);
        Assert.assertEquals(HEADER + "progress are not equals", report.getProgressPercent(), progressPercent, 0);
        Assert.assertEquals(HEADER + "start time are not equals", report.getStartTime(), startTime);
        Assert.assertEquals(HEADER + "report time are not equals", report.getReportTime(), reportTime);
        Assert.assertEquals(HEADER + "temporary packet size are not equals", report.getTemporaryPacketSize(),
                temporaryPacketSize, 0);
        Assert.assertEquals(HEADER + "total packet size are not equals", report.getTotalPacketSize(), totalPacketSize);
        Assert.assertEquals(HEADER + "transfer rate in octet are not equals", report.getTransferRateOctet(),
                transferRateOctet, 0);
        Assert.assertEquals(HEADER + "transfer rate in bit are not equals", report.getTransferRateBit(),
                transferRateBit, 0);
        Assert.assertEquals(HEADER + "request number are not equals", report.getRequestNum(), requestNum);
    }
}
