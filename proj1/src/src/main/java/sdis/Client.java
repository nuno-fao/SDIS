package sdis;

import java.rmi.Naming;

public class Client {
    public static void main(String[] args) throws Exception{
        RemoteInterface server = (RemoteInterface) Naming.lookup("peerserver");
    }
}
