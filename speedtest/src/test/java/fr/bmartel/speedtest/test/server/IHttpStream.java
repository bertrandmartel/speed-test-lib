package fr.bmartel.speedtest.test.server;

/**
 * Interface for writing http data frame
 *
 * @author Bertrand Martel
 */
public interface IHttpStream {

    /**
     * Write http request frame
     *
     * @param data data to be written
     * @return 0 if OK -1 if error
     */
    int writeHttpFrame(byte[] data);

}