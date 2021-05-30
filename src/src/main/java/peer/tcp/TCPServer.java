package peer.tcp;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;

public class TCPServer {
    SSLServerSocket serverSocket;
    SSLSocket socket;

    public TCPServer() {
        try {
            this.serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);
            this.serverSocket.setNeedClientAuth(true);
            this.serverSocket.setEnabledProtocols(this.serverSocket.getSupportedProtocols());

        } catch (IOException e) {
            System.out.println("Could not open datagram serverSocket");
            System.exit(3);
        }
    }

    public SSLSocket getSocket() {
        return this.socket;
    }

    public void start() {
        try {
            this.socket = (SSLSocket) this.serverSocket.accept();

        } catch (Exception e) {
            return;
        }
    }


    public int getPort() {
        return this.serverSocket.getLocalPort();
    }
}
