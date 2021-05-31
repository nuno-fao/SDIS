package peer;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class UnicastDispatcher implements Runnable {
    private SSLServerSocket serverSocket;
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private int peerId;
    private Chord chord;
    private ConcurrentHashMap<String, File> localFiles;
    private ConcurrentHashMap<String, File> localCopies;
    private AtomicLong maxSize;
    private AtomicLong currentSize;
    private CopyOnWriteArraySet<BigInteger> notStoredFiles;
    private ConcurrentHashMap<String, Boolean> receivedMessages = new ConcurrentHashMap<>();

    public UnicastDispatcher(int port, int peerId, Chord chord, ConcurrentHashMap<String, File> localFiles, ConcurrentHashMap<String, File> localCopies, AtomicLong maxSize, AtomicLong currentSize, CopyOnWriteArraySet<BigInteger> notStoredFiles) {
        this.peerId = peerId;
        this.chord = chord;
        this.localFiles = localFiles;
        this.localCopies = localCopies;
        this.maxSize = maxSize;
        this.currentSize = currentSize;
        this.notStoredFiles = notStoredFiles;
        try {
            this.serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(port);
            this.serverSocket.setNeedClientAuth(true);
            this.serverSocket.setEnabledProtocols(this.serverSocket.getSupportedProtocols());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not open datagram serverSocket");
            System.exit(3);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                SSLSocket socket;
                socket = (SSLSocket) this.serverSocket.accept();
                this.pool.execute(new PreHandler(socket, this.peerId, this.chord, this.localFiles, this.localCopies, this.maxSize, this.currentSize, this.receivedMessages, this.notStoredFiles));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}