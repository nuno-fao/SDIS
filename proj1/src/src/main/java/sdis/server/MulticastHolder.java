package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastHolder implements Runnable {
    private long last = System.currentTimeMillis();
    private MulticastSocket socket;
    private int port;
    private String host;
    private InetAddress address;
    private int bufferSize;
    private int peerId;

    public MulticastHolder(int port, String host, int bufferSize, int dataSize, int peerId) {
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


    public void send(DatagramPacket packet) {
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
        byte[] buffer = new byte[this.bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, this.bufferSize);
        while (true) {
            try {
                this.socket.receive(packet);

                Server.getServer().getPool().execute(new Handler(packet, this.peerId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
