package fr.bmartel.speedtest;

import java.util.Arrays;
import java.util.Random;

/**
 * Generate Random byte array for randomly generated uploaded file.
 *
 * @author Bertrand Martel
 */
public class RandomGen {

    /**
     * list of incremented byte.
     */
    private static final byte[] symbols;

    /**
     * number of incremented byte.
     */
    private static final int SYMBOLS_LENGTH = 255;

    /**
     * minimum length required for random byte array.
     */
    private static final int MINIMUM_LENGTH = 1;

    static {
        symbols = new byte[SYMBOLS_LENGTH];
        for (int i = 0; i < SYMBOLS_LENGTH; i++) {
            symbols[i] = (byte) i;
        }
    }

    /**
     * random object.
     */
    private final Random random = new Random();

    /**
     * buffer used to retrieve random values.
     */
    private final byte[] buf;

    public RandomGen(final int length) {
        if (length < MINIMUM_LENGTH) {
            throw new IllegalArgumentException("length < " + MINIMUM_LENGTH + ": " + length);
        }
        buf = new byte[length];
    }

    /**
     * generate random byte array.
     *
     * @return byte array
     */
    public byte[] nextArray() {
        for (int idx = 0; idx < buf.length; ++idx) {
            final int val = random.nextInt(SYMBOLS_LENGTH);
            buf[idx] = symbols[val];
        }
        return Arrays.copyOf(buf, buf.length);
    }
}
