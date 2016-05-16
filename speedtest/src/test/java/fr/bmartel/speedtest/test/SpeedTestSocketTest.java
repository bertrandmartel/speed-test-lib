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

import fr.bmartel.speedtest.SpeedTestSocket;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Speed test socket test
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocketTest {

    private SpeedTestSocket socket;

    private final static String HEADER = TestUtils.generateMessageHeader(SpeedTestReportTest.class);

    @Test
    public void socketTimeoutDefaultTest() {
        socket = new SpeedTestSocket();
        assertSame(socket.getSocketTimeout(), 0);
    }

    @Test
    public void socketTimeoutSetterValidTest() {
        final int socketTimeout = 10000;
        socket = new SpeedTestSocket();
        socket.setSocketTimeout(socketTimeout);
        assertSame(HEADER + "socket timeout are not equals", socket.getSocketTimeout(), socketTimeout);
    }

    @Test
    public void socketTimeoutSetterInvalidTest() {
        final int socketTimeout = -1;
        socket = new SpeedTestSocket();
        assertNotSame(HEADER + "socket timeout are equals, shouldnt be (-1)", socket.getSocketTimeout(), socketTimeout);
        assertSame(HEADER + "socket timeout should be 0", socket.getSocketTimeout(), 0);

    }

    @Test
    public void uploadChunkSizeDefaultTest() {
        socket = new SpeedTestSocket();
        assertSame(HEADER + "chunk size should be 65535 for default value", socket.getUploadChunkSize(), 65535);
    }

    @Test
    public void uploadChunkSizeSetterTest() {
        final int chunkSize = 30000;
        socket = new SpeedTestSocket();
        socket.setUploadChunkSize(chunkSize);
        assertSame(HEADER + "chunk size incorrect value after set", socket.getUploadChunkSize(), chunkSize);
    }

}
