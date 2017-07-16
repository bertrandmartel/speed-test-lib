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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.test.model.Server;
import fr.bmartel.speedtest.test.utils.TestCommon;
import net.jodah.concurrentunit.Waiter;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Test a default list of speed test server.
 *
 * @author Bertrand Martel
 */
public class SpeedTestServerTest extends AbstractTest {

    /**
     * Common waiter for functional test.
     */
    private Waiter mWaiter;

    private static SecureRandom random = new SecureRandom();

    private static String generateFileName() {
        return new BigInteger(130, random).toString(32);
    }

    @Test
    public void serverListTest() throws IOException, ParseException, TimeoutException {

        initSocket();

        final GsonBuilder gsonBuilder = new GsonBuilder();
        final Gson gson = gsonBuilder.create();

        final URL url = getClass().getResource("/" + TestCommon.SERVER_LIST_FILENAME);

        final JsonReader reader = new JsonReader(new FileReader(url.getPath()));

        final List<Server> serverList = gson.fromJson(reader,
                new TypeToken<List<Server>>() {
                }.getType());

        for (final Server server : serverList) {

            mWaiter = new Waiter();

            final URL serverUrl = new URL(server.getUri());

            System.out.println("[" + server.getMode() + "] " + serverUrl.getProtocol() + " - " + server.getUri());

            switch (server.getMode()) {
                case DOWNLOAD:
                    mSocket.startDownload(server.getUri());
                    break;
                case UPLOAD:

                    if (serverUrl.getProtocol().equals("ftp")) {
                        final String fileName = generateFileName() + ".txt";
                        mSocket.startUpload(server.getUri() + fileName, TestCommon.FILE_SIZE_LARGE);
                    } else {
                        mSocket.startUpload(server.getUri(), TestCommon.FILE_SIZE_LARGE);
                    }
                    break;
                default:
                    break;
            }

            mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
            mWaiter = new Waiter();
            mSocket.forceStopTask();
            mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
        }
        mSocket.clearListeners();
    }

    private void initSocket() {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
                mWaiter.resume();
            }

            @Override
            public void onError(final SpeedTestError speedTestError, final String errorMessage) {
                mWaiter.fail(TestCommon.UNEXPECTED_ERROR_STR + speedTestError + " : " + errorMessage);
                mWaiter.resume();
            }

            @Override
            public void onProgress(final float percent, final SpeedTestReport downloadReport) {
                mWaiter.resume();
            }
        });
    }
}
