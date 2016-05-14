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

/**
 * Speed Test stats variables used for report + states
 *
 * @author Bertrand Martel
 */
public class SpeedTestStats {

    /**
     * size of file to upload
     */
    private long uploadFileSize = 0;

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
    private long downloadPacketSize = 0;

    /**
     * Build speed test stats object
     */
    public SpeedTestStats() {
        uploadFileSize = 0;
        timeStart = 0;
        timeEnd = 0;
        speedTestMode = SpeedTestMode.NONE;
        uploadTemporaryFileSize = 0;
        downloadTemporaryPacketSize = 0;
        downloadPacketSize = 0;
    }

    public long getUploadFileSize() {
        return uploadFileSize;
    }

    public void setUploadFileSize(long uploadFileSize) {
        this.uploadFileSize = uploadFileSize;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public SpeedTestMode getSpeedTestMode() {
        return speedTestMode;
    }

    public void setSpeedTestMode(SpeedTestMode speedTestMode) {
        this.speedTestMode = speedTestMode;
    }

    public int getUploadTemporaryFileSize() {
        return uploadTemporaryFileSize;
    }

    public void setUploadTemporaryFileSize(int uploadTemporaryFileSize) {
        this.uploadTemporaryFileSize = uploadTemporaryFileSize;
    }

    public int getDownloadTemporaryPacketSize() {
        return downloadTemporaryPacketSize;
    }

    public void setDownloadTemporaryPacketSize(int downloadTemporaryPacketSize) {
        this.downloadTemporaryPacketSize = downloadTemporaryPacketSize;
    }

    public long getDownloadPacketSize() {
        return downloadPacketSize;
    }

    public void setDownloadPacketSize(long downloadPacketSize) {
        this.downloadPacketSize = downloadPacketSize;
    }
}
