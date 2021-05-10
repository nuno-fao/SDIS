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
    private byte[] portionOfNextMessage;

    public Prehandler(SSLSocket socket, int peerId)
    {
        this.socket = socket;
        this.peerId = peerId;
        this.currentMessageSize = 512;
        this.message = new byte[currentMessageSize];
        this.portionOfNextMessage = null;
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
        while (!receivedFlag())
        {
            try
            {
                stream.readFully(this.message, currentIndex, 512);
            }
            catch (EOFException e)
            {
                if (receivedFlag())
                {
                    this.message = MessageStuffer.unstuffMessage(this.message);
                    processMessage();
                }
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
    }

    private boolean receivedFlag()
    {
        for (int i = (message.length - 1); i >= 0; i--) {
            if (message[i] == MessageStuffer.flag)
            {
                this.portionOfNextMessage = new byte[message.length - i - 1];
                System.arraycopy(message, i + 1, this.portionOfNextMessage, 0, message.length - i - 1);
                return true; 
            } 
        }
        return false;
    }

    private void processMessage()
    {
        if (this.portionOfNextMessage != null)
        {
            byte[] auxBuffer = new byte[this.message.length + this.portionOfNextMessage.length];
            System.arraycopy(this.portionOfNextMessage, 0, auxBuffer, 0, this.portionOfNextMessage.length);
            System.arraycopy(this.message, 0, auxBuffer, this.portionOfNextMessage.length, this.message.length);
            this.message = auxBuffer;
        }

        Handler handler = new Handler(this.message, this.peerId);
        handler.processMessage();
    }
    
}
