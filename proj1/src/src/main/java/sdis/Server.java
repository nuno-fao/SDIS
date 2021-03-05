package sdis;

import sdis.server.ClientConnection;

import java.rmi.RemoteException;
import java.util.Map;

public class Server {
    long maxSpace;

    public static void main(String[] args) throws RemoteException {
        new ClientConnection();
        System.out.println("Running...");
    }
}
