package fr.bmartel.speedtest.examples;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Chaining fixed download & upload.
 *
 * @author Bertrand Martel
 */
public class ChainingFixedExample {

    /**
     * spedd examples server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL = "http://ipv4.ikoula.testdebit.info/10M.iso";

    /**
     * logger.
     */
    private final static Logger LOGGER = LogManager.getLogger(ChainingFixedExample.class.getName());

    /**
     * speed test duration set to 3s.
     */
    private static final int SPEED_TEST_DURATION = 3000;

    /**
     * amount of time between each speed test report set to 1s.
     */
    private static final int REPORT_INTERVAL = 1000;

    /**
     * set socket timeout to 3s.
     */
    private static final int SOCKET_TIMEOUT = 3000;

    /**
     * speed test socket.
     */
    private static SpeedTestSocket speedTestSocket = new SpeedTestSocket();

    /**
     * speed examples server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_UL = "http://ipv4.ikoula.testdebit.info/";

    /**
     * upload 10Mo file size.
     */
    private static final int FILE_SIZE = 10000000;

    /**
     * chain request counter.
     */
    private static int chainCount = 10;

    /**
     * Chain fixed download/upload example main.
     *
     * @param args no args required
     */
    public static void main(final String[] args) {

        speedTestSocket.setSocketTimeout(SOCKET_TIMEOUT);

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download/upload is complete
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(LogUtils.LOG_REPORT_SEPARATOR);
                    LOGGER.debug("---------------------" + report.getSpeedTestMode() + " FINISHED------------------");
                    LOGGER.debug(LogUtils.LOG_REPORT_SEPARATOR);
                }
                LogUtils.logReport(report, LOGGER);

                if (chainCount > 0) {
                    if (chainCount % 2 == 0) {
                        speedTestSocket.startFixedUpload(SPEED_TEST_SERVER_URI_UL, FILE_SIZE, SPEED_TEST_DURATION, REPORT_INTERVAL);
                    } else {
                        speedTestSocket.startFixedDownload(SPEED_TEST_SERVER_URI_DL, SPEED_TEST_DURATION, REPORT_INTERVAL);
                    }
                    chainCount--;
                } else {
                    LOGGER.debug("chaining end");
                }
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(errorMessage);
                }
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("---------------current " + report.getSpeedTestMode() + " report------------------");
                }
                LogUtils.logReport(report, LOGGER);
            }
        });

        speedTestSocket.startFixedDownload(SPEED_TEST_SERVER_URI_DL, SPEED_TEST_DURATION, REPORT_INTERVAL);
    }
}
