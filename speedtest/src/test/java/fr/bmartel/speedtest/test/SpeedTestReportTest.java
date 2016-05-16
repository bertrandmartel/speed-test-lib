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

import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Date;

/**
 * Speed test report object test
 *
 * @author Bertrand Martel
 */
public class SpeedTestReportTest {

    private final static String HEADER = TestUtils.generateMessageHeader(SpeedTestReportTest.class);

    @Test
    public void speedTestReportTest() {

        final SpeedTestMode speedTestMode = SpeedTestMode.DOWNLOAD;
        final float progressPercent = 98;
        final long startTime = new Date().getTime();
        final long reportTime = new Date().getTime();
        final long temporaryPacketSize = 65535;
        final long totalPacketSize = 128000;
        final float transferRateOctet = 20000.54f;
        final float transferRateBit = 20000.54f / 8;
        final int requestNum = 2;

        SpeedTestReport report = new SpeedTestReport(
                speedTestMode,
                progressPercent,
                startTime,
                reportTime,
                temporaryPacketSize,
                totalPacketSize,
                transferRateOctet,
                transferRateBit,
                requestNum);

        assertSame(HEADER + "speed test mode are not equals", report.getSpeedTestMode(), speedTestMode);
        assertSame(HEADER + "progress are not equals", report.getProgressPercent(), progressPercent);
        assertSame(HEADER + "start time are not equals", report.getStartTime(), startTime);
        assertSame(HEADER + "report time are not equals", report.getReportTime(), reportTime);
        assertSame(HEADER + "temporary packet size are not equals", report.getTemporaryPacketSize(), temporaryPacketSize);
        assertSame(HEADER + "total packet size are not equals", report.getTotalPacketSize(), totalPacketSize);
        assertSame(HEADER + "transfer rate in octet are not equals", report.getTransferRateOctet(), transferRateOctet);
        assertSame(HEADER + "transfer rate in bit are not equals", report.getTransferRateBit(), transferRateBit);
        assertSame(HEADER + "request number are not equals", report.getRequestNum(), requestNum);
    }
}
