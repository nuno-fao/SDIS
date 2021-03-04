package com.sdis.server;

import com.sdis.ClientInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class ClientConnection extends UnicastRemoteObject implements ClientInterface {
    public String getMessage(String arg){
        return "12345"+arg;
    }

    public ClientConnection() throws RemoteException {
        super(0);
        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            //do nothing, error means registry already exists
        }


        // Bind this object instance to the name "RmiServer"
        try {
            Naming.rebind("//localhost/peerserver", this);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
