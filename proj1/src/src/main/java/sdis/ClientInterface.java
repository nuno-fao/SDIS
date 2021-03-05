package sdis;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    String getMessage(String arg) throws RemoteException;
}
