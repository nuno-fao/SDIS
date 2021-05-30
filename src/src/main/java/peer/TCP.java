package peer;


import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;

import static java.lang.Thread.sleep;

public class TCP {
    private SSLSocket socket;
    private String address;
    private int port;

    public TCP(String address, int port) {
        this.address = address;
        this.port = port;

        try {
            this.socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public SSLSocket getSocket() {
        return socket;
    }

    public TCP(String address, int port, boolean shouldNotCatchExceptions) throws IOException {
        this.address = address;
        this.port = port;

        this.socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, port);
    }

    public static boolean IsAlive(Address address) {
        try {
            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address.address, address.port);
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    public void write(byte[] message) {
        try {
            DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
            out.write(message);
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] message, boolean shouldThrow) throws IOException{
        DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
        out.write(message);
        try {
            sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out.close();
    }

    public void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
