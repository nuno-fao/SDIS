package server;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Objects;

import static java.lang.String.valueOf;

public class RemoteObject implements RMIInterface{
    private static HashMap<String,String> IPTable;

    public RemoteObject() {
        IPTable = new HashMap<>();
    }

    @Override
    public String register(String dnsName, String ip) {
        IPTable.put(dnsName,ip);
        String out = valueOf(IPTable.size());
        System.out.println("register " + dnsName + " " + ip + " :: " + out);
        return out;

    }

    @Override
    public String lookup(String dnsName) {
        String lookupResult = IPTable.get(dnsName);
        String out = Objects.requireNonNullElse(lookupResult, "-1");
        System.out.println("lookup " + dnsName + " :: " + out);
        return out;
    }


}
