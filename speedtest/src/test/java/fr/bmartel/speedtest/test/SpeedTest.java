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
    private static boolean initDownloadBar = false;

    /**
     * check upload bar initialization
     */
    private static boolean initUploadBar = false;

    /**
     * Instanciate Speed Test and start download and upload process with speed
     * test server of your choice
     *
     * @param args
     */
    public static void main(String[] args) {

		/* instanciate speed test */
        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.setSocketTimeout(5000);

		/* add a listener to wait for speed test completion and progress */
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadPacketsReceived(long packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {

                logFinishedTask(SpeedTestMode.DOWNLOAD, packetSize, transferRateBitPerSeconds, transferRateOctetPerSeconds);

            }

            @Override
            public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
                System.out.println("Download error " + speedTestError + " : " + errorMessage);
            }

            @Override
            public void onUploadPacketsReceived(long packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {

                logFinishedTask(SpeedTestMode.UPLOAD, packetSize, transferRateBitPerSeconds, transferRateOctetPerSeconds);

            }

            @Override
            public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
                System.out.println("Upload error " + speedTestError + " : " + errorMessage);
            }

            @Override
            public void onDownloadProgress(float percent, SpeedTestReport downloadReport) {

                logSpeedTestReport(downloadReport);

                updateDownloadProgressBar(percent);
            }

            @Override
            public void onUploadProgress(float percent, SpeedTestReport uploadReport) {

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
    private static void logSpeedTestReport(SpeedTestReport report) {

        switch (report.getSpeedTestMode()) {
            case DOWNLOAD:
                System.out.println("--------------current download report--------------------");
                break;
            case UPLOAD:
                System.out.println("---------------current upload report--------------------");
                break;
            default:
                break;
        }

        System.out.println("progress             : " + report.getProgressPercent() + "%");
        System.out.println("transfer rate bit    : " + report.getTransferRateBit() + "b/s");
        System.out.println("transfer rate octet  : " + report.getTransferRateOctet() + "B/s");
        System.out.println("uploaded for now     : " + report.getTemporaryPacketSize() + "/" + report.getTotalPacketSize());

        if (report.getStartTime() > 0) {
            System.out.println("amount of time       : " + ((report.getReportTime() - report.getStartTime()) / 1000) + "s");
        }
        System.out.println("request number       : " + report.getRequestNum());

        System.out.println("--------------------------------------------------------");
    }

    /**
     * print upload/download result
     *
     * @param mode
     * @param packetSize
     * @param transferRateBitPerSeconds
     * @param transferRateOctetPerSeconds
     */
    private static void logFinishedTask(SpeedTestMode mode, long packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {

        switch (mode) {
            case DOWNLOAD:
                System.out.println("======== Download [ OK ] =============");
                break;
            case UPLOAD:
                System.out.println("========= Upload [ OK ]  =============");
                break;
            default:
                break;
        }

        System.out.println("upload packetSize     : " + packetSize + " octet(s)");
        System.out.println("upload transfer rate  : " + transferRateBitPerSeconds + " bit/second   | " + transferRateBitPerSeconds / 1000
                + " Kbit/second  | " + transferRateBitPerSeconds / 1000000 + " Mbit/second");
        System.out.println("upload transfer rate  : " + transferRateOctetPerSeconds + " octet/second | " + transferRateOctetPerSeconds / 1000
                + " Koctet/second | " + +transferRateOctetPerSeconds / 1000000 + " Moctet/second");
        System.out.println("##################################################################");
    }

    /**
     * update download progress bar
     *
     * @param percent
     */
    private static void updateDownloadProgressBar(float percent) {

        if (!initDownloadBar) {
            System.out.print("download progress | < ");
        }
        initDownloadBar = true;

        if (percent % 4 == 0) {
            System.out.print("=");
        }

        if (percent == 100) {
            System.out.println(" 100%");
        }
    }

    /**
     * update upload progress bar
     *
     * @param percent
     */
    private static void updateUploadProgressBar(float percent) {

        if (!initUploadBar)
            System.out.print("upload progress | < ");
        initUploadBar = true;
        if (percent % 5 == 0)
            System.out.print("=");

        if (percent == 100) {
            System.out.println("upload 100%");
        }
    }
}
