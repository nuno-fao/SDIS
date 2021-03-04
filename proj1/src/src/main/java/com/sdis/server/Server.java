import java.rmi.RemoteException;

public class Server {
    public static void main(String[] args) throws RemoteException {
        new ClientConnection();
    }
}
