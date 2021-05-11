package peer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import javax.net.ssl.SSLSocket;

public class Prehandler implements Runnable {
    private SSLSocket socket;
    private int peerId;
    private byte[] message;
    private int currentMessageSize;

    public Prehandler(SSLSocket socket, int peerId)
    {
        this.socket = socket;
        this.peerId = peerId;
        this.currentMessageSize = 512;
        this.message = new byte[currentMessageSize];
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
        while (true)
        {
            try
            {
                stream.readFully(this.message, currentIndex, 512);
            }
            catch (EOFException e)
            {
                break;
            }
            catch (IOException e)
            {
                System.out.println("Failed to read from input stream in Prehandler!");
                e.printStackTrace();
            }
            
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
        Handler handler = new Handler(this.message, this.peerId);
        handler.processMessage();
    }
    
}
