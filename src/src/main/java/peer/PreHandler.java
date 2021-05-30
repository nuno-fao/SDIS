package peer;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PreHandler implements Runnable {
    private SSLSocket socket;
    private int peerId;
    private byte[] message;
    private Chord chord;
    private ConcurrentHashMap<String, File> localFiles;
    private ConcurrentHashMap<String, File> localCopies;
    private AtomicLong maxSize;
    private AtomicLong currentSize;

    public PreHandler(SSLSocket socket, int peerId, Chord chord, ConcurrentHashMap<String, File> localFiles, ConcurrentHashMap<String, File> localCopies, AtomicLong maxSize, AtomicLong currentSize) {
        this.socket = socket;
        this.peerId = peerId;
        this.chord = chord;
        this.localFiles = localFiles;
        this.localCopies = localCopies;
        this.maxSize = maxSize;
        this.currentSize = currentSize;
    }

    @Override
    public void run() {
        try {
            // open streams
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);

            String requestString = in.readLine();

            out.println("ok");

            // close streams
            this.socket.shutdownOutput();
            while (in.readLine() != null) ;
            out.close();
            in.close();

            this.socket.close();
            this.message = requestString.getBytes();
        } catch (Exception e) {
            return;
        }
        this.processMessage();
    }


    private void processMessage() {
        Handler handler = new Handler(this.message, this.peerId, this.chord, new Address(this.socket.getLocalAddress().getHostAddress(), 0), this.localFiles, this.localCopies, this.maxSize, this.currentSize);
        handler.processMessage();
    }

}