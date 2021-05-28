package test;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    String Backup(String filename, int replicationDegree) throws RemoteException;

    boolean Restore(String filename) throws RemoteException;

    boolean Delete(String filename) throws RemoteException;

    void Reclaim(long spaceLeft) throws RemoteException;

    String State() throws RemoteException;
}
