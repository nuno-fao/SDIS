package peer.sockets;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpListener implements Runnable {
    Selector selector;
    SSLContext context;
    ConcurrentLinkedDeque<ServerSocketChannel> serverSocketChannels = new ConcurrentLinkedDeque<>();
    ConcurrentLinkedDeque<ChannelEngine> channelEngines = new ConcurrentLinkedDeque<>();


    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public TcpListener(SSLContext context) throws IOException, NoSuchAlgorithmException {
        this.selector = SelectorProvider.provider().openSelector();
        this.context = context;
    }

    public void addListener(String hostname, int port) throws IOException {
        System.out.println(hostname + ":" + port);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(hostname, port));
        serverSocketChannels.push(serverSocketChannel);
        selector.wakeup();
    }

    private void addConnection(SelectionKey key) throws Exception {
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        pool.execute(() -> {
            SSLEngine engine = context.createSSLEngine("localhost", socketChannel.socket().getLocalPort());
            engine.setUseClientMode(false);
            try {
                TcpUtils.Handshake(socketChannel, engine);
            } catch (Exception e) {
                e.printStackTrace();
            }
            channelEngines.push(new ChannelEngine(socketChannel, engine));
            selector.wakeup();
        });
    }

    @Override
    public void run() {
        while (true) {
            while (!serverSocketChannels.isEmpty()) {
                try {
                    serverSocketChannels.pop().register(selector, SelectionKey.OP_ACCEPT);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            }
            while (!channelEngines.isEmpty()) {
                try {
                    ChannelEngine channelEngine = channelEngines.pop();
                    channelEngine.channel.register(selector, SelectionKey.OP_READ, channelEngine.engine);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            }
            try {
                this.selector.select();
            } catch (IOException e) {
                continue;
            }
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid()) {
                    continue;
                }

                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        try {
                            addConnection(key);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (key.isReadable()) {
                        key.cancel();

                        SocketChannel socketChannel = ((SocketChannel) key.channel());
                        System.out.println("Successfully connected to: " + socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getLocalPort());

                    }
                }
            }
        }
    }

    private class ChannelEngine {
        SocketChannel channel;
        SSLEngine engine;

        public ChannelEngine(SocketChannel channel, SSLEngine engine) {
            this.channel = channel;
            this.engine = engine;
        }
    }
}


