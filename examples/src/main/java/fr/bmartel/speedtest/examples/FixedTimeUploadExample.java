/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016-2017 Bertrand Martel
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

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.SpeedTestMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Begin to upload a file from server & stop uploading when test duration is elapsed.
 *
 * @author Bertrand Martel
 */
public class FixedTimeUploadExample {

    /**
     * spedd examples server uri.
     */
    private static final String SPEED_TEST_SERVER_URI_UL = "http://ipv4.ikoula.testdebit.info/";

    /**
     * upload 100Mo file size.
     */
    private static final int FILE_SIZE = 100000000;

    /**
     * amount of time between each speed test reports set to 1s.
     */
    private static final int REPORT_INTERVAL = 1000;

    /**
     * speed test duration set to 15s.
     */
    private static final int SPEED_TEST_DURATION = 15000;

    /**
     * logger.
     */
    private final static Logger LOGGER = LogManager.getLogger(FixedTimeUploadExample.class.getName());

    /**
     * Fixed time upload example main.
     *
     * @param args no args required
     */
    public static void main(final String[] args) {

        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        //speedTestSocket.setUploadStorageType(UploadStorageType.FILE_STORAGE);

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(errorMessage);
                }
            }

            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when upload is finished
                LogUtils.logFinishedTask(SpeedTestMode.UPLOAD, report.getTotalPacketSize(),
                        report.getTransferRateBit(),
                        report.getTransferRateOctet(), LOGGER);
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport uploadReport) {
                //notify progress
                LogUtils.logSpeedTestReport(uploadReport, LOGGER);
            }
        });

        speedTestSocket.startFixedUpload(SPEED_TEST_SERVER_URI_UL,
                FILE_SIZE, SPEED_TEST_DURATION, REPORT_INTERVAL);
    }
}
