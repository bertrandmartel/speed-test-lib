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
 * Variables used for statistics & states in download repeat task
 *
 * @author Bertrand Martel
 */
public class DownloadRepeatStats {

    /**
     * define if download should be repeated
     */
    private boolean isRepeatDownload = false;

    /**
     * start time for download repeat task
     */
    private long startDateRepeat = 0;

    /**
     * time window for download repeat task
     */
    private int repeatWindows = 0;

    /**
     * current number of request for download repeat task
     */
    private int repeatRequestNum = 0;

    /**
     * number of packet pending for download repeat task
     */
    private long repeatPacketSize = 0;

    /**
     * number of packet downloaded for download repeat task
     */
    private long repeatTemporaryPacketSize = 0;

    /**
     * current transfer rate in octet/s for download repeat task
     */
    private float repeatTransferRateBps = 0;

    /**
     * define if the first download repeat has been sent and waiting for connection. It is reset to false when the client is connected to server the first time
     */
    private boolean isFirstDownloadRepeat = false;

    /**
     * define if download repeat task is finished
     */
    private boolean repeatFinished = false;

    /**
     * Build download repeat stat object
     */
    public DownloadRepeatStats() {
        isRepeatDownload = false;
        startDateRepeat = 0;
        repeatWindows = 0;
        repeatRequestNum = 0;
        repeatPacketSize = 0;
        repeatTemporaryPacketSize = 0;
        repeatTransferRateBps = 0;
        isFirstDownloadRepeat = false;
        repeatFinished = false;
    }

    public boolean isRepeatDownload() {
        return isRepeatDownload;
    }

    public void setRepeatDownload(boolean repeatDownload) {
        isRepeatDownload = repeatDownload;
    }

    public long getStartDateRepeat() {
        return startDateRepeat;
    }

    public void setStartDateRepeat(long startDateRepeat) {
        this.startDateRepeat = startDateRepeat;
    }

    public int getRepeatWindows() {
        return repeatWindows;
    }

    public void setRepeatWindows(int repeatWindows) {
        this.repeatWindows = repeatWindows;
    }

    public int getRepeatRequestNum() {
        return repeatRequestNum;
    }

    public void setRepeatRequestNum(int repeatRequestNum) {
        this.repeatRequestNum = repeatRequestNum;
    }

    public long getRepeatPacketSize() {
        return repeatPacketSize;
    }

    public void setRepeatPacketSize(long repeatPacketSize) {
        this.repeatPacketSize = repeatPacketSize;
    }

    public long getRepeatTemporaryPacketSize() {
        return repeatTemporaryPacketSize;
    }

    public void setRepeatTemporaryPacketSize(long repeatTemporaryPacketSize) {
        this.repeatTemporaryPacketSize = repeatTemporaryPacketSize;
    }

    public float getRepeatTransferRateBps() {
        return repeatTransferRateBps;
    }

    public void setRepeatTransferRateBps(float repeatTransferRateBps) {
        this.repeatTransferRateBps = repeatTransferRateBps;
    }

    public boolean isFirstDownloadRepeat() {
        return isFirstDownloadRepeat;
    }

    public void setFirstDownloadRepeat(boolean firstDownloadRepeat) {
        isFirstDownloadRepeat = firstDownloadRepeat;
    }

    public boolean isRepeatFinished() {
        return repeatFinished;
    }

    public void setRepeatFinished(boolean repeatFinished) {
        this.repeatFinished = repeatFinished;
    }
}
