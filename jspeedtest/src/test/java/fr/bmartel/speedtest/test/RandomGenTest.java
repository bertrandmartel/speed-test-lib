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

import fr.bmartel.speedtest.test.utils.TestUtils;
import fr.bmartel.speedtest.utils.RandomGen;
import org.junit.Assert;
import org.junit.Test;

/**
 * Random file generator examples.
 *
 * @author Bertrand Martel
 */
public class RandomGenTest {

    /**
     * unit examples message header.
     */
    private static final String HEADER = TestUtils.generateMessageHeader(RandomGenTest.class);

    /**
     * file size tested.
     */
    private final int[] SIZES = new int[]{1, 10, 10000, 10000000};

    /**
     * test generated file for upload.
     */
    @Test
    public void randomGenTest() {
        for (int i = 0; i < SIZES.length; i++) {
            final RandomGen random = new RandomGen();
            final int length = random.generateRandomArray(SIZES[i]).length;
            Assert.assertEquals(HEADER + "random generated array are not equals", length, SIZES[i]);
        }
    }
}
