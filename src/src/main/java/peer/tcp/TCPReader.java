package peer.tcp;


import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;

public class TCPReader {
    private SSLSocket socket;
    private String address;
    private int port;

    public TCPReader(String address, int port) {
        this.address = address;
        this.port = port;

        try {
            this.socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public SSLSocket getSocket() {
        return this.socket;
    }

    public byte[] read() throws IOException {
        StringBuilder b = new StringBuilder("");
        while (true) {
            try {
                b.append(this.socket.getInputStream().readAllBytes());
            } catch (Exception e) {
                e.printStackTrace();
                return b.toString().getBytes();
            }
        }

    }

    public void close() {
    }
}
