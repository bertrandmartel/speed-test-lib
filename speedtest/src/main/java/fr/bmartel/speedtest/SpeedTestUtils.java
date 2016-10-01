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
package fr.bmartel.speedtest;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

import java.util.List;

/**
 * Speed Test utility functions.
 *
 * @author Bertrand Martel
 */
public class SpeedTestUtils {

    /**
     * dispatch error listener according to errors.
     *
     * @param forceCloseSocket
     * @param listenerList
     * @param isDownload       downloading task or uploading task
     * @param errorMessage     error message from Exception
     */
    public static void dispatchError(final boolean forceCloseSocket,
                                     final List<ISpeedTestListener> listenerList,
                                     final boolean isDownload,
                                     final String errorMessage) {

        if (!forceCloseSocket) {
            if (isDownload) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.CONNECTION_ERROR, errorMessage);
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.CONNECTION_ERROR, errorMessage);
                }
            }
        } else {
            if (isDownload) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, errorMessage +
                            SpeedTestConst.FORCE_CLOSE_CAUSE_MESSAGE);
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.FORCE_CLOSE_SOCKET, errorMessage +
                            SpeedTestConst.FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * dispatch socket timeout error.
     *
     * @param forceCloseSocket
     * @param listenerList
     * @param isDownload       define if currently downloading or uploading
     * @param errorMessage     error message
     */
    public static void dispatchSocketTimeout(final boolean forceCloseSocket,
                                             final List<ISpeedTestListener> listenerList,
                                             final boolean isDownload,
                                             final String errorMessage) {

        if (!forceCloseSocket) {
            if (isDownload) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.SOCKET_TIMEOUT, errorMessage);
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onUploadError(SpeedTestError.SOCKET_TIMEOUT, errorMessage);
                }
            }
        }
    }


    /**
     * check for http uri error.
     *
     * @param forceCloseSocket
     * @param listenerList
     * @param httFrameState    http frame state to check
     */
    public static void checkHttpFrameError(final boolean forceCloseSocket,
                                           final List<ISpeedTestListener> listenerList,
                                           final HttpStates httFrameState) {

        if (httFrameState != HttpStates.HTTP_FRAME_OK) {

            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE,
                            SpeedTestConst.PARSING_ERROR +
                                    "http frame");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET,
                            SpeedTestConst.PARSING_HTTP_ERROR +
                                    "frame" + SpeedTestConst.FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * check for http header error.
     *
     * @param forceCloseSocket
     * @param listenerList
     * @param httpHeaderState  http frame state to check
     */
    public static void checkHttpHeaderError(final boolean forceCloseSocket,
                                            final List<ISpeedTestListener> listenerList,
                                            final HttpStates httpHeaderState) {

        if (httpHeaderState != HttpStates.HTTP_FRAME_OK) {

            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE,
                            SpeedTestConst.PARSING_ERROR +
                                    "http headers");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET,
                            SpeedTestConst.PARSING_HTTP_ERROR +
                                    "headers" + SpeedTestConst.FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }

    /**
     * check for http content length error.
     *
     * @param forceCloseSocket
     * @param listenerList
     * @param httpFrame        http frame state to check
     */
    public static void checkHttpContentLengthError(final boolean forceCloseSocket,
                                                   final List<ISpeedTestListener> listenerList,
                                                   final HttpFrame httpFrame) {

        if (httpFrame.getContentLength() <= 0) {

            if (!forceCloseSocket) {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error content length " +
                            "is inconsistent");
                }
            } else {
                for (int i = 0; i < listenerList.size(); i++) {
                    listenerList.get(i).onDownloadError(SpeedTestError.FORCE_CLOSE_SOCKET, "Error content length is " +
                            "inconsistent" + SpeedTestConst.FORCE_CLOSE_CAUSE_MESSAGE);
                }
            }
        }
    }
}
