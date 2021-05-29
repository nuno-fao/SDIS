package peer;

import test.RemoteInterface;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Socket;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer implements RemoteInterface {
    private static UnicastDispatcher dispatcher;
    private static ChordHelper chordHelper;


    public static void main(String args[]) throws IOException {
        System.setProperty("javax.net.ssl.keyStore", "keys/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        String address = args[0];
        int port = Integer.parseInt(args[1]);
        int id = 0;

        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest((address + ":" + port).getBytes());
            BigInteger num = new BigInteger(1, digest);
            id = num.mod(BigDecimal.valueOf(Math.pow(2, Chord.m)).toBigInteger()).intValue();
        }
        catch (NoSuchAlgorithmException e)
        {
            System.out.println("Invalid algorithm");
        }

        Chord chord = new Chord(id, address, port);

        dispatcher = new UnicastDispatcher(port, id, chord);
        new Thread(dispatcher).start();

        boolean needToCreateCircle = true;

        for (String arg : args) {
            if (arg.contains(":"))
            {
                chord.Join(new Node(arg));
                needToCreateCircle = false;
            } 
        }

        if (needToCreateCircle) chord.Create();

        ExecutorService pool = Executors.newFixedThreadPool(10);

        chordHelper = new ChordHelper(chord);
        new Thread(chordHelper).start();

        System.out.println(port);
        if(port == 8000){
            SSLServerSocket s = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);
            TCP t = new TCP(address,8001);
            t.write(MessageType.createPutFile("3",address, String.valueOf(s.getLocalPort()),1));

            SSLSocket clientSocket;
            clientSocket = (SSLSocket) s.accept();

            System.out.println("is about to write");
            clientSocket.getOutputStream().write("dsadsaasadsdsaadsdsadsadsa".getBytes());
        }
        else {
        }

    }

    @Override
    public String Backup(String filename, int replicationDegree) throws RemoteException {
        return null;
    }

    @Override
    public boolean Restore(String filename) throws RemoteException {
        return false;
    }

    @Override
    public boolean Delete(String filename) throws RemoteException {
        return false;
    }

    @Override
    public void Reclaim(long spaceLeft) throws RemoteException {

    }

    @Override
    public String State() throws RemoteException {
        return null;
    }
}
