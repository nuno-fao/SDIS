package peer.sockets;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpWriter implements Runnable {
    Selector selector = Selector.open();
    String hostname;
    int port;
    SSLContext context;
    ConcurrentLinkedDeque<byte[]> stack = new ConcurrentLinkedDeque();
    AtomicBoolean exit = new AtomicBoolean(false);

    SocketChannel socketChannel;

    public TcpWriter(String hostname, int port, SSLContext context) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.context = context;
    }

    public void write(byte[] data) {
        stack.push(data);
        synchronized (stack) {
            stack.notifyAll();
        }
    }

    public void close() {
        exit.set(true);
    }

    @Override
    public void run() {
        SSLEngine engine = null;
        try {
            TcpUtils.SocketEngine s = TcpUtils.CreateTCPChannel(hostname, port, context);
            socketChannel = s.socketChannel;
            engine = s.engine;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (!exit.get()) {
            if (stack.isEmpty()) {
                try {
                    synchronized (stack) {
                        stack.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            try {
                ByteBuffer buffer = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                engine.wrap(ByteBuffer.wrap(stack.pop()), buffer);
                socketChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
