package server;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public class Server {
    private static ConcurrentHashMap<String,String> ipTable;
    private static int serverPort, mcastPort;
    private static String mcastAdress;
    private static MulticastSocket multicastSocket; //for multicasting
    private static DatagramSocket datagramSocket; //for scheduled broadcasting

    /*private static String processRequest(String request){
        String[] requestArgs = request.split(" ");

        if(requestArgs.length < 2){
            return "-1";
        }
        else if(requestArgs[0].equals("register")){
            IPTable.put(requestArgs[1],requestArgs[2]);
            return valueOf(IPTable.size());
        }
        else if(requestArgs[0].equals("lookup")){
            String lookupResult = IPTable.get(requestArgs[1]);
            return Objects.requireNonNullElse(lookupResult, "-1");
        }
        return "-1";
    }*/

    private static ScheduledExecutorService scheduledBroadcast(){

    }


    public static void main(String[] args) {

        //read arguments
        if(args.length != 3){
            System.out.println("Usage: java Server <srvc_port> <mcast_addr> <mcast_port>");
            System.exit(1);
        }
        try{
            serverPort = Integer.parseInt(args[0]);
            mcastAdress = args[1];
            mcastPort = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e){
            System.out.println("Invalid arguments");
            System.exit(2);
        }

        //create sockets and IP table
        try{
            multicastSocket = new MulticastSocket(mcastPort);
            datagramSocket = new DatagramSocket(serverPort);
        }
        catch (IOException e){
            System.out.println("Failed to open sockets");
            System.exit(3);
        }

        ipTable = new ConcurrentHashMap<String,String>();

        ScheduledExecutorService executorService;


    }
}
