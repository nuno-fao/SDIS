package peer.tcp;


import peer.Address;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TCPWriter {
    private SSLSocket socket;
    private String address;
    private int port;

    public TCPWriter(String address, int port) {
        this.address = address;
        this.port = port;

        try {
            this.socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public TCPWriter(String address, int port, boolean shouldNotCatchExceptions) throws IOException {
        this.address = address;
        this.port = port;

        this.socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, port);
    }

    public static boolean IsAlive(Address address) {
        try {
            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address.address, address.port);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void write(byte[] message) {
        Executors.newSingleThreadScheduledExecutor().schedule(new Thread(() -> {
            try {
                PrintWriter out; // output stream
                BufferedReader in; // input stream
                out = new PrintWriter(this.socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

                out.println(new String(message));

                try {
                    in.readLine();
                } catch (IOException e) {
                }

                this.socket.shutdownOutput();
                while (in.readLine() != null) ;
                out.close();
                in.close();

                this.socket.close();
            } catch (Exception e) {
            }
        }), new Random().nextInt(20), TimeUnit.MILLISECONDS);
    }

    public void write(byte[] message, boolean shouldThrow) throws IOException {
        this.write(message);
    }

    public void close() {
    }

    public void burn(){
        try{
        this.socket.close();
        }
        catch(Exception e){
            
        }
    }
}
