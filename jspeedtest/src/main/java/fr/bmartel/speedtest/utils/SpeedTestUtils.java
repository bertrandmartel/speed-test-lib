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

package fr.bmartel.speedtest.utils;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;
import fr.bmartel.speedtest.SpeedTestConst;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.inter.ISpeedTestSocket;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.UploadStorageType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Speed Test utility functions.
 *
 * @author Bertrand Martel
 */
public class SpeedTestUtils {

    /**
     * random number.
     */
    private static SecureRandom random = new SecureRandom();

    /**
     * Generate a random file name for file FTP upload.
     *
     * @return random file name
     */
    public static String generateFileName() {
        return new BigInteger(130, random).toString(32);
    }

    /**
     * dispatch error listener according to errors.
     *
     * @param forceCloseSocket define if interruption callback must be called
     * @param listenerList     list of speed test listeners
     * @param errorMessage     error message from Exception
     */
    public static void dispatchError(final ISpeedTestSocket speedTestSocket,
                                     final boolean forceCloseSocket,
                                     final List<ISpeedTestListener> listenerList,
                                     final String errorMessage) {

        if (!forceCloseSocket) {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onError(SpeedTestError.CONNECTION_ERROR, errorMessage);
            }
        } else {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onCompletion(speedTestSocket.getLiveReport());
            }
        }
    }

    public static void dispatchError(
            final ISpeedTestSocket speedTestSocket,
            final boolean forceCloseSocket,
            final List<ISpeedTestListener> listenerList,
            final SpeedTestError error,
            final String errorMessage) {

        if (!forceCloseSocket) {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onError(error, errorMessage);
            }
        } else {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onCompletion(speedTestSocket.getLiveReport());
            }
        }
    }

    /**
     * Read data from RAM of FILE storage for upload task.
     *
     * @param storageType        RAM or FILE storage
     * @param body               full upload body for RAM storage case
     * @param uploadFile         file pointer to upload for FILE storage case
     * @param uploadTempFileSize temporary file size (offset)
     * @param chunkSize          chunk size to read
     * @return byte array to flush
     */
    public static byte[] readUploadData(final UploadStorageType storageType,
                                        final byte[] body,
                                        final RandomAccessFile uploadFile,
                                        final int uploadTempFileSize,
                                        final int chunkSize) throws IOException {

        byte[] data;

        if (storageType == UploadStorageType.RAM_STORAGE) {
            data = Arrays.copyOfRange(body, uploadTempFileSize,
                    uploadTempFileSize + chunkSize);
        } else {
            data = new byte[chunkSize];
            uploadFile.seek(uploadTempFileSize);
            uploadFile.read(data);
        }
        return data;
    }

    /**
     * dispatch socket timeout error.
     *
     * @param forceCloseSocket define if interruption callback must be called
     * @param listenerList     list of speed test listeners
     * @param errorMessage     error message
     */
    public static void dispatchSocketTimeout(final boolean forceCloseSocket,
                                             final List<ISpeedTestListener> listenerList,
                                             final String errorMessage) {

        if (!forceCloseSocket) {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onError(SpeedTestError.SOCKET_TIMEOUT, errorMessage);
            }
        }
    }

    /**
     * check for http uri error.
     *
     * @param forceCloseSocket define if interruption callback must be called
     * @param listenerList     list of speed test listeners
     * @param httFrameState    http frame state to check
     */
    public static void checkHttpFrameError(
            final boolean forceCloseSocket,
            final List<ISpeedTestListener> listenerList,
            final HttpStates httFrameState) {

        if (httFrameState != HttpStates.HTTP_FRAME_OK && !forceCloseSocket) {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onError(SpeedTestError.INVALID_HTTP_RESPONSE,
                        SpeedTestConst.PARSING_ERROR +
                                "http frame");
            }
        }
    }

    /**
     * check for http header error.
     *
     * @param forceCloseSocket define if interruption callback must be called
     * @param listenerList     list of speed test listeners
     * @param httpHeaderState  http frame state to check
     */
    public static void checkHttpHeaderError(
            final boolean forceCloseSocket,
            final List<ISpeedTestListener> listenerList,
            final HttpStates httpHeaderState) {

        if (httpHeaderState != HttpStates.HTTP_FRAME_OK && !forceCloseSocket) {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onError(SpeedTestError.INVALID_HTTP_RESPONSE,
                        SpeedTestConst.PARSING_ERROR +
                                "http headers");
            }
        }
    }

    /**
     * check for http content length error.
     *
     * @param forceCloseSocket define if interruption callback must be called
     * @param listenerList     list of speed test listeners
     * @param httpFrame        http frame state to check
     */
    public static void checkHttpContentLengthError(
            final boolean forceCloseSocket,
            final List<ISpeedTestListener> listenerList,
            final HttpFrame httpFrame) {

        if (httpFrame.getContentLength() <= 0 && !forceCloseSocket) {
            for (int i = 0; i < listenerList.size(); i++) {
                listenerList.get(i).onError(SpeedTestError.INVALID_HTTP_RESPONSE, "Error content length " +
                        "is inconsistent");
            }
        }
    }
}
