package peer;
import test.RemoteInterface;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer implements RemoteInterface {
    private static UnicastDispatcher dispatcher;
    private static int port = 6666;
    private static int peerId = 1;


    public static void main(String args[]) {
        System.setProperty("javax.net.ssl.keyStore", "keys/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");


        ExecutorService pool = Executors.newFixedThreadPool(10);



        dispatcher = new UnicastDispatcher(port, peerId);
        new Thread(dispatcher).start();

        TCPWriter writer = new TCPWriter("localhost",6666);

        writer.write(("qwertyuiopasdfghjkl").getBytes());
        writer.close();
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
