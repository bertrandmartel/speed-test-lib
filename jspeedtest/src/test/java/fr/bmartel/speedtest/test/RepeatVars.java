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

package fr.bmartel.speedtest.test;

import fr.bmartel.speedtest.RepeatWrapper;
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
    private final Field mRepeatDownload;

    /**
     * define if upload should be repeated.
     */
    private final Field mRepeatUpload;

    /**
     * start time for download repeat task.
     */
    private final Field mStartDateRepeat;

    /**
     * time window for download repeat task.
     */
    private final Field mRepeatWindows;

    /**
     * current number of request for download repeat task.
     */
    private final Field mRepeatRequestNum;

    /**
     * number of packet pending for download repeat task.
     */
    private final Field mRepeatPacketSize;

    /**
     * number of packet downloaded for download/upload repeat task.
     */
    private final Field mRepeatTempPckSize;

    /**
     * define if the first download repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private final Field mFirstDownloadRepeat;

    /**
     * define if the first upload repeat has been sent and waiting for connection
     * It is reset to false when the client is connected to server the first time.
     */
    private final Field mFirstUploadRepeat;

    /**
     * transfer rate list.
     */
    private final Field mRepeatTransferRateList;

    /**
     * define if download repeat task is finished.
     */
    private final Field mRepeatFinished;

    /**
     * Repeat wrapper.
     */
    private final RepeatWrapper mRepeatWrapper;

    /**
     * Get repeat vars field and set SpeedTestSocket object ref.
     *
     * @param socket
     */
    public RepeatVars(final SpeedTestSocket socket) throws NoSuchFieldException, IllegalAccessException {

        final Field repeatWrapperField = socket.getClass().getDeclaredField("mRepeatWrapper");
        Assert.assertNotNull("mRepeatWrapper is null", repeatWrapperField);
        repeatWrapperField.setAccessible(true);

        mRepeatWrapper = (RepeatWrapper) repeatWrapperField.get(socket);
        Assert.assertNotNull("mRepeatWrapper is null", mRepeatWrapper);

        mRepeatDownload = mRepeatWrapper.getClass().getDeclaredField("mRepeatDownload");
        Assert.assertNotNull("mRepeatDownload is null", mRepeatDownload);
        mRepeatDownload.setAccessible(true);

        mRepeatUpload = mRepeatWrapper.getClass().getDeclaredField("mRepeatUpload");
        Assert.assertNotNull("mRepeatUpload is null", mRepeatUpload);
        mRepeatUpload.setAccessible(true);

        mStartDateRepeat = mRepeatWrapper.getClass().getDeclaredField("mStartDateRepeat");
        Assert.assertNotNull("mStartDateRepeat is null", mStartDateRepeat);
        mStartDateRepeat.setAccessible(true);

        mRepeatWindows = mRepeatWrapper.getClass().getDeclaredField("mRepeatWindows");
        Assert.assertNotNull("mRepeatWindows is null", mRepeatWindows);
        mRepeatWindows.setAccessible(true);

        mRepeatRequestNum = mRepeatWrapper.getClass().getDeclaredField("mRepeatRequestNum");
        Assert.assertNotNull("mRepeatRequestNum is null", mRepeatRequestNum);
        mRepeatRequestNum.setAccessible(true);

        mRepeatPacketSize = mRepeatWrapper.getClass().getDeclaredField("mRepeatPacketSize");
        Assert.assertNotNull("mRepeatPacketSize is null", mRepeatPacketSize);
        mRepeatPacketSize.setAccessible(true);

        mRepeatTempPckSize = mRepeatWrapper.getClass().getDeclaredField("mRepeatTempPckSize");
        Assert.assertNotNull("mRepeatTempPckSize is null", mRepeatTempPckSize);
        mRepeatTempPckSize.setAccessible(true);

        mFirstDownloadRepeat = mRepeatWrapper.getClass().getDeclaredField("mFirstDownloadRepeat");
        Assert.assertNotNull("mFirstDownloadRepeat is null", mFirstDownloadRepeat);
        mFirstDownloadRepeat.setAccessible(true);

        mFirstUploadRepeat = mRepeatWrapper.getClass().getDeclaredField("mFirstUploadRepeat");
        Assert.assertNotNull("mFirstUploadRepeat is null", mFirstUploadRepeat);
        mFirstUploadRepeat.setAccessible(true);

        mRepeatTransferRateList = mRepeatWrapper.getClass().getDeclaredField("mRepeatTransferRateList");
        Assert.assertNotNull("mRepeatTransferRateList is null", mRepeatTransferRateList);
        mRepeatTransferRateList.setAccessible(true);

        mRepeatFinished = mRepeatWrapper.getClass().getDeclaredField("mRepeatFinished");
        Assert.assertNotNull("mRepeatFinished is null", mRepeatFinished);
        mRepeatFinished.setAccessible(true);
    }

    /**
     * Get current value of field mRepeatDownload.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isRepeatDownload() throws IllegalAccessException {
        return mRepeatDownload.getBoolean(mRepeatWrapper);
    }

    /**
     * Set value of field mRepeatDownload.
     *
     * @param repeatDownload
     * @throws IllegalAccessException
     */
    public void setRepeatDownload(final boolean repeatDownload) throws IllegalAccessException {
        this.mRepeatDownload.setBoolean(mRepeatWrapper, repeatDownload);
    }

    /**
     * Get current value of field mRepeatUpload.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isRepeatUpload() throws IllegalAccessException {
        return mRepeatUpload.getBoolean(mRepeatWrapper);
    }

    /**
     * Set value of field mRepeatUpload.
     *
     * @param repeatUpload
     * @throws IllegalAccessException
     */
    public void setRepeatUpload(final boolean repeatUpload) throws IllegalAccessException {
        this.mRepeatUpload.setBoolean(mRepeatWrapper, repeatUpload);
    }

    /**
     * Get current value of field mStartDateRepeat.
     *
     * @return
     * @throws IllegalAccessException
     */
    public long getStartDateRepeat() throws IllegalAccessException {
        return mStartDateRepeat.getLong(mRepeatWrapper);
    }

    /**
     * Set current value of field mStartDateRepeat.
     *
     * @param startDateRepeat
     * @throws IllegalAccessException
     */
    public void setStartDateRepeat(final long startDateRepeat) throws IllegalAccessException {
        this.mStartDateRepeat.setLong(mRepeatWrapper, startDateRepeat);
    }

    /**
     * Get current value of field mRepeatWindows.
     *
     * @return
     * @throws IllegalAccessException
     */
    public int getRepeatWindows() throws IllegalAccessException {
        return mRepeatWindows.getInt(mRepeatWrapper);
    }

    /**
     * Set current value of field mRepeatWindows.
     *
     * @param repeatWindows
     * @throws IllegalAccessException
     */
    public void setRepeatWindows(final int repeatWindows) throws IllegalAccessException {
        this.mRepeatWindows.setInt(mRepeatWrapper, repeatWindows);
    }

    /**
     * Get current value of field mRepeatRequestNum.
     *
     * @return
     * @throws IllegalAccessException
     */
    public int getRepeatRequestNum() throws IllegalAccessException {
        return mRepeatRequestNum.getInt(mRepeatWrapper);
    }

    /**
     * Set current value of field mRepeatRequestNum.
     *
     * @param repeatRequestNum
     * @throws IllegalAccessException
     */
    public void setRepeatRequestNum(final int repeatRequestNum) throws IllegalAccessException {
        this.mRepeatRequestNum.setInt(mRepeatWrapper, repeatRequestNum);
    }

    /**
     * Get current value of field mRepeatPacketSize.
     *
     * @return
     * @throws IllegalAccessException
     */
    public BigDecimal getRepeatPacketSize() throws IllegalAccessException {
        return (BigDecimal) mRepeatPacketSize.get(mRepeatWrapper);
    }

    /**
     * Set current value of field mRepeatPacketSize.
     *
     * @param repeatPacketSize
     * @throws IllegalAccessException
     */
    public void setRepeatPacketSize(final BigDecimal repeatPacketSize) throws IllegalAccessException {
        this.mRepeatPacketSize.set(mRepeatWrapper, repeatPacketSize);
    }

    /**
     * Get current value of field mRepeatTempPckSize.
     *
     * @return
     */
    public long getRepeatTempPckSize() throws IllegalAccessException {
        return mRepeatTempPckSize.getLong(mRepeatWrapper);
    }

    /**
     * Set current value of field mRepeatTempPckSize.
     *
     * @param repeatTempPckSize
     */
    public void setRepeatTempPckSize(final long repeatTempPckSize) throws IllegalAccessException {
        this.mRepeatTempPckSize.setLong(mRepeatWrapper, repeatTempPckSize);
    }

    /**
     * Get current value of field mFirstDownloadRepeat.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isFirstDownloadRepeat() throws IllegalAccessException {
        return mFirstDownloadRepeat.getBoolean(mRepeatWrapper);
    }

    /**
     * Set current value of field mFirstDownloadRepeat.
     *
     * @param firstDownloadRepeat
     * @throws IllegalAccessException
     */
    public void setFirstDownloadRepeat(final boolean firstDownloadRepeat) throws IllegalAccessException {
        this.mFirstDownloadRepeat.setBoolean(mRepeatWrapper, firstDownloadRepeat);
    }

    /**
     * Get current value of field mFirstUploadRepeat.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isFirstUploadRepeat() throws IllegalAccessException {
        return mFirstUploadRepeat.getBoolean(mRepeatWrapper);
    }

    /**
     * Set current value of field mFirstUploadRepeat.
     *
     * @param firstUploadRepeat
     * @throws IllegalAccessException
     */
    public void setFirstUploadRepeat(final boolean firstUploadRepeat) throws IllegalAccessException {
        this.mFirstUploadRepeat.setBoolean(mRepeatWrapper, firstUploadRepeat);
    }

    /**
     * Get current value of field mRepeatTransferRateList.
     *
     * @return
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public List<BigDecimal> getRepeatTransferRateList() throws IllegalAccessException {
        return (List<BigDecimal>) mRepeatTransferRateList.get(mRepeatWrapper);
    }

    /**
     * Set current value of field mRepeatTransferRateList.
     *
     * @param repeatTransferRateList
     * @throws IllegalAccessException
     */
    public void setRepeatTransferRateList(final List<BigDecimal> repeatTransferRateList) throws IllegalAccessException {
        this.mRepeatTransferRateList.set(mRepeatWrapper, repeatTransferRateList);
    }

    /**
     * Get current value of mRepeatFinished.
     *
     * @return
     * @throws IllegalAccessException
     */
    public boolean isRepeatFinished() throws IllegalAccessException {
        return mRepeatFinished.getBoolean(mRepeatWrapper);
    }

    /**
     * Set current value of mRepeatFinished.
     *
     * @param repeatFinished
     * @throws IllegalAccessException
     */
    public void setRepeatFinished(final boolean repeatFinished) throws IllegalAccessException {
        this.mRepeatFinished.setBoolean(mRepeatWrapper, repeatFinished);
    }
}
