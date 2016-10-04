/*
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

package fr.bmartel.speedtest.test.server;

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
    private volatile boolean mRunning = true;

    /**
     * define which port we use for connection.
     */
    private final int mPort;

    /**
     * set SSL encryption or not.
     */
    private boolean mSsl;

    private boolean mServerClosed;

    /**
     * keystore certificate type.
     */
    private String mKeystoreDefaultType = "";

    /**
     * trustore certificate type.
     */
    private String mTrustoreDefaultType = "";

    /**
     * keystore file path.
     */
    private String mKeystoreFile = "";

    /**
     * trustore file path.
     */
    private String mTrustoreFile = "";

    /**
     * mSsl protocol used.
     */
    private String mSslProtocol = "";

    /**
     * keystore file password.
     */
    private String mKeystorePassword = "";

    /**
     * trustore file password.
     */
    private String mTrustorePassword = "";

    /**
     * define server socket object.
     */
    private ServerSocket mServerSocket;

    /**
     * server event listener.
     */
    private final List<IHttpServerEventListener> mServerEventListenerList = new ArrayList<IHttpServerEventListener>();

    /**
     * Initialize server.
     */
    public HttpServer(final int port) {
        this.mPort = port;
    }

    /**
     * main loop for web server mRunning.
     */
    public void start() {
        try {
            /* server will be mRunning while mRunning == true */
            mRunning = true;

            if (mSsl) {

				/* initial server keystore instance */
                final KeyStore keystore = KeyStore.getInstance(mKeystoreDefaultType);

				/* load keystore from file */
                keystore.load(new FileInputStream(mKeystoreFile),
                        mKeystorePassword.toCharArray());

                /*
                 * assign a new keystore containing all certificated to be
                 * trusted
                 */
                final KeyStore tks = KeyStore.getInstance(mTrustoreDefaultType);

				/* load this keystore from file */
                tks.load(new FileInputStream(mTrustoreFile),
                        mTrustorePassword.toCharArray());

				/* initialize key manager factory with chosen algorithm */
                final KeyManagerFactory kmf = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());

				/* initialize trust manager factory with chosen algorithm */
                final TrustManagerFactory tmf;

                tmf = TrustManagerFactory.getInstance(TrustManagerFactory
                        .getDefaultAlgorithm());

				/* initialize key manager factory with initial keystore */
                kmf.init(keystore, mKeystorePassword.toCharArray());

				/*
                 * initialize trust manager factory with keystore containing
				 * certificates to be trusted
				 */
                tmf.init(tks);

				/* get SSL context chosen algorithm */
                final SSLContext ctx = SSLContext.getInstance(mSslProtocol);

				/*
                 * initialize SSL context with key manager and trust managers
				 */
                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                final SSLServerSocketFactory sslserversocketfactory = ctx
                        .getServerSocketFactory();

                mServerSocket = sslserversocketfactory.createServerSocket(mPort);

            } else {
                mServerSocket = new ServerSocket(mPort);
            }

            for (int i = 0; i < mServerEventListenerList.size(); i++) {
                mServerEventListenerList.get(i).onServerStarted();
            }

			/*
             * server thread main loop : accept a new connect each time
			 * requested by correct client
			 */
            while (mRunning) {
                final Socket newSocketConnection = mServerSocket.accept();

                newSocketConnection.setKeepAlive(true);
                final ServerSocketChannel server = new ServerSocketChannel(
                        newSocketConnection, this);

                final Thread newSocket = new Thread(server);
                newSocket.start();
            }
            /* close server socket safely */
            mServerSocket.close();
        } catch (SocketException e) {
            //e.printStackTrace();
            /* stop all thread and server socket */
            stop();
        } catch (IOException e) {
            //e.printStackTrace();
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
     * Set mSsl parameters.
     *
     * @param keystoreDefaultType keystore certificates type
     * @param trustoreDefaultType trustore certificates type
     * @param keystoreFile        keystore file path
     * @param trustoreFile        trustore file path
     * @param sslProtocol         mSsl protocol used
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
        this.mKeystoreDefaultType = keystoreDefaultType;
        this.mTrustoreDefaultType = trustoreDefaultType;
        this.mKeystoreFile = keystoreFile;
        this.mTrustoreFile = trustoreFile;
        this.mSslProtocol = sslProtocol;
        this.mKeystorePassword = keystorePassword;
        this.mTrustorePassword = trustorePassword;
    }

    /**
     * Stop server socket and stop mRunning thread.
     */
    private void stop() {
        /* disable loop */
        mRunning = false;

        if (!mServerClosed) {
            mServerClosed = true;

			/* close socket connection */
            closeServerSocket();
        }

    }

    /**
     * Stop server socket.
     */
    private void closeServerSocket() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
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
        return mSsl;
    }

    public void setSsl(final boolean mSsl) {
        this.mSsl = mSsl;
    }

    @Override
    public void addServerEventListener(final IHttpServerEventListener listener) {
        mServerEventListenerList.add(listener);
    }

    @Override
    public void onServerStarted() {

    }

    @Override
    public void onHttpFrameReceived(final IHttpFrame httpFrame,
                                    final HttpStates receptionStates, final IHttpStream httpStream) {
        for (int i = 0; i < mServerEventListenerList.size(); i++) {
            mServerEventListenerList.get(i).onHttpFrameReceived(httpFrame,
                    receptionStates, httpStream);
        }
    }
}