package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

import static java.lang.String.valueOf;

public class Server {
    private static int port;
    private static ServerSocket socket;
    private static HashMap<String,String> IPTable;

    public static void main(String[] args)  {

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
            socket = new ServerSocket(port);
        }
        catch (IOException e) {
            System.out.println("Couldn't open socket on the given port.\n");
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
                Socket socket = null;
                socket = Server.socket.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String requestString = in.readLine();
                System.out.println("Received: " + requestString);

                String replyString = processRequest(requestString);
                out.println(replyString);

                System.out.println("Replied: " + replyString);
                System.out.println();

                socket.shutdownOutput();
                while(in.readLine() != null);
                out.close();
                in.close();
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

