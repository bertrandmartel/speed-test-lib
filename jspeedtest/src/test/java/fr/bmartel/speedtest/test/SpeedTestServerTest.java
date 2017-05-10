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

package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.*;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.test.utils.TestCommon;
import net.jodah.concurrentunit.Waiter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
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

        final URL url = getClass().getResource("/" + TestCommon.SERVER_LIST_FILENAME);

        final Object obj = new JSONParser().parse(new FileReader(url.getPath()));

        final JSONArray servers = (JSONArray) obj;

        for (int i = 0; i < servers.size(); i++) {

            final JSONObject serverObj = (JSONObject) servers.get(i);

            if (serverObj.containsKey("host")) {

                final String host = serverObj.get("host").toString();

                if (serverObj.containsKey("download")) {

                    final JSONArray downloadEndpoints = (JSONArray) serverObj.get("download");

                    for (int j = 0; j < downloadEndpoints.size(); j++) {

                        final JSONObject downloadEndpoint = (JSONObject) downloadEndpoints.get(j);

                        if (downloadEndpoint.containsKey("protocol")) {

                            final String protocol = downloadEndpoint.get("protocol").toString();

                            if (downloadEndpoint.containsKey("uri")) {

                                final String uri = downloadEndpoint.get("uri").toString();

                                switch (protocol) {
                                    case "http":
                                        System.out.println("[download] HTTP - testing " + host + " with uri " + uri);
                                        mWaiter = new Waiter();
                                        mSocket.startDownload("http://" + host + uri);
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        mWaiter = new Waiter();
                                        mSocket.forceStopTask();
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        break;
                                    case "ftp":

                                        String username = SpeedTestConst.FTP_DEFAULT_USER;
                                        String password = SpeedTestConst.FTP_DEFAULT_PASSWORD;

                                        if (downloadEndpoint.containsKey("username")) {
                                            username = downloadEndpoint.get("username").toString();
                                        }
                                        if (downloadEndpoint.containsKey("password")) {
                                            password = downloadEndpoint.get("password").toString();
                                        }
                                        System.out.println("[download] FTP - testing " + "ftp://" + username + ":" +
                                                password + "@" + host + ":"
                                                + SpeedTestConst.FTP_DEFAULT_PORT + uri);
                                        mWaiter = new Waiter();
                                        mSocket.startDownload("ftp://" + username + ":" + password + "@" + host + ":"
                                                + SpeedTestConst.FTP_DEFAULT_PORT + uri);
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        mWaiter = new Waiter();
                                        mSocket.forceStopTask();
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        break;
                                    default:
                                        break;
                                }

                            } else {
                                Assert.fail("download for host " + host + " has no uri");
                            }
                        } else {
                            Assert.fail("download for host " + host + " has no protocol");
                        }
                    }
                }
                if (serverObj.containsKey("upload")) {

                    final JSONArray uploadEndpoints = (JSONArray) serverObj.get("upload");

                    for (int j = 0; j < uploadEndpoints.size(); j++) {

                        final JSONObject uploadEndpoint = (JSONObject) uploadEndpoints.get(j);

                        if (uploadEndpoint.containsKey("protocol")) {

                            final String protocol = uploadEndpoint.get("protocol").toString();

                            if (uploadEndpoint.containsKey("uri")) {

                                final String uri = uploadEndpoint.get("uri").toString();

                                switch (protocol) {
                                    case "http":
                                        System.out.println("[upload] HTTP - testing " + host + " with uri " + uri);
                                        mWaiter = new Waiter();
                                        mSocket.startUpload("http://" + host + uri, TestCommon.FILE_SIZE_LARGE);
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        mWaiter = new Waiter();
                                        mSocket.forceStopTask();
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        break;
                                    case "ftp":
                                        String username = SpeedTestConst.FTP_DEFAULT_USER;
                                        String password = SpeedTestConst.FTP_DEFAULT_PASSWORD;

                                        if (uploadEndpoint.containsKey("username")) {
                                            username = uploadEndpoint.get("username").toString();
                                        }
                                        if (uploadEndpoint.containsKey("password")) {
                                            password = uploadEndpoint.get("password").toString();
                                        }
                                        System.out.println("[upload] FTP - testing " + host + " with uri " + uri);
                                        final String fileName = generateFileName() + ".txt";
                                        mWaiter = new Waiter();
                                        mSocket.startUpload("ftp://" + username + ":" + password + "@" + host + ":" +
                                                SpeedTestConst
                                                        .FTP_DEFAULT_PORT + uri + "/" +
                                                fileName, TestCommon.FILE_SIZE_LARGE);
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        mWaiter = new Waiter();
                                        mSocket.forceStopTask();
                                        mWaiter.await(TestCommon.WAITING_TIMEOUT_VERY_LONG_OPERATION, SECONDS);
                                        break;
                                    default:
                                        break;
                                }

                            } else {
                                Assert.fail("upload for host " + host + " has no uri");
                            }

                        } else {
                            Assert.fail("upload for host " + host + " has no protocol");
                        }
                    }
                }
            }
        }

        mSocket.clearListeners();
    }

    private void initSocket() {

        mSocket.setSocketTimeout(TestCommon.DEFAULT_SOCKET_TIMEOUT);

        mSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(final SpeedTestReport report) {
                //called when download is finished
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

            @Override
            public void onInterruption() {
                mWaiter.resume();
            }
        });
    }
}
