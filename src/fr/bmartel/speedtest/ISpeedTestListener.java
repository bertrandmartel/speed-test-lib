/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Bertrand Martel
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
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
 * Listener for speed test output results :
 * 
 * <ul>
 * <li>monitor download process result with transfer rate in bit/s and octet/s</li>
 * <li>monitor download progress</li>
 * <li>monitor upload process result with transfer rate in bit/s and octet/s</li>
 * <li>monitor upload progress</li>
 * </ul>
 * 
 * @author Bertrand Martel
 *
 */
public interface ISpeedTestListener {

	/**
	 * 
	 * monitor download process result with transfer rate in bit/s and octet/s
	 * 
	 * @param packetSize
	 *            packet size retrieved from server
	 * @param transferRateBitPerSeconds
	 *            transfer rate in bit/seconds
	 * @param transferRateOctetPerSeconds
	 *            transfer rate in octet/seconds
	 */
	public void onDownloadPacketsReceived(int packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds);

	/**
	 * monitor download progress
	 * 
	 * @param percent
	 *            % of progress
	 */
	public void onDownloadProgress(int percent);

	/**
	 * Error catch for download process
	 * 
	 * @param errorCode
	 *            error code defined in SpeedTestError.java
	 * @param message
	 *            error message
	 */
	public void onDownloadError(int errorCode, String message);

	/**
	 * monitor upload process result with transfer rate in bit/s and octet/s
	 * 
	 * @param errorCode
	 * @param message
	 */
	public void onUploadPacketsReceived(int packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds);

	/**
	 * Error catch for upload process
	 * 
	 * @param errorCode
	 *            error code defined in SpeedTestError.java
	 * @param message
	 *            error message
	 */
	public void onUploadError(int errorCode, String message);

	/**
	 * monitor upload progress
	 * 
	 * @param percent
	 *            % of progress
	 */
	public void onUploadProgress(int percent);

}
