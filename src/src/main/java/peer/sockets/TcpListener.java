package peer.sockets;

import peer.Address;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class ChannelEngine {
    SSLEngine engine;
    SocketChannel socketChannel;

    public ChannelEngine(SSLEngine engine, SocketChannel socketChannel) {
        this.engine = engine;
        this.socketChannel = socketChannel;
    }
}

public class TcpListener implements Runnable {
    Selector selector;
    ConcurrentLinkedDeque<ChannelEngine> ce = new ConcurrentLinkedDeque<>();


    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public TcpListener() throws IOException, NoSuchAlgorithmException {
        this.selector = SelectorProvider.provider().openSelector();
    }

    public void addListener(String hostname, int port) throws IOException {
        System.out.println(hostname + ":" + port);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(hostname, port));
        selector.wakeup();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void addConnection(SelectionKey key) {
        try {
            SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
            socketChannel.configureBlocking(false);
            SSLEngine engine = TcpUtils.GetEngine(new Address("localhost", socketChannel.socket().getLocalPort()));
            engine.setUseClientMode(false);
            TcpUtils.Handshake(socketChannel, engine, pool);


            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        AtomicInteger i = new AtomicInteger();
        while (true) {
            try {
                this.selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        try {
                            addConnection(key);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        SSLEngine engine = (SSLEngine) key.attachment();

                        ByteBuffer peerNetData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                        ByteBuffer peerAppData = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                        pool.execute(() -> {
                            try {
                                while (true) {
                                    int n = socketChannel.read(peerNetData);
                                    if (n > 0) {

                                        peerNetData.flip();
                                        while (peerNetData.hasRemaining()) {
                                            SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);


                                            switch (result.getStatus()) {

                                                case OK:
                                                    System.out.println(i.getAndIncrement());
                                                    System.out.println(new String(peerAppData.array()));
                                                    break;
                                                case CLOSED:
                                                    engine.closeOutbound();
                                                    TcpUtils.Handshake(socketChannel, engine, pool);
                                                    socketChannel.close();
                                                    return;
                                            }
                                        }
                                    } else if (n < 0) {
                                        //todo nao funciona
                                        engine.closeInbound();
                                        engine.closeOutbound();
                                        TcpUtils.Handshake(socketChannel, engine, pool);
                                        socketChannel.close();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
    }
}


