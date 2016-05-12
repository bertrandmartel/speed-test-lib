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
package fr.bmartel.speedtest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

/**
 * Client socket main implementation
 * <p/>
 * Two modes upload and download
 * <p/>
 * upload will write a file to a specific host with given uri. The file is
 * randomly generated with a given size
 * <p/>
 * download will retrieve a content from a specific host with given uri.
 * <p/>
 * For both mode, transfer rate is calculated independently from socket initial
 * connection
 *
 * @author Bertrand Martel
 */
public class SpeedTestSocket {

    /**
     * size of the write read buffer for downloading
     */
    private final static int READ_BUFFER_SIZE = 65535;

    /**
     * default size of each packet sent to upload server
     */
    private final static int DEFAULT_UPLOAD_SIZE_CHUNK = 65535;

    /**
     * socket server hostname
     */
    private String hostname = "";

    /**
     * socket server port
     */
    private int port = 0;

    /**
     * socket object
     */
    private Socket socket = null;

    /**
     * thread used to read http inputstream data
     */
    private Thread readingThread = null;

    /**
     * define if reading thread is currently running
     */
    private volatile boolean isReading = false;

    /**
     * speed test listener list
     */
    private List<ISpeedTestListener> speedTestListenerList = new ArrayList<ISpeedTestListener>();

    /**
     * size of file to upload
     */
    private int uploadFileSize = 0;

    /**
     * start time triggered in millis
     */
    private long timeStart = 0;

    /**
     * end time triggered in millis
     */
    private long timeEnd = 0;

    /**
     * current speed test mode
     */
    private SpeedTestMode speedTestMode = SpeedTestMode.NONE;

    /**
     * this is the size of each data sent to upload server
     */
    private int uploadChunkSize = DEFAULT_UPLOAD_SIZE_CHUNK;

    /**
     * Build Client socket
     */
    public SpeedTestSocket() {
    }

    /**
     * Add a speed test listener to list
     *
     * @param listener
     */
    public void addSpeedTestListener(ISpeedTestListener listener) {
        speedTestListenerList.add(listener);
    }

    /**
     * Relive a speed listener from list
     *
     * @param listener
     */
    public void removeSpeedTestListener(ISpeedTestListener listener) {
        speedTestListenerList.remove(listener);
    }

    /**
     * this is the number of bit uploaded at this time
     */
    private int uploadTemporaryFileSize = 0;

    /**
     * this is the number of packet dowloaded at this time
     */
    private int downloadTemporaryPacketSize = 0;

    /**
     * this is the number of packet to download
     */
    private int downloadPacketSize = 0;

