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

import fr.bmartel.speedtest.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Upload File Speed test example.
 *
 * @author Bertrand Martel
 */
public class UploadFileExample {

    /**
     * socket timeout used in ms.
     */
    private static final int SOCKET_TIMEOUT = 5000;

    /**
     * speed examples server host name.
     */
    private static final String SPEED_TEST_SERVER_HOST = "1.testdebit.info";

    /**
     * spedd examples server uri.
     */
    private static final String SPEED_TEST_SERVER_URI_UL = "/";

    /**
     * speed examples server port.
     */
    private static final int SPEED_TEST_SERVER_PORT = 80;

    /**
     * upload 10Mo file size.
     */
    private static final int FILE_SIZE = 10000000;

    /**
     * logger.
     */
    private static final Logger log = LogManager.getLogger(DownloadFileExample.class.getName());

    /**
     * Instantiate Speed Test and start download and upload process with speed
     * examples server of your choice.
     *
     * @param args no args required
     */
    public static void main(final String[] args) {

        // instantiate speed examples
        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        //set timeout for download
        speedTestSocket.setSocketTimeout(SOCKET_TIMEOUT);

        // add a listener to wait for speed examples completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadPacketsReceived(final long packetSize, final float transferRateBitPerSeconds, final
            float transferRateOctetPerSeconds) {

                LogUtils.logFinishedTask(SpeedTestMode.DOWNLOAD, packetSize, transferRateBitPerSeconds,
                        transferRateOctetPerSeconds, log);

            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (log.isErrorEnabled()) {
                    log.error("Download error " + speedTestError + " : " + errorMessage);
                }
            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final float transferRateBitPerSeconds, final
            float transferRateOctetPerSeconds) {

                LogUtils.logFinishedTask(SpeedTestMode.UPLOAD, packetSize, transferRateBitPerSeconds,
                        transferRateOctetPerSeconds, log);

            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {
                if (log.isErrorEnabled()) {
                    log.error("Upload error " + speedTestError + " : " + errorMessage);
                }
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport downloadReport) {

                LogUtils.logSpeedTestReport(downloadReport, log);
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport uploadReport) {

                LogUtils.logSpeedTestReport(uploadReport, log);
            }
        });

        speedTestSocket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL,
                FILE_SIZE);
    }
}
