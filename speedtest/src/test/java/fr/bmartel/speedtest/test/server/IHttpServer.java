package fr.bmartel.speedtest.test.server;

/**
 * Template for http server interface featuring close and events.
 *
 * @author Bertrand Martel
 */
public interface IHttpServer {

    /**
     * close http server.
     */
    void closeServer();

    /**
     * Add Client event listener to server list for library user to be notified
     * of all server events.
     *
     * @param listener
     */
    void addServerEventListener(IHttpServerEventListener listener);

}