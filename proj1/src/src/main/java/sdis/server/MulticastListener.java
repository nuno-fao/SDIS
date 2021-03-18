package sdis.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastListener implements Runnable {
    private ExecutorService pool = Executors.newCachedThreadPool();
    private MulticastSocket socket;
    private int port;
    private String host;
    private int bufferSize;
    private int dataSize;

    public MulticastListener(int port, String host, int bufferSize, int dataSize){
        this.port = port;
        this.host = host;
        try {
            socket = new MulticastSocket(port);
            socket.joinGroup(InetAddress.getByName(host));
            socket.setTimeToLive(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.bufferSize = bufferSize;
        this.dataSize = dataSize;
    }

    @Override
    public void run() {
        System.out.println(host + " Running");
        while (true) {
            System.out.println(host + " Reading");
            byte[] buffer = new byte[bufferSize];

            DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
            try {
                socket.receive(packet);
                pool.execute(new Handler(packet));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
