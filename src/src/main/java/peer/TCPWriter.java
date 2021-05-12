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
        this.address=address;
        this.port=port;

        try {
            socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void write(byte[] message){
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(message);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
