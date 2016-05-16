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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Speed test socket test
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocketTest {

    private SpeedTestSocket socket = null;

    public SpeedTestSocketTest() {
    }

    @Test
    public void socketTimeoutDefaultTest() {
        socket = new SpeedTestSocket();
        assertTrue(socket.getSocketTimeout() == 0);
    }

    @Test
    public void socketTimeoutSetterTest() {

        int socketTimeout = 10000;

        socket = new SpeedTestSocket();
        socket.setSocketTimeout(socketTimeout);
        assertTrue(socket.getSocketTimeout() == socketTimeout);

        socket = new SpeedTestSocket();
        socketTimeout = -1;
        assertFalse(socket.getSocketTimeout() == socketTimeout);
        assertTrue(socket.getSocketTimeout() == 0);

    }

    @Test
    public void uploadChunkSizeDefaultTest() {
        socket = new SpeedTestSocket();
        assertTrue(socket.getUploadChunkSize() == 65535);
    }

    @Test
    public void uploadChunkSizeSetterTest() {
        int chunkSize = 30000;
        socket = new SpeedTestSocket();
        socket.setUploadChunkSize(chunkSize);
        assertTrue(socket.getUploadChunkSize() == chunkSize);
    }

}
