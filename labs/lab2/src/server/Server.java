package server;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private static ConcurrentHashMap<String,String> ipTable;
    private static int serverPort, mcastPort;
    private static String mcastAdress;
    private static MulticastSocket multicastSocket;
    private static DatagramSocket datagramSocket;

    private static ScheduledExecutorService scheduledBroadcast(){
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

        executor.scheduleAtFixedRate(
                new Runnable() {
                     public void run() {
                         byte[] content = Integer.toString(serverPort).getBytes();
                         try {
                             DatagramPacket mcast_packet = new DatagramPacket(content, content.length, InetAddress.getByName(mcastAdress), mcastPort);

                             String hostAddress = InetAddress.getLocalHost().getHostAddress();
                             System.out.println("multicast: " + mcastAdress + " " + String.valueOf(mcastPort) + ": " + hostAddress + " " + String.valueOf(serverPort));

                             multicastSocket.setTimeToLive(1);
                             multicastSocket.send(mcast_packet);
                         } catch (IOException e) {
                             System.out.println("Error multicasting packet");
                         }
                     }
                 },
                0,
                1,
                TimeUnit.SECONDS);
        return  executor;
    }

    private static String processRequest(String request){
        String[] requestArgs = request.split(" ");

        if(requestArgs.length < 2){
            return "-1";
        }
        else if(requestArgs[0].equals("register")){
            ipTable.put(requestArgs[1],requestArgs[2]);
            return String.valueOf(ipTable.size());
        }
        else if(requestArgs[0].equals("lookup")){
            String lookupResult = ipTable.get(requestArgs[1]);
            return Objects.requireNonNullElse(lookupResult, "-1");
        }
        return "-1";
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

        ScheduledExecutorService executorService = scheduledBroadcast();

        //start loop
        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while(true){
            try{
                //System.out.println("Waiting for client request...\n");
                datagramSocket.receive(packet);
                String request = new String(packet.getData());
                System.out.println("Server: " + request);
                String response = processRequest(request.trim());
                packet.setData(response.getBytes(StandardCharsets.UTF_8));
                datagramSocket.send(packet);
                buffer = new byte[512];
                packet = new DatagramPacket(buffer, buffer.length);
            }
            catch (IOException e){
                System.out.println("Error receiving request.\n");
                System.exit(4);
            }
        }


    }
}
