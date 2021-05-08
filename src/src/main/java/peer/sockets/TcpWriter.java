package peer.sockets;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

public class TcpWriter implements Runnable {
    String hostname;
    int port;

    SocketChannel socketChannel;
    byte[] data;
    ExecutorService pool;
    ConcurrentLinkedDeque<byte[]> stack = new ConcurrentLinkedDeque<>();

    public TcpWriter(String hostname, int port, byte[] data, ExecutorService pool) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.data = data;
        this.pool = pool;
    }


    @Override
    public void run() {
        SSLEngine engine = null;
        try {
            TcpUtils.SocketEngine s = TcpUtils.CreateTCPChannel(hostname, port, pool);
            socketChannel = s.socketChannel;
            engine = s.engine;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        ByteBuffer myAppData = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
        ByteBuffer myNetData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());


        myNetData.clear();
        myAppData.put(data);
        myAppData.flip();

        try {
            var res = engine.wrap(myAppData, myNetData);

            switch (res.getStatus()) {
                case OK:
                    myNetData.flip();
                    while (myNetData.hasRemaining()) {
                        socketChannel.write(myNetData);
                    }
                    break;
            }

        } catch (Exception e) {

        }
    }
}
