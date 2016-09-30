package fr.bmartel.speedtest.examples.server;

import fr.bmartel.protocol.http.inter.IHttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

/**
 * Http custom event : notify anybody that add that event listener to server
 * list of http request / response.
 *
 * @author Bertrand Martel
 */
public interface IHttpServerEventListener {

    /**
     * called wen server is started.
     */
    void onServerStarted();

    /**
     * called when Http frame received from inputstream.
     *
     * @param httpFrame       http frame object
     * @param receptionStates reception decoding states (should be HTTP_FRAME_OK if no
     *                        decoding error happened)
     */
    void onHttpFrameReceived(IHttpFrame httpFrame,
                             HttpStates receptionStates, IHttpStream httpStream);

}