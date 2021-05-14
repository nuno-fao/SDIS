package peer;


import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;

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
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
