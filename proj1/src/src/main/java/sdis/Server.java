package sdis;

import sdis.server.Address;
import sdis.server.MulticastListener;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server extends UnicastRemoteObject implements RemoteInterface  {
    String accessPoint;
    String version;
    long peerId;
    MulticastListener mc;
    MulticastListener mdb;
    MulticastListener mdr;

    public Server(String version, long peerId, String accessPoint, Address mc, Address mdb, Address mdr) throws RemoteException {
        super(0);
        this.accessPoint = accessPoint;
        this.version = version;
        this.peerId = peerId;

        this.mc = new MulticastListener(mc.port, mc.address, 64500,64000);
        this.mdb = new MulticastListener(mdb.port, mdb.address, 64500,64000);
        this.mdr = new MulticastListener(mdr.port, mdr.address, 64500,64000);
        new Thread(this.mc).start();
        new Thread(this.mdb).start();
        new Thread(this.mdr).start();
    }

    public static void main(String[] args) throws Exception{
            if (args.length < 9) {
                throw new Exception("Some args are missing!");
            }
            try {
                Integer.parseInt(args[1]);
            } catch (Exception e) {
                throw new Exception("invalid parameter " + args[1] + ", should be a valid number");
            }
            new Server(args[0], Integer.parseInt(args[1]), args[2], new Address(args[3], Integer.parseInt(args[4])), new Address(args[5], Integer.parseInt(args[6])), new Address(args[7], Integer.parseInt(args[8]))).startRemoteObject();
        }
    public String getAccessPoint() {
        return accessPoint;
    }

    public String getVersion() {
        return version;
    }

    public long getPeerId() {
        return peerId;
    }

    public void startRemoteObject(){
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
        }

        try {
            Naming.rebind(accessPoint, this);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Backup(String filename, int replicationDegree) {

    }

    @Override
    public void Restore(String filename) {

    }

    @Override
    public void Delete(String filename) {

    }

    @Override
    public void Reclaim(long spaceLeft) {

    }
}
