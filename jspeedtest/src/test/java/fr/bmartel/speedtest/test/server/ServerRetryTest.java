package fr.bmartel.speedtest.test.server;

import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.test.AbstractTest;
import org.junit.Before;

/**
 * Abstract class for Server test.
 *
 * @author Bertrand Martel
 */
public class ServerRetryTest extends AbstractTest {

    /**
     * http server.
     */
    protected static HttpServer mServer;

    /**
     * Stop server.
     */
    protected void stopServer() {
        if (mServer != null) {
            mServer.closeServer();
        }
    }

    @Before
    public void clear() {
        if (mSocket != null) {
            mSocket.clearListeners();
        }
        mSocket = new SpeedTestSocket();
        stopServer();
    }
}
