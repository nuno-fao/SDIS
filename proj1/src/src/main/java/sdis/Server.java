package sdis;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server extends UnicastRemoteObject implements RemoteInterface  {
    String accessPoint;
    String version;
    long peerId;
    ThreadPoolExecutor cachedThreadPool;
    ThreadPoolExecutor scheduledThreadPool;

    public Server(String accessPoint,String version,long peerId) throws RemoteException {
        super(0);
        cachedThreadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        scheduledThreadPool = (ThreadPoolExecutor) Executors.newScheduledThreadPool(10);

        this.accessPoint = accessPoint;
        this.version = version;
        this.peerId = peerId;
    }

    public static void main(String[] args) throws Exception {
        if(args.length < 4){
            throw new Exception("Some args are missing!");
        }
        try{
            Integer.parseInt(args[3]);
        }
        catch (Exception e){
            throw new Exception("invalid parameter "+args[3]+", should be a valid number");
        }
        new Server(args[1],args[2],Integer.parseInt(args[3])).startRemoteObject();
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
