package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.test.utils.TestCommon;
import net.jodah.concurrentunit.Waiter;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SpeedTestNestedTest {

    private final static String FIXED_UL_URI = "http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon
            .SPEED_TEST_SERVER_PORT + TestCommon.SPEED_TEST_SERVER_URI_UL;

    private final static String FIXED_DL_URI = "http://" + TestCommon.SPEED_TEST_SERVER_HOST + ":" + TestCommon.SPEED_TEST_SERVER_PORT +
            TestCommon.SPEED_TEST_SERVER_URI_DL;

    private Waiter mWaiter;

    /**
     * chain request counter.
     */
    private int chainCount = 2;

    @Test
    public void nestedFixedDownload() throws TimeoutException {
        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(final SpeedTestReport report) {
                if (chainCount > 0) {
                    if (chainCount % 2 == 0) {
                        speedTestSocket.startFixedUpload(FIXED_UL_URI,
                                100000000, 1000);
                    } else {
                        speedTestSocket.startFixedDownload(FIXED_DL_URI, 1000, 500);
                    }
                    chainCount--;
                } else {
                    mWaiter.resume();
                }
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError);
                mWaiter.resume();
            }
        });

        mWaiter = new Waiter();

        speedTestSocket.startFixedDownload(FIXED_DL_URI, 1000, 500);

        mWaiter.await(3, TimeUnit.SECONDS);
    }
}
