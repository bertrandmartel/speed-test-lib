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

package fr.bmartel.speedtest.test.server;

import fr.bmartel.protocol.http.inter.IHttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

/**
 * Http custom event : notify anybody that add that event listener to server
 * list of http request / response.
 *
 * @author Bertrand Martel
 */
public interface IHttpServerEventListener {

    /**
     * called wen server is started.
     */
    void onServerStarted();

    /**
     * called when Http frame received from inputstream.
     *
     * @param httpFrame       http frame object
     * @param receptionStates reception decoding states (should be HTTP_FRAME_OK if no
     *                        decoding error happened)
     */
    void onHttpFrameReceived(IHttpFrame httpFrame,
                             HttpStates receptionStates, IHttpStream httpStream);

}