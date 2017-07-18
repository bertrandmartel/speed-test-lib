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
package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.test.utils.TestCommon;
import net.jodah.concurrentunit.Waiter;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test Redirect & Moved URI.
 *
 * @author Bertrand Martel
 */
public class SpeedTestRedirect extends AbstractTest {

    /**
     * Waiter for speed test listener callback.
     */
    private static Waiter mWaiter;

    @Test
    public void redirectTest() throws TimeoutException {

        mWaiter = new Waiter();

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(final SpeedTestReport report) {
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport report) {

            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {

            }
        });
        mSocket.startDownload("http://bmartel.fr/test2");
        mWaiter.await(TestCommon.WAITING_TIMEOUT_DEFAULT_SEC, TimeUnit.SECONDS);
        mSocket.forceStopTask();
        mSocket.clearListeners();
    }
}
