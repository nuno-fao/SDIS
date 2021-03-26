package sdis;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    void Backup(String filename, int replicationDegree)  throws RemoteException;
    void Restore(String filename)  throws RemoteException;
    void Delete(String filename)  throws RemoteException;
    void Reclaim(long spaceLeft)  throws RemoteException;
    String State()  throws RemoteException;
}
