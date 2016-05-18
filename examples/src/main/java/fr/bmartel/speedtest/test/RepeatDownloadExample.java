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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
public class RepeatDownloadExample {

    /**
     * speed test server host name.
     */
    private final static String SPEED_TEST_SERVER_HOST = "1.testdebit.info";

    /**
     * spedd test server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL = "/fichiers/10Mo.dat";

    /**
     * speed test server port.
     */
    private final static int SPEED_TEST_SERVER_PORT = 80;

    /**
     * logger.
     */
    private final static Logger log = LogManager.getLogger(DownloadFileExample.class.getName());

    /**
     * Instanciate Speed Test and start download and upload process with speed
     * test server of your choice.
     *
     * @param args no args required
     */
    public static void main(final String[] args) {

        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.startDownloadRepeat("1.testdebit.info", 80, "/fichiers/10Mo.dat", 11000, 1000, new
                IRepeatListener() {
                    @Override
                    public void onFinish(SpeedTestReport report) {

                        if (log.isEnabled(Level.DEBUG)) {
                            log.debug("--------------------------------------------------------");
                            log.debug("---------------------DOWNLOAD FINISHED------------------");
                            log.debug("--------------------------------------------------------");
                        }
                        LogUtils.logReport(report, log);
                    }

                    @Override
                    public void onReport(SpeedTestReport report) {
                        if (log.isEnabled(Level.DEBUG)) {
                            log.debug("---------------current download report------------------");
                        }
                        LogUtils.logReport(report, log);
                    }
                });
    }
}
