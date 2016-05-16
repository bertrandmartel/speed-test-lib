package fr.bmartel.speedtest.test;

/**
 * Created by akinaru on 16/05/16.
 */
public class TestUtils {

    public static String generateMessageHeader(Class className) {
        return "[" + className.getSimpleName() + "] ";
    }
}
