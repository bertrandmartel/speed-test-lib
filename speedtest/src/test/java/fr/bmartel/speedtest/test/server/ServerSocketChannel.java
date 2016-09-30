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
package fr.bmartel.speedtest.test.server;


import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Server socket connection management.
 *
 * @author Bertrand Martel
 */
public class ServerSocketChannel implements Runnable, IHttpStream {

    /**
     * socket to be used by server.
     */
    private Socket socket;

    /**
     * inputstream to be used for reading.
     */
    private InputStream inputStream;

    /**
     * outputstream to be used for writing.
     */
    private OutputStream outputStream;

    /**
     * http request parser.
     */
    private HttpFrame httpFrameParser;

    private IHttpServerEventListener clientListener;

    /**
     * Initialize socket connection when the connection is available ( socket
     * parameter wil block until it is opened).
     *
     * @param socket         the socket opened
     * @param clientListener event listener
     */
    public ServerSocketChannel(final Socket socket,
                               final IHttpServerEventListener clientListener) {
        try {
            this.clientListener = clientListener;

			/* give the socket opened to the main class */
            this.socket = socket;
            /* extract the associated input stream */
            this.inputStream = socket.getInputStream();
            /* extract the associated output stream */
            this.outputStream = socket.getOutputStream();

			/*
             * initialize parsing method for request string and different body
			 * of http request
			 */
            this.httpFrameParser = new HttpFrame();
            /*
             * intialize response manager for writing data to outputstream
			 * method (headers generation ...)
			 */

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main socket thread : parse all datas passing through socket inputstream.
     */
    @Override
    public void run() {
        try {

			/* clear richRequest object (specially headers) */
            this.httpFrameParser = new HttpFrame();

            final HttpStates httpStates = this.httpFrameParser.parseHttp(inputStream);

            clientListener.onHttpFrameReceived(this.httpFrameParser,
                    httpStates, this);

            closeSocket();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close socket inputstream.
     *
     * @throws IOException
     */
    private void closeInputStream() throws IOException {
        this.inputStream.close();
    }

    /**
     * Close socket inputstream.
     */
    private void closeOutputStream() throws IOException {
        this.outputStream.close();
    }

    private void closeSocket() {
        try {
            closeInputStream();
            closeOutputStream();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int writeHttpFrame(final byte[] data) {
        try {
            this.outputStream.write(data);
            this.outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
}