    /**
     * Create and connect socket
     *
     * @param task       task to be executed when connected to socket
     * @param isDownload define if it is a download or upload test
     */
    public void connectAndExecuteTask(TimerTask task, final boolean isDownload) {

        boolean socketError = false;

        // close socket before recreating it
        if (socket != null) {
            closeSocket();
        }
        try {
            /* create a basic socket connection */
            socket = new Socket();

			/* establish socket parameters */
            socket.setReuseAddress(true);

            socket.setKeepAlive(true);

            socket.connect(new InetSocketAddress(hostname, port));

            if (readingThread != null) {
                isReading = false;
                readingThread.join();
            }

            isReading = true;
            readingThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    boolean isSocketError = false;

                    if (isDownload) {
                        byte[] buffer = new byte[READ_BUFFER_SIZE];
                        int read = 0;
                        downloadTemporaryPacketSize = 0;

                        try {

                            HttpFrame httpFrame = new HttpFrame();

                            timeStart = System.currentTimeMillis();

                            HttpStates errorCode = httpFrame.decodeFrame(socket.getInputStream());
                            if (errorCode != HttpStates.HTTP_FRAME_OK) {
                                System.err.println("Error while parsing http frame");
                                for (int i = 0; i < speedTestListenerList.size(); i++) {
                                    speedTestListenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE);
                                }
                            }

                            HttpStates headerError = httpFrame.parseHeader(socket.getInputStream());
                            if (headerError != HttpStates.HTTP_FRAME_OK) {
                                System.err.println("Error while parsing http headers");
                                for (int i = 0; i < speedTestListenerList.size(); i++) {
                                    speedTestListenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE);
                                }
                            }
                            if (httpFrame.getContentLength() < 0) {
                                System.err.println("Error content length is inconsistent");
                                for (int i = 0; i < speedTestListenerList.size(); i++) {
                                    speedTestListenerList.get(i).onDownloadError(SpeedTestError.INVALID_HTTP_RESPONSE);
                                }
                            }

                            downloadPacketSize = httpFrame.getContentLength();

                            while ((read = socket.getInputStream().read(buffer)) != -1) {
                                downloadTemporaryPacketSize += read;
                                for (int i = 0; i < speedTestListenerList.size(); i++) {
                                    SpeedTestReport report = getLiveDownloadReport();
                                    speedTestListenerList.get(i).onDownloadProgress(report.getProgressPercent(), getLiveDownloadReport());
                                }
                                if (downloadTemporaryPacketSize == downloadPacketSize) {
                                    break;
                                }
                            }
                            timeEnd = System.currentTimeMillis();

                            float transferRate_bps = (downloadPacketSize * 8) / ((timeEnd - timeStart) / 1000f);
                            float transferRate_Bps = downloadPacketSize / ((timeEnd - timeStart) / 1000f);

                            for (int i = 0; i < speedTestListenerList.size(); i++) {
                                speedTestListenerList.get(i).onDownloadPacketsReceived(downloadPacketSize, transferRate_bps, transferRate_Bps);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                            isSocketError = true;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            isSocketError = true;
                        }

                        if (isSocketError) {
                            for (int i = 0; i < speedTestListenerList.size(); i++) {
                                speedTestListenerList.get(i).onDownloadError(SpeedTestError.SOCKET_ERROR);
                            }
                        }

                        closeSocket();

                    } else {
                        while (isReading) {
                            try {
                                HttpFrame frame = new HttpFrame();

                                HttpStates httpStates = frame.parseHttp(socket.getInputStream());

                                if (httpStates == HttpStates.HTTP_FRAME_OK) {
                                    if (frame.getStatusCode() == 200 && frame.getReasonPhrase().toLowerCase().equals("ok")) {

                                        timeEnd = System.currentTimeMillis();
                                        float transferRate_bps = (uploadFileSize * 8) / ((timeEnd - timeStart) / 1000f);
                                        float transferRate_Bps = (uploadFileSize) / ((timeEnd - timeStart) / 1000f);

                                        for (int i = 0; i < speedTestListenerList.size(); i++) {
                                            speedTestListenerList.get(i).onUploadPacketsReceived(uploadFileSize, transferRate_bps, transferRate_Bps);
                                        }
                                    }
                                    speedTestMode = SpeedTestMode.NONE;
                                    return;
                                } else if (httpStates == HttpStates.HTTP_READING_ERROR) {
                                    isReading = false;
                                    closeSocket();
                                }
                                for (int i = 0; i < speedTestListenerList.size(); i++) {
                                    speedTestListenerList.get(i).onUploadError(SpeedTestError.SOCKET_ERROR);
                                }

                            } catch (SocketException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    speedTestMode = SpeedTestMode.NONE;
                }
            });
            readingThread.start();

            if (task != null) {
                task.run();
            }
        } catch (IOException e) {
            e.printStackTrace();
            socketError = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            socketError = true;
        }
        if (socketError) {
            if (isDownload) {
                for (int i = 0; i < speedTestListenerList.size(); i++) {
                    speedTestListenerList.get(i).onDownloadError(SpeedTestError.CONNECTION_ERROR);
                }
            } else {
                for (int i = 0; i < speedTestListenerList.size(); i++) {
                    speedTestListenerList.get(i).onUploadError(SpeedTestError.CONNECTION_ERROR);
                }
            }
        }
    }

    /**
     * Start download process
     *
     * @param hostname server hostname
     * @param port     server port
     * @param uri      uri to fetch to download file
     */
    public void startDownload(String hostname, int port, String uri) {
        this.hostname = hostname;
        this.port = port;
        String downloadRequest = "GET " + uri + " HTTP/1.1\r\n" + "Host: " + hostname + "\r\n\r\n";
        writeDownload(downloadRequest.getBytes());
    }

    /**
     * Write download request to server host
     *
     * @param data HTTP request to send to initiate downwload process
     */
    public void writeDownload(final byte[] data) {

        speedTestMode = SpeedTestMode.DOWNLOAD;

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {
                if (socket != null && !socket.isClosed()) {
                    try {
                        if (socket.getOutputStream() != null) {
                            timeStart = System.currentTimeMillis();
                            socket.getOutputStream().write(data);
                            socket.getOutputStream().flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, true);
    }

    /**
     * Start upload process
     *
     * @param hostname      server hostname
     * @param port          server port
     * @param uri           uri to fetch
     * @param fileSizeOctet size of file to upload
     */
    public void startUpload(String hostname, int port, String uri, int fileSizeOctet) {
        this.hostname = hostname;
        this.port = port;
        this.uploadFileSize = fileSizeOctet;
        this.timeEnd = 0;
        /* generate a file with size of fileSizeOctet octet */
        RandomGen random = new RandomGen(fileSizeOctet);
        byte[] fileContent = random.nextArray();

        String uploadRequest = "POST " + uri + " HTTP/1.1\r\n" + "Host: " + hostname + "\r\nAccept: */*\r\nContent-Length: " + fileSizeOctet + "\r\n\r\n";
        writeUpload(uploadRequest.getBytes(), fileContent);
    }

    /**
     * Write upload POST request with file generated randomly
     *
     * @param head http headers
     * @param body file content to upload
     */
    public void writeUpload(final byte[] head, final byte[] body) {

        speedTestMode = SpeedTestMode.UPLOAD;

        connectAndExecuteTask(new TimerTask() {
            @Override
            public void run() {
                if (socket != null && !socket.isClosed()) {
                    try {
                        uploadTemporaryFileSize = 0;
                        int step = body.length / uploadChunkSize;
                        int remain = body.length % uploadChunkSize;

                        if (socket.getOutputStream() != null) {

                            socket.getOutputStream().write(head);
                            socket.getOutputStream().flush();
                            timeStart = System.currentTimeMillis();

                            for (int i = 0; i < step; i++) {
                                socket.getOutputStream().write(Arrays.copyOfRange(body, uploadTemporaryFileSize, uploadTemporaryFileSize + uploadChunkSize));
                                socket.getOutputStream().flush();
                                for (int j = 0; j < speedTestListenerList.size(); j++) {
                                    SpeedTestReport report = getLiveUploadReport();
                                    speedTestListenerList.get(j).onUploadProgress(report.getProgressPercent(), report);
                                }
                                uploadTemporaryFileSize += uploadChunkSize;
                            }
                            if (remain != 0) {
                                socket.getOutputStream().write(Arrays.copyOfRange(body, uploadTemporaryFileSize, uploadTemporaryFileSize + remain));
                                socket.getOutputStream().flush();
                            }
                            for (int j = 0; j < speedTestListenerList.size(); j++) {
                                speedTestListenerList.get(j).onUploadProgress(100, getLiveUploadReport());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, false);
    }

    /**
     * get a temporary download report at this moment
     *
     * @return speed test download report
     */
    public SpeedTestReport getLiveDownloadReport() {
        return getReport(SpeedTestMode.DOWNLOAD);
    }

    /**
     * get a temporary upload report at this moment
     *
     * @return speed test upload report
     */
    public SpeedTestReport getLiveUploadReport() {
        return getReport(SpeedTestMode.UPLOAD);
    }

    /**
     * get a download/upload report
     *
     * @param mode speed test mode requested
     * @return speed test report
     */
    private SpeedTestReport getReport(SpeedTestMode mode) {
        int temporaryPacketSize = 0;
        int totalPacketSize = 0;
        switch (mode) {
            case DOWNLOAD:
                temporaryPacketSize = downloadTemporaryPacketSize;
                totalPacketSize = downloadPacketSize;
                break;
            case UPLOAD:
                temporaryPacketSize = uploadTemporaryFileSize;
                totalPacketSize = uploadFileSize;
                break;
        }
        long currentTime;
        if (timeEnd == 0) {
            currentTime = System.currentTimeMillis();
        } else {
            currentTime = timeEnd;
        }
        float transferRate_bps = (temporaryPacketSize * 8) / ((currentTime - timeStart) / 1000f);
        float transferRate_Bps = temporaryPacketSize / ((currentTime - timeStart) / 1000f);
        float percent = 0;
        if (totalPacketSize != 0) {
            percent = temporaryPacketSize * 100f / totalPacketSize;
        }

        return new SpeedTestReport(mode, percent,
                timeStart, currentTime, temporaryPacketSize, totalPacketSize, transferRate_Bps, transferRate_bps);
    }

    /**
     * Close socket streams and socket object
     */
    public void closeSocket() {

        if (socket != null) {
            try {
                socket.getOutputStream().close();
                socket.getInputStream().close();
                socket.close();
            } catch (IOException e) {
            }
        }
        socket = null;
    }

    /**
     * Join reading thread before closing socket
     */
    public void closeSocketJoinRead() {
        if (readingThread != null) {
            isReading = false;
            try {
                readingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        closeSocket();
    }

    /**
     * retrieve current speed test mode
     *
     * @return speed test mode (UPLOAD/DOWNLOAD/NONE)
     */
    public SpeedTestMode getSpeedTestMode() {
        return speedTestMode;
    }

    /**
     * retrieve size of each packet sent to upload server
     *
     * @return size of each packet sent to upload server
     */
    public int getUploadChunkSize() {
        return uploadChunkSize;
    }

    /**
     * set size of each packet sent to upload server
     *
     * @param uploadChunkSize new size of each packet sent to upload server
     */
    public void setUploadChunkSize(int uploadChunkSize) {
        this.uploadChunkSize = uploadChunkSize;
    }
}
