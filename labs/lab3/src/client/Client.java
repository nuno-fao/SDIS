package client;

import server.RMIInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    public static void main(String[] args) {

        //validate arguments

        if(args.length < 4){
            System.out.println("Usage: java Client <host_name> <remote_object_name> <oper> <opnd>*\n");
            System.exit(1);
        }

        try{
            Registry registry = LocateRegistry.getRegistry(args[0]);
            RMIInterface rmiInterface = (RMIInterface) registry.lookup(args[1]);
            String out;
            if(args[2].equals("register")){
                 out = rmiInterface.register(args[3],args[4]);
                System.out.println("register " + args[3] + " " + args[4] + " :: " + out);
            }
            else if(args[2].equals("lookup")){
                 out = rmiInterface.lookup(args[3]);
                 System.out.println("lookup " + args[3] + " :: " + out);
            }
        }
        catch (RemoteException | NotBoundException e){
            e.printStackTrace();
        }

    }
}