package peer;

import peer.sockets.TcpListener;
import peer.sockets.TcpUtils;
import peer.sockets.TcpWriter;
import test.RemoteInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

public class Peer implements RemoteInterface {

    public static void main(String args[]) {


        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            TcpUtils.GetContext("keys/truststore", "keys/client.keys", "123456");
            try {
                TcpListener tcpListener = new TcpListener();
                Thread listener = new Thread(tcpListener);
                tcpListener.addListener("localhost", 34567);
                listener.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            pool.execute(() -> {
                try {
                    pool.execute(new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool));
                    pool.execute(new TcpWriter("localhost", 34567, "trtsfddddddddddddddrtr".getBytes(), pool));
                    pool.execute(new TcpWriter("localhost", 34567, "trtsdffffrtr".getBytes(), pool));
                    pool.execute(new TcpWriter("localhost", 34567, "trsfdddddddddddddddddddddddddddddddddddddtrtr".getBytes(), pool));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            sleep(20000);
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
