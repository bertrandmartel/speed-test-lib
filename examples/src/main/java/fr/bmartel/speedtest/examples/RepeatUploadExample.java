/*
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
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Upload a file repeatedly from speed test server during a fixed amount of time.
 *
 * @author Bertrand Martel
 */
public class RepeatUploadExample {

    /**
     * spedd examples server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_UL = "http://2.testdebit.info/";

    /**
     * logger.
     */
    private final static Logger LOGGER = LogManager.getLogger(RepeatUploadExample.class.getName());

    /**
     * speed test duration set to 11s.
     */
    private static final int SPEED_TEST_DURATION = 11000;

    /**
     * amount of time between each speed test report set to 1s.
     */
    private static final int REPORT_INTERVAL = 1000;

    /**
     * set socket timeout to 3s.
     */
    private static final int SOCKET_TIMEOUT = 3000;

    /**
     * upload 1Mo file size.
     */
    private static final int FILE_SIZE = 1000000;

    /**
     * Repeat upload example main.
     *
     * @param args no args required
     */
    public static void main(final String[] args) {

        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.setSocketTimeout(SOCKET_TIMEOUT);

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadFinished(final SpeedTestReport report) {
                //called when download is finished
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(errorMessage);
                }
            }

            @Override
            public void onUploadFinished(final SpeedTestReport report) {
                //called when upload is finished
            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport downloadReport) {
                //notify download progress
            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport uploadReport) {
                //notify upload progress
            }

            @Override
            public void onInterruption() {
                //triggered when forceStopTask is called
            }
        });

        speedTestSocket.startUploadRepeat(SPEED_TEST_SERVER_URI_UL,
                SPEED_TEST_DURATION, REPORT_INTERVAL, FILE_SIZE, new
                        IRepeatListener() {
                            @Override
                            public void onFinish(final SpeedTestReport report) {

                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug(LogUtils.LOG_REPORT_SEPARATOR);
                                    LOGGER.debug("---------------------UPLOAD FINISHED------------------");
                                    LOGGER.debug(LogUtils.LOG_REPORT_SEPARATOR);
                                }
                                LogUtils.logReport(report, LOGGER);
                            }

                            @Override
                            public void onReport(final SpeedTestReport report) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("---------------current upload report------------------");
                                }
                                LogUtils.logReport(report, LOGGER);
                            }
                        });
    }
}
