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

/**
 * Interface for speed test socket.
 *
 * @author Bertrand Martel
 */
public interface ISpeedTestSocket {

    /**
     * Start upload process.
     *
     * @param hostname      server hostname
     * @param port          server port
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     */
    void startUpload(String hostname, int port, String uri, int fileSizeOctet);

    /**
     * Start download process.
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    void startDownload(String hostname, int port, String uri);


    /**
     * Add a speed test listener to list.
     *
     * @param listener speed test listener to be added
     */
    void addSpeedTestListener(ISpeedTestListener listener);

    /**
     * Relive a speed listener from list.
     *
     * @param listener speed test listener to be removed
     */
    void removeSpeedTestListener(ISpeedTestListener listener);

    /**
     * close socket + shutdown thread pool.
     */
    void forceStopTask();

    /**
     * get a temporary download report at this moment.
     *
     * @return speed test download report
     */
    SpeedTestReport getLiveDownloadReport();

    /**
     * get a temporary upload report at this moment.
     *
     * @return speed test upload report
     */
    SpeedTestReport getLiveUploadReport();

    /**
     * Close socket streams and socket object.
     */
    void closeSocket();

    /**
     * Shutdown threadpool and wait for task completion.
     */
    void shutdownAndWait();
}
