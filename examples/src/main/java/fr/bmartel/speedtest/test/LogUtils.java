package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.SpeedTestMode;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Created by akinaru on 18/05/16.
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
     * print speed test report object.
     *
     * @param report speed test report to log
     */
    public static void logSpeedTestReport(final SpeedTestReport report, Logger log) {

        if (log.isEnabled(Level.DEBUG)) {

            switch (report.getSpeedTestMode()) {
                case DOWNLOAD:
                    log.debug("--------------current download report--------------------");
                    break;
                case UPLOAD:
                    log.debug("---------------current upload report--------------------");
                    break;
                default:
                    break;
            }

            log.debug("progress             : " + report.getProgressPercent() + "%");
            log.debug("transfer rate bit    : " + report.getTransferRateBit() + "b/s");
            log.debug("transfer rate octet  : " + report.getTransferRateOctet() + "B/s");
            log.debug("uploaded for now     : " + report.getTemporaryPacketSize() + "/" + report.getTotalPacketSize());

            if (report.getStartTime() > 0) {
                log.debug("amount of time       : " + ((report.getReportTime() - report.getStartTime()) /
                        VALUE_PER_SECONDS) + "s");
            }
            log.debug("request number       : " + report.getRequestNum());

            log.debug("--------------------------------------------------------");
        }
    }

    /**
     * print upload/download result.
     *
     * @param mode                        speed test mode
     * @param packetSize                  packet size received
     * @param transferRateBitPerSeconds   transfer rate in bps
     * @param transferRateOctetPerSeconds transfer rate in Bps
     */
    public static void logFinishedTask(final SpeedTestMode mode, final long packetSize, final float
            transferRateBitPerSeconds, final float transferRateOctetPerSeconds, Logger log) {

        if (log.isEnabled(Level.ERROR)) {
            switch (mode) {
                case DOWNLOAD:
                    log.debug("======== Download [ OK ] =============");
                    break;
                case UPLOAD:
                    log.debug("========= Upload [ OK ]  =============");
                    break;
                default:
                    break;
            }

            log.debug("upload packetSize     : " + packetSize + " octet(s)");
            log.debug("upload transfer rate  : " + transferRateBitPerSeconds + " bit/second   | " +
                    transferRateBitPerSeconds / VALUE_PER_SECONDS
                    + " Kbit/second  | " + transferRateBitPerSeconds / MEGA_VALUE_PER_SECONDS + " Mbit/second");
            log.debug("upload transfer rate  : " + transferRateOctetPerSeconds + " octet/second | " +
                    transferRateOctetPerSeconds / VALUE_PER_SECONDS
                    + " Koctet/second | " + +transferRateOctetPerSeconds / MEGA_VALUE_PER_SECONDS + " Moctet/second");
            log.debug("##################################################################");
        }
    }

    /**
     * @param speedTestSocket
     * @param log
     */
    public static void logReport(SpeedTestSocket speedTestSocket, Logger log) {

        if (log.isEnabled(Level.DEBUG)) {

            SpeedTestReport report = null;

            if (speedTestSocket.getSpeedTestMode() == SpeedTestMode.UPLOAD) {
                report = speedTestSocket.getLiveUploadReport();
                log.debug("---------------current upload report--------------------");
            } else if (speedTestSocket.getSpeedTestMode() == SpeedTestMode.DOWNLOAD) {
                report = speedTestSocket.getLiveDownloadReport();
                log.debug("---------------current download report--------------------");
            }

            if (report != null) {

                log.debug("progress             : " + report.getProgressPercent() + "%");
                log.debug("transfer rate bit    : " + report.getTransferRateBit() + "b/s");
                log.debug("transfer rate octet  : " + report.getTransferRateOctet() + "B/s");
                log.debug("uploaded for now     : " + report.getTemporaryPacketSize()
                        + "/" + report.getTotalPacketSize());

                if (report.getStartTime() > 0) {
                    log.debug("amount of time       : " +
                            ((report.getReportTime() - report.getStartTime()) / 1000) + "s");
                }
                log.debug("--------------------------------------------------------");
            }
        }
    }
}
