package peer;

import peer.sockets.TcpListener;
import peer.sockets.TcpUtils;
import peer.sockets.TcpWriter;
import test.RemoteInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer implements RemoteInterface {

    public static void main(String args[]) {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        TcpUtils.GetContext("keys/truststore", "keys/client.keys", "123456");

        if (args.length == 0 || args[0].equals("s")) {
            try {
                TcpListener tcpListener = new TcpListener();
                Thread listener = new Thread(tcpListener);
                tcpListener.addListener("localhost", 34567);
                listener.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (args.length == 0 || args[0].equals("p")) {
            pool.execute(() -> {
                try {
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();
                    new TcpWriter("localhost", 34567, "trtrdffdsfdsdfdstr".getBytes(), pool).run();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
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
