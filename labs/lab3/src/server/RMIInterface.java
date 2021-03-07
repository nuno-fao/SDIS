package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {
    String register(String dnsName, String ip) throws RemoteException;
    String lookup(String dnsName) throws RemoteException;
}
