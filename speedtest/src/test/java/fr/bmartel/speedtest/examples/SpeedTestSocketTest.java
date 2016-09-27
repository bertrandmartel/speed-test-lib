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
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Speed examples socket examples.
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocketTest {

    /**
     * speed test socket object.
     */
    private SpeedTestSocket socket;

    /**
     * unit examples message header.
     */
    private static final String HEADER = TestUtils.generateMessageHeader(SpeedTestReportTest.class);

    /**
     * value for valid socket timeout.
     */
    private static final int SOCKET_TO_VALID = 10000;

    /**
     * default value for socket timeout.
     */
    private static final int SOCKET_TO_DEFAULT = 10000;

    /**
     * value for invalid socket timeout.
     */
    private static final int SOCKET_TO_INVALID = -1;

    /**
     * default value of upload chunk size.
     */
    private static final int UPLOAD_CHUNK_SIZE_DEFAULT = 65535;

    /**
     * invalid value for upload chunk packet size.
     */
    private static final int UPLOAD_CHUNK_INVALID = 30000;

    /**
     * speed examples server host name.
     */
    private final static String SPEED_TEST_SERVER_HOST = "2.testdebit.info";

    /**
     * spedd examples server uri.
     */
    private final static String SPEED_TEST_SERVER_URI_DL = "/fichiers/100Mo.dat";

    /**
     * speed examples server port.
     */
    private final static int SPEED_TEST_SERVER_PORT = 80;

    /**
     * spedd examples server uri.
     */
    private static final String SPEED_TEST_SERVER_URI_UL = "/";

    /**
     * upload 10Mo file size.
     */
    private static final int FILE_SIZE = 10000000;

    /**
     * test socket timeout default value.
     */
    @Test
    public void socketTimeoutDefaultTest() {
        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + " socket timeout default value should be " + SOCKET_TO_DEFAULT, socket
                .getSocketTimeout(), SOCKET_TO_DEFAULT);
    }

    /**
     * test socket timeout setter valid test.
     */
    @Test
    public void socketTimeoutSetterValidTest() {
        socket = new SpeedTestSocket();
        socket.setSocketTimeout(SOCKET_TO_VALID);
        Assert.assertEquals(HEADER + "socket timeout are not equals", socket.getSocketTimeout(), SOCKET_TO_VALID);
    }

    /**
     * test socket timeout invalid value is detected.
     */
    @Test
    public void socketTimeoutSetterInvalidTest() {
        socket = new SpeedTestSocket();
        Assert.assertNotSame(HEADER + "socket timeout are equals, shouldnt be (-1)", socket.getSocketTimeout(),
                SOCKET_TO_INVALID);
        Assert.assertEquals(HEADER + "socket timeout should be " + SOCKET_TO_DEFAULT, socket.getSocketTimeout
                (), SOCKET_TO_DEFAULT);

    }

    /**
     * test upload chunk size default value.
     */
    @Test
    public void uploadChunkSizeDefaultTest() {
        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + "chunk size should be 65535 for default value", socket.getUploadChunkSize(),
                UPLOAD_CHUNK_SIZE_DEFAULT);
    }

    /**
     * test upload chunk size setter valid value.
     */
    @Test
    public void uploadChunkSizeSetterTest() {
        socket = new SpeedTestSocket();
        socket.setUploadChunkSize(UPLOAD_CHUNK_INVALID);
        Assert.assertEquals(HEADER + "chunk size incorrect value after set", socket.getUploadChunkSize(),
                UPLOAD_CHUNK_INVALID);
    }

    /**
     * test rounding mode setter valid value.
     */
    @Test
    public void defaultRoundingModeSetterTest() {
        socket = new SpeedTestSocket();
        socket.setDefaultRoundingMode(RoundingMode.HALF_UP);
        Assert.assertEquals(HEADER + "rounding mode incorrect value after set", socket.getDefaultRoundingMode(),
                RoundingMode.HALF_UP);
    }

    /**
     * test scale setter valid value.
     */
    @Test
    public void defaultScaleSetterTest() {
        socket = new SpeedTestSocket();
        socket.setDefaultScale(8);
        Assert.assertEquals(HEADER + "scale incorrect value after set", socket.getDefaultScale(),
                8);
    }

    /**
     * test speed test mode value.
     */
    @Test
    public void speedTestModeTest() {
        socket = new SpeedTestSocket();
        Assert.assertEquals(HEADER + "speed test mode value after init", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);
        socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL);
        Assert.assertEquals(HEADER + "speed test mode value after startDownload", socket.getSpeedTestMode(),
                SpeedTestMode.DOWNLOAD);
        socket.forceStopTask();
        Assert.assertEquals(HEADER + "speed test mode value after forceStopTask", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                socket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL,
                        FILE_SIZE);
            }
        }).start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(HEADER + "speed test mode value after startUpload", socket.getSpeedTestMode(),
                SpeedTestMode.UPLOAD);
        socket.forceStopTask();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(HEADER + "speed test mode value after forceStopTask", socket.getSpeedTestMode(),
                SpeedTestMode.NONE);
    }

    /**
     * test listener object.
     */
    @Test
    public void listenerTest() {
        socket = new SpeedTestSocket();

        final List<ISpeedTestListener> listenerList = new ArrayList<ISpeedTestListener>();

        final ISpeedTestListener listener = new ISpeedTestListener() {
            @Override
            public void onDownloadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                  final BigDecimal transferRateOps) {

            }

            @Override
            public void onDownloadProgress(final float percent, final SpeedTestReport report) {

            }

            @Override
            public void onDownloadError(final SpeedTestError speedTestError, final String errorMessage) {

            }

            @Override
            public void onUploadPacketsReceived(final long packetSize, final BigDecimal transferRateBps,
                                                final BigDecimal transferRateOps) {

            }

            @Override
            public void onUploadError(final SpeedTestError speedTestError, final String errorMessage) {

            }

            @Override
            public void onUploadProgress(final float percent, final SpeedTestReport report) {

            }
        };

        try {
            final Field field = socket.getClass().getDeclaredField("listenerList");
            Assert.assertNotNull(HEADER + "listenerList is null", field);
            field.setAccessible(true);
            field.set(socket, listenerList);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        socket.addSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener add failed", listenerList.size(), 1);

        socket.removeSpeedTestListener(listener);

        Assert.assertEquals(HEADER + "listener remove failed", listenerList.size(), 0);
    }

    /**
     * test socket valid value.
     */
    @Test
    public void socketTest() {
        socket = new SpeedTestSocket();

        try {
            final Field field = socket.getClass().getDeclaredField("socket");
            Assert.assertNotNull(HEADER + "socket is null", field);
            field.setAccessible(true);

            Assert.assertNull(HEADER + "socket value at init", field.get(socket));

            socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL);
            Thread.sleep(500);
            Assert.assertNotNull(HEADER + "socket value after download", field.get(socket));
            Assert.assertTrue(HEADER + "socket connected after download", ((Socket) field.get(socket)).isConnected());
            Assert.assertFalse(HEADER + "socket closed after download", ((Socket) field.get(socket)).isClosed());

            socket.forceStopTask();
            Thread.sleep(500);
            Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(socket)).isClosed());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    socket.startUpload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_UL,
                            FILE_SIZE);
                }
            }).start();

            Thread.sleep(500);
            Assert.assertNotNull(HEADER + "socket value after upload", field.get(socket));
            Assert.assertTrue(HEADER + "socket connected after upload", ((Socket) field.get(socket)).isConnected());
            Assert.assertFalse(HEADER + "socket closed after upload", ((Socket) field.get(socket)).isClosed());

            socket.forceStopTask();
            Thread.sleep(500);
            Assert.assertTrue(HEADER + "socket closed after stop upload", ((Socket) field.get(socket)).isClosed());

            socket.startDownload(SPEED_TEST_SERVER_HOST, SPEED_TEST_SERVER_PORT, SPEED_TEST_SERVER_URI_DL);
            Thread.sleep(500);
            Assert.assertNotNull(HEADER + "socket value after download", field.get(socket));
            Assert.assertTrue(HEADER + "socket connected after download", ((Socket) field.get(socket)).isConnected());
            Assert.assertFalse(HEADER + "socket closed after download", ((Socket) field.get(socket)).isClosed());
            socket.closeSocket();
            Assert.assertTrue(HEADER + "socket closed after stop download", ((Socket) field.get(socket)).isClosed());

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
