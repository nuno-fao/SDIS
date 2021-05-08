package peer;

import peer.sockets.TcpListener;
import peer.sockets.TcpUtils;
import peer.sockets.TcpWriter;
import test.RemoteInterface;

import javax.net.ssl.SSLContext;
import java.rmi.RemoteException;

public class Peer implements RemoteInterface {

    public static void main(String args[]) {
        try {
            SSLContext context = TcpUtils.GetContext("keys/truststore", "keys/server.keys", "123456");
            try {
                TcpListener tcpListener = new TcpListener(context);
                Thread listener = new Thread(tcpListener);
                listener.start();
                tcpListener.addListener("localhost", 34567);
            } catch (Exception e) {
                e.printStackTrace();
            }

            SSLContext ncontext = TcpUtils.GetContext("keys/truststore", "keys/client.keys", "123456");
            TcpWriter tcpWriter = new TcpWriter("localhost", 34567, ncontext);
            Thread writer = new Thread(tcpWriter);
            writer.start();
            tcpWriter.write("ola".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String Backup(String filename, int replicationDegree) throws RemoteException {
        return null;
    }

    @Override
    public boolean Restore(String filename) throws RemoteException {
        return false;
    }

    @Override
    public boolean Delete(String filename) throws RemoteException {
        return false;
    }

    @Override
    public void Reclaim(long spaceLeft) throws RemoteException {

    }

    @Override
    public String State() throws RemoteException {
        return null;
    }
}
