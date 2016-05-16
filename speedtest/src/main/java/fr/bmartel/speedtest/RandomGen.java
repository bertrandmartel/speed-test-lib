package fr.bmartel.speedtest;

import java.util.Random;

/**
 * Generate Random byte array for randomly generated uploaded file
 *
 * @author Bertrand Martel
 */
public class RandomGen {

    private static final byte[] symbols;
    private final static int SYMBOLS_TABLE_LENGTH = 255;
    private final static int MINIMUM_LENGTH = 1;

    static {
        symbols = new byte[SYMBOLS_TABLE_LENGTH];
        for (int i = 0; i < SYMBOLS_TABLE_LENGTH; i++) {
            symbols[i] = (byte) i;
        }
    }

    private final Random random = new Random();

    private final byte[] buf;

    public RandomGen(int length) {
        if (length < MINIMUM_LENGTH) {
            throw new IllegalArgumentException("length < " + MINIMUM_LENGTH + ": " + length);
        }
        buf = new byte[length];
    }

    public byte[] nextArray() {
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[random.nextInt(symbols.length)];
        }
        return buf;
    }
}
