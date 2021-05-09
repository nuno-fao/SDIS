package peer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import javax.net.ssl.SSLSocket;

public class Prehandler implements Runnable {
    private SSLSocket socket;
    private byte flag = 0x7e;
    private byte esc1 = 0x7d;
    private byte esc2 = 0x5e;
    private byte esc3 = 0x5d;
    private byte[] message;
    private int currentMessageSize;

    public Prehandler(SSLSocket socket)
    {
        this.socket = socket;
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
                    unstuffMessage();
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
            if (message[i] == flag) return true;
        }
        return false;
    }

    private void unstuffMessage()
    {
        for (int i = 0; i < this.message.length; i++) {
            if (message[i] == this.esc1 && message[i + 1] == this.esc2)
            {
                byte[] auxBuffer = new byte[this.message.length - 1];
                System.arraycopy(this.message, 0, auxBuffer, 0, i);
                auxBuffer[i] = this.flag;
                System.arraycopy(this.message, i + 2, auxBuffer, i + 1, this.message.length - i - 2);
                this.message = auxBuffer;
            }
            else if (message[i] == this.esc1 && message[i + 1] == this.esc3)
            {
                byte[] auxBuffer = new byte[this.message.length - 1];
                System.arraycopy(this.message, 0, auxBuffer, 0, i);
                auxBuffer[i] = this.esc1;
                System.arraycopy(this.message, i + 2, auxBuffer, i + 1, this.message.length - i - 2);
                this.message = auxBuffer;
            }
        }
    }

    private void processMessage()
    {
        //TODO
        //Implement this
    }
    
}
