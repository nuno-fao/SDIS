package test;

import peer.Address;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    String Backup(String filename, int replicationDegree) throws RemoteException, IOException;

    boolean Restore(String filename) throws IOException;

    boolean Delete(String filename) throws RemoteException;

    void Reclaim(long spaceLeft) throws IOException;

    String State() throws RemoteException;
}
