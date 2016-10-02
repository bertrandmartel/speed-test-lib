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

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Constants for Speed Test library.
 *
 * @author Bertrand Martel
 */
public class SpeedTestConst {

    /**
     * maximum size for report thread pool.
     */
    public static final int THREAD_POOL_REPORT_SIZE = 1;

    /**
     * size of the write read buffer for downloading.
     */
    public static final int READ_BUFFER_SIZE = 65535;

    /**
     * default size of each packet sent to upload server.
     */
    public static final int DEFAULT_UPLOAD_SIZE = 65535;

    /**
     * default socket timeout in milliseconds.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = 10000;

    /**
     * time to wait for task to complete when threadpool is shutdown
     */
    public static final int THREADPOOL_WAIT_COMPLETION_MS = 500;

    /**
     * http ok status code.
     */
    public static final int HTTP_OK = 200;

    /**
     * max value for percent.
     */
    public static final BigDecimal PERCENT_MAX = new BigDecimal("100");

    /**
     * millisecond divider.
     */
    public static final BigDecimal MILLIS_DIVIDER = new BigDecimal("1000");

    /**
     * bit multiplier value.
     */
    public static final BigDecimal BIT_MULTIPLIER = new BigDecimal("8");

    /**
     * parsing error message.
     */
    public static final String PARSING_ERROR = "Error occurred while parsing ";

    /**
     * writing socket error message.
     */
    public static final String SOCKET_WRITE_ERROR = "Error occurred while writing to socket";

    /**
     * default scale for BigDecimal.
     */
    public static final int DEFAULT_SCALE = 4;

    /**
     * default rounding mode for BigDecimal.
     */
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;

}
