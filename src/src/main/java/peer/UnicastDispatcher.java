package peer;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class UnicastDispatcher implements Runnable {
    private SSLServerSocket socket;
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private int peerId;
    private Chord chord;
    private ConcurrentHashMap<String,File> localFiles;
    private ConcurrentHashMap<String, RemoteFile> localCopies;
    private AtomicLong maxSize;
    private AtomicLong currentSize;

    public UnicastDispatcher(int port, int peerId, Chord chord, ConcurrentHashMap<String,File> localFiles, ConcurrentHashMap<String,RemoteFile> localCopies, AtomicLong maxSize, AtomicLong currentSize) {
        this.peerId = peerId;
        this.chord = chord;
        this.localFiles = localFiles;
        this.localCopies = localCopies;
        this.maxSize = maxSize;
        this.currentSize = currentSize;

        try {
            this.socket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(port);
        } catch (IOException e) {
            System.out.println("Failed to create SSL socket!");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        SSLSocket clientSocket;

        while (true) {
            try {
                clientSocket = (SSLSocket) this.socket.accept();
                this.pool.execute(new PreHandler(clientSocket, this.peerId, this.chord, localFiles, localCopies,maxSize,currentSize));
            } catch (IOException e) {
                System.out.println("Failed to accept a new connection!");
                e.printStackTrace();
            }
        }
    }
}