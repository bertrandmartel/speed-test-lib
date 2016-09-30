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
package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.SpeedTestSocket;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

/**
 * Repeat vars wrapper to retrieve those private fields and test directly in SpeedTestSocketTest class.
 *
 * @author Bertrand Martel
 */
public class RepeatVars {

    /**
     * define if download should be repeated.
     */
    private Field repeatDownload;

    /**
     * define if upload should be repeated.
     */
    private Field repeatUpload;

    /**
     * start time for download repeat task.
     */
    private Field startDateRepeat;

    /**
     * time window for download repeat task.
     */
    private Field repeatWindows;

    /**
     * current number of request for download repeat task.
     */
    private Field repeatRequestNum;

    /**
     * number of packet pending for download repeat task.
     */
    private Field repeatPacketSize;

    /**
     * number of packet downloaded for download/upload repeat task.
     */
    private Field repeatTempPckSize;

    /**
     * define if the first download repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private Field firstDownloadRepeat;

    /**
     * define if the first upload repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private Field firstUploadRepeat;

    /**
     * transfer rate list.
     */
    private Field repeatTransferRateList;

    /**
     * define if download repeat task is finished.
     */
    private Field repeatFinished;

    /**
     * Object class to call the Fields from.
     */
    private final SpeedTestSocket socket;

