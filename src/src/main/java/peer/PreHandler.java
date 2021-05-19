package peer;

import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.IOException;

public class PreHandler implements Runnable {
    private static final int bufferSize = 512;
    private SSLSocket socket;
    private int peerId;
    private byte[] message;
    private int currentMessageSize;
    private int actualMessageSize;
    private Chord chord;

    public PreHandler(SSLSocket socket, int peerId, Chord chord) {
        this.socket = socket;
        this.peerId = peerId;
        this.currentMessageSize = bufferSize;
        this.message = new byte[this.currentMessageSize];
        this.actualMessageSize = 0;
        this.chord = chord;
    }

    @Override
    public void run() {
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Failed to read from socket in Prehandler!");
            e.printStackTrace();
        }

        int currentIndex = 0;

        int numReadBytes;
        while (true) {
            try {
                numReadBytes = stream.read(this.message, currentIndex, bufferSize);
            } catch (IOException e) {
                System.out.println("Failed to read from input stream in Prehandler!");
                e.printStackTrace();

                this.currentMessageSize += bufferSize;
                byte[] auxBuffer = new byte[this.currentMessageSize];
                System.arraycopy(this.message, 0, auxBuffer, 0, this.currentMessageSize - bufferSize);
                this.message = auxBuffer;

                break;
            }
            if (numReadBytes == -1) break;
            this.actualMessageSize += numReadBytes;

            currentIndex += bufferSize;
            this.currentMessageSize += bufferSize;
            byte[] auxBuffer = new byte[this.currentMessageSize];
            System.arraycopy(this.message, 0, auxBuffer, 0, this.currentMessageSize - bufferSize);
            this.message = auxBuffer;
        }

        this.processMessage();
    }


    private void processMessage() {
        try
        {
            this.socket.shutdownOutput();
            this.socket.close(); 
        }
        catch (IOException e)
        {
            System.out.println("Couldn't close socket in Prehandler!");
            e.printStackTrace();
        }

        if (this.actualMessageSize < this.message.length) {
            byte[] auxBuffer = new byte[this.actualMessageSize];
            System.arraycopy(this.message, 0, auxBuffer, 0, this.actualMessageSize);
            this.message = auxBuffer;
        }
        Handler handler = new Handler(this.message, this.peerId, this.chord);
        handler.processMessage();
    }

}