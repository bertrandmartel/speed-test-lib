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
package fr.bmartel.speedtest.examples;

import fr.bmartel.speedtest.SpeedTestMode;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Log utilities method for examples.
 *
 * @author Bertrand Martel
 */
public class LogUtils {

    /**
     * conversion const for per second value.
     */
    private static final int VALUE_PER_SECONDS = 1000;

    /**
     * conversion const for M per second value.
     */
    private static final int MEGA_VALUE_PER_SECONDS = 1000000;

    /**
     * print speed examples report object.
     *
     * @param report speed examples report to log
     * @param logger log4j logger
     */
    public static void logSpeedTestReport(final SpeedTestReport report, Logger logger) {

        if (logger.isEnabled(Level.DEBUG)) {

            switch (report.getSpeedTestMode()) {
                case DOWNLOAD:
                    logger.debug("--------------current download report--------------------");
                    break;
                case UPLOAD:
                    logger.debug("---------------current upload report--------------------");
                    break;
                default:
                    break;
            }

            logger.debug("progress             : " + report.getProgressPercent() + "%");
            logger.debug("transfer rate bit    : " + report.getTransferRateBit() + "b/s");
            logger.debug("transfer rate octet  : " + report.getTransferRateOctet() + "B/s");
            logger.debug("uploaded for now     : " + report.getTemporaryPacketSize() + "/" + report
                    .getTotalPacketSize());

            if (report.getStartTime() > 0) {
                logger.debug("amount of time       : " + ((report.getReportTime() - report.getStartTime()) /
                        VALUE_PER_SECONDS) + "s");
            }
            logger.debug("request number       : " + report.getRequestNum());

            logger.debug("--------------------------------------------------------");
        }
    }

    /**
     * print upload/download result.
     *
     * @param mode                        speed examples mode
     * @param packetSize                  packet size received
     * @param transferRateBitPerSeconds   transfer rate in bps
     * @param transferRateOctetPerSeconds transfer rate in Bps
     * @param logger                      log4j logger
     */
    public static void logFinishedTask(final SpeedTestMode mode, final long packetSize, final float
            transferRateBitPerSeconds, final float transferRateOctetPerSeconds, Logger logger) {

        if (logger.isEnabled(Level.ERROR)) {
            switch (mode) {
                case DOWNLOAD:
                    logger.debug("======== Download [ OK ] =============");
                    break;
                case UPLOAD:
                    logger.debug("========= Upload [ OK ]  =============");
                    break;
                default:
                    break;
            }

            logger.debug("upload packetSize     : " + packetSize + " octet(s)");
            logger.debug("upload transfer rate  : " + transferRateBitPerSeconds + " bit/second   | " +
                    transferRateBitPerSeconds / VALUE_PER_SECONDS
                    + " Kbit/second  | " + transferRateBitPerSeconds / MEGA_VALUE_PER_SECONDS + " Mbit/second");
            logger.debug("upload transfer rate  : " + transferRateOctetPerSeconds + " octet/second | " +
                    transferRateOctetPerSeconds / VALUE_PER_SECONDS
                    + " Koctet/second | " + +transferRateOctetPerSeconds / MEGA_VALUE_PER_SECONDS + " Moctet/second");
            logger.debug("##################################################################");
        }
    }

    /**
     * print report from speed test socket object.
     *
     * @param speedTestSocket speed test socket instance
     * @param logger          log4j logger
     */
    public static void logReport(SpeedTestSocket speedTestSocket, Logger logger) {

        if (logger.isEnabled(Level.DEBUG)) {

            SpeedTestReport report = null;

            if (speedTestSocket.getSpeedTestMode() == SpeedTestMode.UPLOAD) {
                report = speedTestSocket.getLiveUploadReport();
                logger.debug("---------------current upload report--------------------");
            } else if (speedTestSocket.getSpeedTestMode() == SpeedTestMode.DOWNLOAD) {
                report = speedTestSocket.getLiveDownloadReport();
                logger.debug("---------------current download report--------------------");
            }

            if (report != null) {
                logReport(report, logger);
            }
        }
    }

    /**
     * Print log report from speed test report object.
     *
     * @param report speed test report instance
     * @param logger log4j logger
     */
    public static void logReport(SpeedTestReport report, Logger logger) {
        logger.debug("progress             : " + report.getProgressPercent() + "%");
        logger.debug("transfer rate bit    : " + report.getTransferRateBit() + "b/s");
        logger.debug("transfer rate octet  : " + report.getTransferRateOctet() + "B/s");
        logger.debug("uploaded for now     : " + report.getTemporaryPacketSize()
                + "/" + report.getTotalPacketSize());

        if (report.getStartTime() > 0) {
            logger.debug("amount of time       : " +
                    ((report.getReportTime() - report.getStartTime()) / 1000) + "s");
        }
        logger.debug("--------------------------------------------------------");
    }
}
