package peer.sockets;

import peer.Address;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;

public class TcpUtils {
    static String truststoreFile, keyFile, password;
    static SSLContext context = null;

    static public SSLEngine GetEngine(Address address) {
        SSLEngine engine = null;
        if (context == null) {
            try {
                char[] passphrase = password.toCharArray();

                // First initialize the key and trust material
                KeyStore ksKeys = KeyStore.getInstance("JKS");
                ksKeys.load(new FileInputStream(keyFile), passphrase);
                KeyStore ksTrust = KeyStore.getInstance("JKS");
                ksTrust.load(new FileInputStream(truststoreFile), passphrase);

                // KeyManagers decide which key material to use
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
                kmf.init(ksKeys, passphrase);

                // TrustManagers decide whether to allow connections
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
                tmf.init(ksTrust);

                // Get an instance of SSLContext for TLS protocols
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                TcpUtils.context = sslContext;

            } catch (Exception e) {

            }
        }
        engine = context.createSSLEngine(address.address, address.port);

        return engine;
    }

    static public void GetContext(String truststoreFile, String keyFile, String password) {
        TcpUtils.truststoreFile = truststoreFile;
        TcpUtils.keyFile = keyFile;
        TcpUtils.password = password;
    }

    static public void Handshake(SocketChannel socketChannel, SSLEngine engine, ExecutorService pool) throws Exception {
        SSLSession session = engine.getSession();
        ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize() + 500);
        ByteBuffer peerNetData = ByteBuffer.allocate(session.getPacketBufferSize() + 500);

        handler(socketChannel, engine, myNetData, peerNetData, pool);
    }

    private static void handler(SocketChannel socketChannel, SSLEngine engine, ByteBuffer myNetData, ByteBuffer peerNetData, ExecutorService pool) throws Exception {
        SSLSession session = engine.getSession();
        ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize() + 500);
        ByteBuffer peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize() + 500);


        engine.beginHandshake();
        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();

        int i = 0;
        while (true) {
            switch (hs) {
                case NOT_HANDSHAKING:
                    throw new Exception();
                case FINISHED:
                    return;
                case NEED_UNWRAP: {
                    int a = socketChannel.read(peerNetData);
                    if (a <= 0) {

                    } else {
                    }
                    peerNetData.flip();
                    SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
                    peerNetData.compact();
                    hs = res.getHandshakeStatus();

                    switch (res.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_OVERFLOW:
                            // Maybe need to enlarge the peer application data buffer.
                            if (engine.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
                                // enlarge the peer application data buffer
                            } else {
                                // compact or clear the buffer
                            }
                            // retry the operation
                            break;

                        case BUFFER_UNDERFLOW:
                            // Maybe need to enlarge the peer network packet buffer
                            if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
                                peerNetData = ByteBuffer.allocate(peerNetData.capacity() * 2);
                            } else {
                            }
                            // obtain more inbound network data and then retry the operation
                            break;
                    }
                }
                break;

                case NEED_WRAP: {
                    myNetData.clear();

                    var res = engine.wrap(myAppData, myNetData);
                    hs = res.getHandshakeStatus();

                    switch (res.getStatus()) {
                        case OK:
                            myNetData.flip();

                            while (myNetData.hasRemaining()) {
                                socketChannel.write(myNetData);
                            }
                            break;
                        default:
                            throw new Exception();

                    }
                }
                break;

                case NEED_TASK: {
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        pool.execute(task);
                    }
                    hs = engine.getHandshakeStatus();
                }
                break;
            }
        }
    }

    static public SocketEngine CreateTCPChannel(String hostname, int port, ExecutorService pool) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(hostname, port));

        while (!socketChannel.finishConnect()) {
            // do something until connect completed
        }
        SSLEngine engine = GetEngine(new Address(hostname, port));
        engine.setUseClientMode(true);

        try {
            TcpUtils.Handshake(socketChannel, engine, pool);
        } catch (Exception e) {
            e.printStackTrace();
            socketChannel.close();
        }

        return new SocketEngine(socketChannel, engine);
    }

    public static boolean IsAlive(Address address) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(address.address, address.port));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    static class SocketEngine {
        SocketChannel socketChannel;
        SSLEngine engine;

        public SocketEngine(SocketChannel socketChannel, SSLEngine engine) {
            this.socketChannel = socketChannel;
            this.engine = engine;
        }
    }

}