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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Speed Test example
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
     * check download bar initialization
     */
    private static boolean initDownloadBar;

    /**
     * check upload bar initialization
     */
    private static boolean initUploadBar;

    /**
     * logger
     */
    private final static Logger log = Logger.getLogger(SpeedTest.class.getName());

    /**
     * Instanciate Speed Test and start download and upload process with speed
     * test server of your choice
     *
     * @param args
     */
    public static void main(final String[] args) {

		/* instanciate speed test */
        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.setSocketTimeout(5000);

		/* add a listener to wait for speed test completion and progress */
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadPacketsReceived(final long packetSize, final float transferRateBitPerSeconds, final float transferRateOctetPerSeconds) {

                logFinishedTask(SpeedTestMode.DOWNLOAD, packetSize, transferRateBitPerSeconds, transferRateOctetPerSeconds);

            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Download error " + speedTestError + " : " + errorMessage);
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final float transferRateBitPerSeconds, final float transferRateOctetPerSeconds) {

                logFinishedTask(SpeedTestMode.UPLOAD, packetSize, transferRateBitPerSeconds, transferRateOctetPerSeconds);

            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Upload error " + speedTestError + " : " + errorMessage);
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

        speedTestSocket.startDownload("1.testdebit.info", 80, "/fichiers/10Mo.dat");
    }

    /**
     * print speed test report object
     *
     * @param report
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
                log.fine("amount of time       : " + ((report.getReportTime() - report.getStartTime()) / 1000) + "s");
            }
            log.fine("request number       : " + report.getRequestNum());

            log.fine("--------------------------------------------------------");
        }
    }

    /**
     * print upload/download result
     *
     * @param mode
     * @param packetSize
     * @param transferRateBitPerSeconds
     * @param transferRateOctetPerSeconds
     */
    private static void logFinishedTask(final SpeedTestMode mode, final long packetSize, final float transferRateBitPerSeconds, final float transferRateOctetPerSeconds) {

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
            log.fine("upload transfer rate  : " + transferRateBitPerSeconds + " bit/second   | " + transferRateBitPerSeconds / 1000
                    + " Kbit/second  | " + transferRateBitPerSeconds / 1000000 + " Mbit/second");
            log.fine("upload transfer rate  : " + transferRateOctetPerSeconds + " octet/second | " + transferRateOctetPerSeconds / 1000
                    + " Koctet/second | " + +transferRateOctetPerSeconds / 1000000 + " Moctet/second");
            log.fine("##################################################################");
        }
    }

    /**
     * update download progress bar
     *
     * @param percent
     */
    private static void updateDownloadProgressBar(final float percent) {

        if (log.isLoggable(Level.FINE)) {
            if (!initDownloadBar) {
                log.fine("download progress | < ");
            }
            initDownloadBar = true;

            if (percent % 4 == 0) {
                log.fine("=");
            }

            if (percent == 100) {
                log.fine(" 100%");
            }
        }
    }

    /**
     * update upload progress bar
     *
     * @param percent
     */
    private static void updateUploadProgressBar(final float percent) {

        if (log.isLoggable(Level.FINE)) {
            if (!initUploadBar)
                log.fine("upload progress | < ");
            initUploadBar = true;
            if (percent % 5 == 0)
                log.fine("=");

            if (percent == 100) {
                log.fine("upload 100%");
            }
        }
    }
}
