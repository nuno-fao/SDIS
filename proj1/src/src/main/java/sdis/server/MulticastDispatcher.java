package sdis.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastDispatcher implements Runnable {
    private long last = System.currentTimeMillis();
    private MulticastSocket socket;
    private int port;
    private String host;
    private InetAddress address;
    private int bufferSize;
    private int peerId;
    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public MulticastDispatcher(int port, String host, int bufferSize, int dataSize, int peerId) {
        this.port = port;
        this.host = host;
        try {
            this.address = InetAddress.getByName(host);
            this.socket = new MulticastSocket(port);
            this.socket.joinGroup(this.address);
            this.socket.setTimeToLive(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.bufferSize = bufferSize;

        this.peerId = peerId;
    }


    public synchronized void send(DatagramPacket packet) {
        try {
            this.socket.send(packet);
        } catch (IOException e) {
        }
    }

    public int getPort() {
        return this.port;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    @Override
    public void run() {
        System.out.println(this.host + " Running");
        while (true) {
            try {
                byte[] buffer = new byte[this.bufferSize];
                DatagramPacket packet = new DatagramPacket(buffer, this.bufferSize);
                this.socket.receive(packet);

                this.pool.execute(new Handler(packet, this.peerId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
