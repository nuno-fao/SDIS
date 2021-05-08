package peer.sockets;

import peer.Address;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.cert.CertificateException;

public class TcpUtils {
    static String truststoreFile, keyFile, password;
    static SSLContext context;

    static public SSLEngine GetEngine(Address address) {
        return context.createSSLEngine(address.address, address.port);
    }

    static public SSLContext GetContext(String truststoreFile, String keyFile, String password) throws KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException, CertificateException, UnrecoverableKeyException {
        TcpUtils.truststoreFile = truststoreFile;
        TcpUtils.keyFile = keyFile;
        TcpUtils.password = password;
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
        return sslContext;
    }

    static public void Handshake(SocketChannel socketChannel, SSLEngine engine) throws Exception {
        SSLSession session = engine.getSession();
        ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        ByteBuffer peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());

        handler(socketChannel, engine, myNetData, peerNetData);
    }

    private static void handler(SocketChannel socketChannel, SSLEngine engine, ByteBuffer myNetData, ByteBuffer peerNetData) throws Exception {
        SSLSession session = engine.getSession();
        ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        ByteBuffer peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());


        engine.beginHandshake();
        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();

        int i = 0;
        while (true) {
            switch (hs) {
                case NOT_HANDSHAKING:
                    throw new Exception();
                case FINISHED:
                    return;
                case NEED_UNWRAP:
                    if (socketChannel.read(peerNetData) < 0) {

                    }

                    peerNetData.flip();
                    SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
                    peerNetData.compact();
                    hs = res.getHandshakeStatus();

                    switch (res.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_UNDERFLOW:
                            if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
                            } else {
                                peerNetData.clear();
                                //System.out.println(i++);
                            }
                            break;
                    }
                    break;

                case NEED_WRAP:
                    myNetData.clear();

                    res = engine.wrap(myAppData, myNetData);
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
                    break;

                case NEED_TASK:
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        new Thread(task).start();
                    }
                    hs = engine.getHandshakeStatus();
                    break;
            }
        }
    }

    static public SocketEngine CreateTCPChannel(String hostname, int port, SSLContext context) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(hostname, port));

        while (!socketChannel.finishConnect()) {
            // do something until connect completed
        }
        SSLEngine engine = context.createSSLEngine(hostname, port);
        engine.setUseClientMode(true);

        try {
            TcpUtils.Handshake(socketChannel, engine);
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