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
                System.out.println("Download [ OK ]");
                System.out.println("download packetSize     : " + packetSize + " octet(s)");
                System.out.println("download transfer rate  : " + transferRateBitPerSeconds + " bit/second   | " + transferRateBitPerSeconds / 1000
                        + " Kbit/second  | " + transferRateBitPerSeconds / 1000000 + " Mbit/second");
                System.out.println("download transfer rate  : " + transferRateOctetPerSeconds + " octet/second | " + transferRateOctetPerSeconds / 1000
                        + " Koctet/second | " + +transferRateOctetPerSeconds / 1000000 + " Moctet/second");
                System.out.println("##################################################################");
            }

            @Override
            public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
                System.out.println("Download error " + speedTestError + " : " + errorMessage);
            }

            @Override
            public void onUploadPacketsReceived(long packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {
                System.out.println("========= Upload [ OK ]   =============");
                System.out.println("upload packetSize     : " + packetSize + " octet(s)");
                System.out.println("upload transfer rate  : " + transferRateBitPerSeconds + " bit/second   | " + transferRateBitPerSeconds / 1000
                        + " Kbit/second  | " + transferRateBitPerSeconds / 1000000 + " Mbit/second");
                System.out.println("upload transfer rate  : " + transferRateOctetPerSeconds + " octet/second | " + transferRateOctetPerSeconds / 1000
                        + " Koctet/second | " + +transferRateOctetPerSeconds / 1000000 + " Moctet/second");
                System.out.println("##################################################################");
            }

            @Override
            public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
                System.out.println("Upload error " + speedTestError + " : " + errorMessage);
            }

            @Override
            public void onDownloadProgress(float percent, SpeedTestReport downloadReport) {

                System.out.println("---------------current download report--------------------");
                System.out.println("progress             : " + downloadReport.getProgressPercent() + "%");
                System.out.println("transfer rate bit    : " + downloadReport.getTransferRateBit() + "b/s");
                System.out.println("transfer rate octet  : " + downloadReport.getTransferRateOctet() + "B/s");
                System.out.println("downloaded for now   : " + downloadReport.getTemporaryPacketSize() + "/" + downloadReport.getTotalPacketSize());
                if (downloadReport.getStartTime() > 0) {
                    System.out.println("amount of time       : " + ((downloadReport.getReportTime() - downloadReport.getStartTime()) / 1000) + "s");
                }
                System.out.println("request number       : " + downloadReport.getRequestNum());

                if (!initDownloadBar)
                    System.out.print("download progress | < ");
                initDownloadBar = true;

                if (percent % 4 == 0)
                    System.out.print("=");

                if (percent == 100)
                    System.out.println(" 100%");

            }

            @Override
            public void onUploadProgress(float percent, SpeedTestReport uploadReport) {
                System.out.println("---------------current upload report--------------------");
                System.out.println("progress             : " + uploadReport.getProgressPercent() + "%");
                System.out.println("transfer rate bit    : " + uploadReport.getTransferRateBit() + "b/s");
                System.out.println("transfer rate octet  : " + uploadReport.getTransferRateOctet() + "B/s");
                System.out.println("uploaded for now     : " + uploadReport.getTemporaryPacketSize() + "/" + uploadReport.getTotalPacketSize());
                if (uploadReport.getStartTime() > 0) {
                    System.out.println("amount of time       : " + ((uploadReport.getReportTime() - uploadReport.getStartTime()) / 1000) + "s");
                }
                System.out.println("--------------------------------------------------------");

                if (!initUploadBar)
                    System.out.print("upload progress | < ");
                initUploadBar = true;
                if (percent % 5 == 0)
                    System.out.print("=");

                if (percent == 100) {
                    System.out.println("upload 100%");
                }
            }
        });

        speedTestSocket.startDownload("1.testdebit.info", 80, "/fichiers/10Mo.dat");
    }
}