    /**
     * Get repeat vars field and set SpeedTestSocket object ref.
     *
     * @param socket
     */
    public RepeatVars(final SpeedTestSocket socket) {

        this.socket = socket;
        try {
            repeatDownload = socket.getClass().getDeclaredField("isRepeatDownload");
            Assert.assertNotNull("repeatDownload is null", repeatDownload);
            repeatDownload.setAccessible(true);

            repeatUpload = socket.getClass().getDeclaredField("isRepeatUpload");
            Assert.assertNotNull("repeatUpload is null", repeatUpload);
            repeatUpload.setAccessible(true);

            startDateRepeat = socket.getClass().getDeclaredField("startDateRepeat");
            Assert.assertNotNull("startDateRepeat is null", startDateRepeat);
            startDateRepeat.setAccessible(true);

            repeatWindows = socket.getClass().getDeclaredField("repeatWindows");
            Assert.assertNotNull("repeatWindows is null", repeatWindows);
            repeatWindows.setAccessible(true);

            repeatRequestNum = socket.getClass().getDeclaredField("repeatRequestNum");
            Assert.assertNotNull("repeatRequestNum is null", repeatRequestNum);
            repeatRequestNum.setAccessible(true);

            repeatPacketSize = socket.getClass().getDeclaredField("repeatPacketSize");
            Assert.assertNotNull("repeatPacketSize is null", repeatPacketSize);
            repeatPacketSize.setAccessible(true);

            repeatTempPckSize = socket.getClass().getDeclaredField("repeatTempPckSize");
            Assert.assertNotNull("repeatTempPckSize is null", repeatTempPckSize);
            repeatTempPckSize.setAccessible(true);

            firstDownloadRepeat = socket.getClass().getDeclaredField("isFirstDownloadRepeat");
            Assert.assertNotNull("firstDownloadRepeat is null", firstDownloadRepeat);
            firstDownloadRepeat.setAccessible(true);

            firstUploadRepeat = socket.getClass().getDeclaredField("isFirstUploadRepeat");
            Assert.assertNotNull("firstUploadRepeat is null", firstUploadRepeat);
            firstUploadRepeat.setAccessible(true);

            repeatTransferRateList = socket.getClass().getDeclaredField("repeatTransferRateList");
            Assert.assertNotNull("repeatTransferRateList is null", repeatTransferRateList);
            repeatTransferRateList.setAccessible(true);

            repeatFinished = socket.getClass().getDeclaredField("repeatFinished");
            Assert.assertNotNull("repeatFinished is null", repeatFinished);
            repeatFinished.setAccessible(true);

        } catch (NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Get current value of field repeatDownload.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isRepeatDownload() throws IllegalAccessException {
        return repeatDownload.getBoolean(socket);
    }

    /**
     * Set value of field repeatDownload.
     *
     * @param repeatDownload
     * @throws IllegalAccessException
     */
    public void setRepeatDownload(final boolean repeatDownload) throws IllegalAccessException {
        this.repeatDownload.setBoolean(socket, repeatDownload);
    }

    /**
     * Get current value of field repeatUpload.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isRepeatUpload() throws IllegalAccessException {
        return repeatUpload.getBoolean(socket);
    }

    /**
     * Set value of field repeatUpload.
     *
     * @param repeatUpload
     * @throws IllegalAccessException
     */
    public void setRepeatUpload(final boolean repeatUpload) throws IllegalAccessException {
        this.repeatUpload.setBoolean(socket, repeatUpload);
    }

    /**
     * Get current value of field startDateRepeat.
     *
     * @return
     * @throws IllegalAccessException
     */
    public long getStartDateRepeat() throws IllegalAccessException {
        return startDateRepeat.getLong(socket);
    }

    /**
     * Set current value of field startDateRepeat.
     *
     * @param startDateRepeat
     * @throws IllegalAccessException
     */
    public void setStartDateRepeat(final long startDateRepeat) throws IllegalAccessException {
        this.startDateRepeat.setLong(socket, startDateRepeat);
    }

    /**
     * Get current value of field repeatWindows.
     *
     * @return
     * @throws IllegalAccessException
     */
    public int getRepeatWindows() throws IllegalAccessException {
        return repeatWindows.getInt(socket);
    }

    /**
     * Set current value of field repeatWindows.
     *
     * @param repeatWindows
     * @throws IllegalAccessException
     */
    public void setRepeatWindows(final int repeatWindows) throws IllegalAccessException {
        this.repeatWindows.setInt(socket, repeatWindows);
    }

    /**
     * Get current value of field repeatRequestNum.
     *
     * @return
     * @throws IllegalAccessException
     */
    public int getRepeatRequestNum() throws IllegalAccessException {
        return repeatRequestNum.getInt(socket);
    }

    /**
     * Set current value of field repeatRequestNum.
     *
     * @param repeatRequestNum
     * @throws IllegalAccessException
     */
    public void setRepeatRequestNum(final int repeatRequestNum) throws IllegalAccessException {
        this.repeatRequestNum.setInt(socket, repeatRequestNum);
    }

    /**
     * Get current value of field repeatPacketSize.
     *
     * @return
     * @throws IllegalAccessException
     */
    public BigDecimal getRepeatPacketSize() throws IllegalAccessException {
        return (BigDecimal) repeatPacketSize.get(socket);
    }

    /**
     * Set current value of field repeatPacketSize.
     *
     * @param repeatPacketSize
     * @throws IllegalAccessException
     */
    public void setRepeatPacketSize(final BigDecimal repeatPacketSize) throws IllegalAccessException {
        this.repeatPacketSize.set(socket, repeatPacketSize);
    }

    /**
     * Get current value of field repeatTempPckSize.
     *
     * @return
     */
    public long getRepeatTempPckSize() throws IllegalAccessException {
        return repeatTempPckSize.getLong(socket);
    }

    /**
     * Set current value of field repeatTempPckSize.
     *
     * @param repeatTempPckSize
     */
    public void setRepeatTempPckSize(final long repeatTempPckSize) throws IllegalAccessException {
        this.repeatTempPckSize.setLong(socket, repeatTempPckSize);
    }

    /**
     * Get current value of field firstDownloadRepeat.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isFirstDownloadRepeat() throws IllegalAccessException {
        return firstDownloadRepeat.getBoolean(socket);
    }

    /**
     * Set current value of field firstDownloadRepeat.
     *
     * @param firstDownloadRepeat
     * @throws IllegalAccessException
     */
    public void setFirstDownloadRepeat(final boolean firstDownloadRepeat) throws IllegalAccessException {
        this.firstDownloadRepeat.setBoolean(socket, firstDownloadRepeat);
    }

    /**
     * Get current value of field firstUploadRepeat.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isFirstUploadRepeat() throws IllegalAccessException {
        return firstUploadRepeat.getBoolean(socket);
    }

    /**
     * Set current value of field firstUploadRepeat.
     *
     * @param firstUploadRepeat
     * @throws IllegalAccessException
     */
    public void setFirstUploadRepeat(final boolean firstUploadRepeat) throws IllegalAccessException {
        this.firstUploadRepeat.setBoolean(socket, firstUploadRepeat);
    }

    /**
     * Get current value of field repeatTransferRateList.
     *
     * @return
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public List<BigDecimal> getRepeatTransferRateList() throws IllegalAccessException {
        return (List<BigDecimal>) repeatTransferRateList.get(socket);
    }

    /**
     * Set current value of field repeatTransferRateList.
     *
     * @param repeatTransferRateList
     * @throws IllegalAccessException
     */
    public void setRepeatTransferRateList(final List<BigDecimal> repeatTransferRateList) throws IllegalAccessException {
        this.repeatTransferRateList.set(socket, repeatTransferRateList);
    }

    /**
     * Get current value of repeatFinished.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isRepeatFinished() throws IllegalAccessException {
        return repeatFinished.getBoolean(socket);
    }

    /**
     * Set current value of repeatFinished.
     *
     * @param repeatFinished
     * @throws IllegalAccessException
     */
    public void setRepeatFinished(final boolean repeatFinished) throws IllegalAccessException {
        this.repeatFinished.setBoolean(socket, repeatFinished);
    }
}
