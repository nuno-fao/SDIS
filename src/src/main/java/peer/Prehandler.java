package peer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;

public class Prehandler implements Runnable {
    private SSLSocket socket;
    private int peerId;
    private byte[] message;
    private int currentMessageSize;
    private int actualMessageSize;

    public Prehandler(SSLSocket socket, int peerId)
    {
        this.socket = socket;
        this.peerId = peerId;
        this.currentMessageSize = 512;
        this.message = new byte[currentMessageSize];
        this.actualMessageSize = 0;
    }

    @Override
    public void run() {
        DataInputStream stream = null;
        try
        {
            stream = new DataInputStream(this.socket.getInputStream());
        }
        catch (IOException e)
        {
            System.out.println("Failed to read from socket in Prehandler!");
            e.printStackTrace();
        }

        int currentIndex = 0;
        
        int numReadBytes;
        while (true)
        {
            try
            {
                numReadBytes = stream.read(this.message, currentIndex, 512);
            }
            catch (IOException e)
            {
                System.out.println("Failed to read from input stream in Prehandler!");
                e.printStackTrace();

                currentIndex += 512;
                this.currentMessageSize += 512;
                byte[] auxBuffer = new byte[this.currentMessageSize];
                System.arraycopy(this.message, 0, auxBuffer, 0, this.currentMessageSize - 512);
                this.message = auxBuffer;

                break;
            }
            if (numReadBytes == -1) break;
            this.actualMessageSize += numReadBytes;

            currentIndex += 512;
            this.currentMessageSize += 512;
            byte[] auxBuffer = new byte[this.currentMessageSize];
            System.arraycopy(this.message, 0, auxBuffer, 0, this.currentMessageSize - 512);
            this.message = auxBuffer;
        }

        processMessage();
    }


    private void processMessage()
    {
        if (this.actualMessageSize < this.message.length)
        {
            byte[] auxBuffer = new byte[this.actualMessageSize];
            System.arraycopy(this.message, 0, auxBuffer, 0,this.actualMessageSize);
            this.message = auxBuffer;
        }

        System.out.println(this.message.length);
        System.out.println(new String(this.message));
        Handler handler = new Handler(this.message, this.peerId);
        handler.processMessage();
    }
    
}