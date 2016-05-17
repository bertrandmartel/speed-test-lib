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

import fr.bmartel.speedtest.*;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Speed Test example.
 * <p/>
 * <ul>
 * <li>Download test with progress bar and output</li>
 * <li>Upload test with progress bar and output</li>
 * </ul>
 *
 * @author Bertrand Martel
 */
public class SpeedTest {

    /**
     * check download bar initialization.
     */
    private static boolean initDownloadBar;

    /**
     * check upload bar initialization.
     */
    private static boolean initUploadBar;

    /**
     * socket timeout used in ms.
     */
    private final static int SOCKET_TIMEOUT = 5000;

    /**
     * speed test server host name.
     */
    private final static String SPEED_TEST_SERVER_HOST = "1.testdebit.info";

    /**
     * spedd test server uri.
     */
    private final static String SPEED_TEST_SERVER_URI = "/fichiers/10Mo.dat";

    /**
     * speed test server port.
     */
    private final static int SPEED_TEST_SERVER_PORT = 80;

    /**
     * conversion const for per second value.
     */
    private final static int VALUE_PER_SECONDS = 1000;

    /**
     * conversion const for M per second value.
     */
    private final static int MEGA_VALUE_PER_SECONDS = 1000000;

    /**
     * logger.
     */
    private final static Logger log = Logger.getLogger(SpeedTest.class.getName());

    /**
     * log handler.
     */
    private final static ConsoleHandler handler = new ConsoleHandler();

    /**
     * log formatter.
     */
    private final static Formatter logFormatter = new SingleLineFormatter();

    /**
     * Instanciate Speed Test and start download and upload process with speed
     * test server of your choice.
     *
     * @param args no args required
     */
    public static void main(final String[] args) {

        //configure logger for quickTest
        setupLogger();

        // instantiate speed test
        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        //configure log
        speedTestSocket.configureLog(Level.SEVERE, handler, logFormatter);

        //set timeout for download
        speedTestSocket.setSocketTimeout(SOCKET_TIMEOUT);

        // add a listener to wait for speed test completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadPacketsReceived(final long packetSize, final float transferRateBitPerSeconds, final
            float transferRateOctetPerSeconds) {

                logFinishedTask(SpeedTestMode.DOWNLOAD, packetSize, transferRateBitPerSeconds,
                        transferRateOctetPerSeconds);

            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (log.isLoggable(Level.FINE)) {
                    log.severe("Download error " + speedTestError + " : " + errorMessage);
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final float transferRateBitPerSeconds, final
            float transferRateOctetPerSeconds) {

                logFinishedTask(SpeedTestMode.UPLOAD, packetSize, transferRateBitPerSeconds,
                        transferRateOctetPerSeconds);

            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (log.isLoggable(Level.FINE)) {
                    log.severe("Upload error " + speedTestError + " : " + errorMessage);
                }
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport downloadReport) {

                logSpeedTestReport(downloadReport);
                updateDownloadProgressBar(percent);
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport uploadReport) {

                logSpeedTestReport(uploadReport);
                updateUploadProgressBar(percent);
            }
        });

        speedTestSocket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI);
    }

    /**
     * setup logger.
     */
    private static void setupLogger() {
        log.setLevel(Level.ALL);
        handler.setLevel(Level.ALL);
        handler.setFormatter(logFormatter);
        log.addHandler(handler);
    }

    /**
     * print speed test report object.
     *
     * @param report speed test report to log
     */
    private static void logSpeedTestReport(final SpeedTestReport report) {

        if (log.isLoggable(Level.FINE)) {

            switch (report.getSpeedTestMode()) {
                case DOWNLOAD:
                    log.fine("--------------current download report--------------------");
                    break;
                case UPLOAD:
                    log.fine("---------------current upload report--------------------");
                    break;
                default:
                    break;
            }

            log.fine("progress             : " + report.getProgressPercent() + "%");
            log.fine("transfer rate bit    : " + report.getTransferRateBit() + "b/s");
            log.fine("transfer rate octet  : " + report.getTransferRateOctet() + "B/s");
            log.fine("uploaded for now     : " + report.getTemporaryPacketSize() + "/" + report.getTotalPacketSize());

            if (report.getStartTime() > 0) {
                log.fine("amount of time       : " + ((report.getReportTime() - report.getStartTime()) /
                        VALUE_PER_SECONDS) + "s");
            }
            log.fine("request number       : " + report.getRequestNum());

            log.fine("--------------------------------------------------------");
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
    private static void logFinishedTask(final SpeedTestMode mode, final long packetSize, final float
            transferRateBitPerSeconds, final float transferRateOctetPerSeconds) {

        if (log.isLoggable(Level.FINE)) {
            switch (mode) {
                case DOWNLOAD:
                    log.fine("======== Download [ OK ] =============");
                    break;
                case UPLOAD:
                    log.fine("========= Upload [ OK ]  =============");
                    break;
                default:
                    break;
            }

            log.fine("upload packetSize     : " + packetSize + " octet(s)");
            log.fine("upload transfer rate  : " + transferRateBitPerSeconds + " bit/second   | " +
                    transferRateBitPerSeconds / VALUE_PER_SECONDS
                    + " Kbit/second  | " + transferRateBitPerSeconds / MEGA_VALUE_PER_SECONDS + " Mbit/second");
            log.fine("upload transfer rate  : " + transferRateOctetPerSeconds + " octet/second | " +
                    transferRateOctetPerSeconds / VALUE_PER_SECONDS
                    + " Koctet/second | " + +transferRateOctetPerSeconds / MEGA_VALUE_PER_SECONDS + " Moctet/second");
            log.fine("##################################################################");
        }
    }

    /**
     * update download progress bar.
     *
     * @param percent progress in percent
     */
    private static void updateDownloadProgressBar(final float percent) {
        initDownloadBar = true;
    }

    /**
     * update upload progress bar.
     *
     * @param percent progress in percent
     */
    private static void updateUploadProgressBar(final float percent) {
        initUploadBar = true;
    }
}
