package fr.bmartel.speedtest.test.server;

/**
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2015 Bertrand Martel
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

import fr.bmartel.protocol.http.inter.IHttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP Server main implementation.
 *
 * @author Bertrand Martel
 */
public class HttpServer implements IHttpServer, IHttpServerEventListener {

    /**
     * boolean loop control for server instance running.
     */
    private volatile boolean running = true;

    /**
     * define which port we use for connection.
     */
    private final int port;

    /**
     * set ssl encryption or not.
     */
    private boolean ssl;

    private boolean isServerClosed;

    /**
     * keystore certificate type.
     */
    private String keystoreDefaultType = "";

    /**
     * trustore certificate type.
     */
    private String trustoreDefaultType = "";

    /**
     * keystore file path.
     */
    private String keystoreFile = "";

    /**
     * trustore file path.
     */
    private String trustoreFile = "";

    /**
     * ssl protocol used.
     */
    private String sslProtocol = "";

    /**
     * keystore file password.
     */
    private String keystorePassword = "";

    /**
     * trustore file password.
     */
    private String trustorePassword = "";

    /**
     * define server socket object.
     */
    private ServerSocket serverSocket;

    /**
     * server event listener.
     */
    private final List<IHttpServerEventListener> serverEventListenerList = new ArrayList<IHttpServerEventListener>();

    /**
     * Initialize server.
     */
    public HttpServer(final int port) {
        this.port = port;
    }

    /**
     * main loop for web server running.
     */
    public void start() {
        try {
            /* server will be running while running == true */
            running = true;

            if (ssl) {

				/* initial server keystore instance */
                final KeyStore keystore = KeyStore.getInstance(keystoreDefaultType);

				/* load keystore from file */
                keystore.load(new FileInputStream(keystoreFile),
                        keystorePassword.toCharArray());

				/*
                 * assign a new keystore containing all certificated to be
				 * trusted
				 */
                final KeyStore tks = KeyStore.getInstance(trustoreDefaultType);

				/* load this keystore from file */
                tks.load(new FileInputStream(trustoreFile),
                        trustorePassword.toCharArray());

				/* initialize key manager factory with chosen algorithm */
                final KeyManagerFactory kmf = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());

				/* initialize trust manager factory with chosen algorithm */
                final TrustManagerFactory tmf;

                tmf = TrustManagerFactory.getInstance(TrustManagerFactory
                        .getDefaultAlgorithm());

				/* initialize key manager factory with initial keystore */
                kmf.init(keystore, keystorePassword.toCharArray());

				/*
                 * initialize trust manager factory with keystore containing
				 * certificates to be trusted
				 */
                tmf.init(tks);

				/* get SSL context chosen algorithm */
                final SSLContext ctx = SSLContext.getInstance(sslProtocol);

				/*
                 * initialize SSL context with key manager and trust managers
				 */
                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                final SSLServerSocketFactory sslserversocketfactory = ctx
                        .getServerSocketFactory();

                serverSocket = sslserversocketfactory.createServerSocket(port);

            } else {
                serverSocket = new ServerSocket(port);
            }

            for (int i = 0; i < serverEventListenerList.size(); i++) {
                serverEventListenerList.get(i).onServerStarted();
            }

			/*
             * server thread main loop : accept a new connect each time
			 * requested by correct client
			 */
            while (running) {
                final Socket newSocketConnection = serverSocket.accept();

                newSocketConnection.setKeepAlive(true);
                final ServerSocketChannel server = new ServerSocketChannel(
                        newSocketConnection, this);

                final Thread newSocket = new Thread(server);
                newSocket.start();
            }
            /* close server socket safely */
            serverSocket.close();
        } catch (SocketException e) {
            //e.printStackTrace();
            /* stop all thread and server socket */
            stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Set ssl parameters.
     *
     * @param keystoreDefaultType keystore certificates type
     * @param trustoreDefaultType trustore certificates type
     * @param keystoreFile        keystore file path
     * @param trustoreFile        trustore file path
     * @param sslProtocol         ssl protocol used
     * @param keystorePassword    keystore password
     * @param trustorePassword    trustore password
     */
    public void setSSLParams(final String keystoreDefaultType,
                             final String trustoreDefaultType,
                             final String keystoreFile,
                             final String trustoreFile,
                             final String sslProtocol,
                             final String keystorePassword,
                             final String trustorePassword) {
        this.keystoreDefaultType = keystoreDefaultType;
        this.trustoreDefaultType = trustoreDefaultType;
        this.keystoreFile = keystoreFile;
        this.trustoreFile = trustoreFile;
        this.sslProtocol = sslProtocol;
        this.keystorePassword = keystorePassword;
        this.trustorePassword = trustorePassword;
    }

    /**
     * Stop server socket and stop running thread.
     */
    private void stop() {
        /* disable loop */
        running = false;

        if (!isServerClosed) {
            isServerClosed = true;

			/* close socket connection */
            closeServerSocket();
        }

    }

    /**
     * Stop server socket.
     */
    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void closeServer() {
        stop();
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(final boolean ssl) {
        this.ssl = ssl;
    }

    @Override
    public void addServerEventListener(final IHttpServerEventListener listener) {
        serverEventListenerList.add(listener);
    }

    @Override
    public void onServerStarted() {

    }

    @Override
    public void onHttpFrameReceived(final IHttpFrame httpFrame,
                                    final HttpStates receptionStates, final IHttpStream httpStream) {
        for (int i = 0; i < serverEventListenerList.size(); i++) {
            serverEventListenerList.get(i).onHttpFrameReceived(httpFrame,
                    receptionStates, httpStream);
        }
    }
}