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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

/**
 * Speed test report object test
 *
 * @author Bertrand Martel
 */
public class SpeedTestReportTest {

    public SpeedTestReportTest() {
    }

    @Test
    public void speedTestReportTest() {

        SpeedTestMode speedTestMode = SpeedTestMode.DOWNLOAD;
        float progressPercent = 98;
        long startTime = new Date().getTime();
        long reportTime = new Date().getTime();
        long temporaryPacketSize = 65535;
        long totalPacketSize = 128000;
        float transferRateOctet = 20000.54f;
        float transferRateBit = 20000.54f / 8;
        int requestNum = 2;

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

        assertTrue(report.getSpeedTestMode() == speedTestMode);
        assertTrue(report.getProgressPercent() == progressPercent);
        assertTrue(report.getStartTime() == startTime);
        assertTrue(report.getReportTime() == reportTime);
        assertTrue(report.getTemporaryPacketSize() == temporaryPacketSize);
        assertTrue(report.getTotalPacketSize() == totalPacketSize);
        assertTrue(report.getTransferRateOctet() == transferRateOctet);
        assertTrue(report.getTransferRateBit() == transferRateBit);
        assertTrue(report.getRequestNum() == requestNum);
    }
}
