import java.rmi.Naming;

public class Client {
    public static void main(String[] args) throws Exception{
        ClientConnection server = (ClientConnection) Naming.lookup("//localhost/peerserver");
    }

}