package server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Server {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java Server <remote_object_name>");
            System.exit(1);
        }

        RemoteObject rmiObject = new RemoteObject();

        //validate arguments
        try{
            RMIInterface manager = (RMIInterface) UnicastRemoteObject.exportObject(rmiObject,0);
            Registry rmiRegistry = LocateRegistry.createRegistry(1099);
            rmiRegistry.bind(args[0],manager);
            System.err.println("Server ready");
        }
        catch(RemoteException | AlreadyBoundException e){
            e.printStackTrace();
        }
    }
}