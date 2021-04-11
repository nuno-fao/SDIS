package test;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.List;

public class TestApp {
    public static void main(String[] args) throws Exception {
        RemoteInterface server;
        if (args.length < 2)
            throw new IllegalArgumentException("Not enough arguments");
        List<String> accessPoint = Arrays.asList(args[0].split(":"));
        if (accessPoint.size() == 1) {
            server = (RemoteInterface) Naming.lookup("//localhost/" + accessPoint.get(0));
        } else if (accessPoint.size() == 2) {
            server = (RemoteInterface) Naming.lookup("//" + accessPoint.get(0) + "/" + accessPoint.get(1));
        } else {
            throw new IllegalArgumentException("PeerAp Error");
        }
        try {
            switch (args[1]) {
                case "BACKUP": {
                    int a;
                    try {
                        a = Integer.parseInt(args[3]);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("4th argument is not a number!!!!");
                    }
                    System.out.println(server.Backup(args[2], a));
                    break;
                }
                case "RESTORE": {
                    if (!server.Restore(args[2])) {
                        System.out.println("No Such file " + args[2]);
                    }
                    break;
                }
                case "DELETE": {
                    if (!server.Delete(args[2]))
                        System.out.println("No Such file " + args[2]);
                    break;
                }
                case "RECLAIM": {
                    int a;
                    try {
                        a = Integer.parseInt(args[2]);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("4th argument is not a number!!!!");
                    }
                    server.Reclaim(a);
                    break;
                }
                case "STATE": {
                    System.out.println(server.State());
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Illegal argument" + args[1]);
                }
            }
        }
        catch (Exception e){
            System.out.println("Not enough arguments were supplied");
        }
    }
}
