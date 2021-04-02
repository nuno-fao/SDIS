package sdis.server;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastHolder implements Runnable {
    private ExecutorService pool = Executors.newCachedThreadPool();
    private MulticastSocket socket;
    private int port;
    private String host;
    private InetAddress address;
    private int bufferSize;
    private int dataSize;
    private int peerId;


    public synchronized void send(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
        }
    }



    public synchronized int getPort() {
        return port;
    }

    public synchronized InetAddress getAddress() {
        return address;
    }

    public MulticastHolder(int port, String host, int bufferSize, int dataSize,int peerId){
        this.port = port;
        this.host = host;
        try {
            this.address = InetAddress.getByName(host);
            socket = new MulticastSocket(port);
            socket.joinGroup(this.address);
            socket.setTimeToLive(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.bufferSize = bufferSize;

        this.peerId = peerId;
    }

    @Override
    public void run() {
        System.out.println(host + " Running");
        while (true) {
            byte[] buffer = new byte[bufferSize];

            DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
            try {
                socket.receive(packet);
                pool.execute(new Handler(packet,this.peerId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
