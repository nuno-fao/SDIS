package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

import static java.lang.String.valueOf;

public class Server {
    private static int port;
    private static DatagramSocket socket;
    private static HashMap<String,String> IPTable;

    public static void main(String[] args) {

        //validate arguments

        if(args.length != 1){
            System.out.println("Usage: java Server <port number>\n");
            System.exit(1);
        }
        try{
            port = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException e){
            System.out.println("Port given is not a number.\n");
            System.exit(2);
        }


        //create socket
        try {
            socket = new DatagramSocket(port);
        }
        catch (SocketException e) {
            System.out.println("Couldn't open datagram socket on the given port.\n");
            System.exit(3);
        }

        //create map
         IPTable = new HashMap<>();

        //start loop
        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while(true){
            try{
                //System.out.println("Waiting for client request...\n");
                socket.receive(packet);
                String request = new String(packet.getData());
                System.out.println("Server: " + request);
                String response = processRequest(request.trim());
                packet.setData(response.getBytes(StandardCharsets.UTF_8));
                socket.send(packet);
                buffer = new byte[512];
                packet = new DatagramPacket(buffer, buffer.length);
            }
            catch (IOException e){
                System.out.println("Error receiving request.\n");
                System.exit(4);
            }
        }

    }

    private static String processRequest(String request){
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
    }
}

