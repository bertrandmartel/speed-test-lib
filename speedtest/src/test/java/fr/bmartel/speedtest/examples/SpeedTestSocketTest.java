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

import fr.bmartel.speedtest.SpeedTestSocket;

import org.junit.Assert;
import org.junit.Test;

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

}
