package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {

        //validate arguments

        if(args.length < 4){
            System.out.println("Usage: java Client <host> <port> <oper> <opnd>*\n");
            System.exit(1);
        }

        Socket socket = null;
        
        //open socket
        try{
            socket = new Socket(args[0], Integer.parseInt(args[1]));
            //create packet and send

            String aux = args[2] + " " + args[3];

            if(args.length == 5){
                aux += " " + args[4];
            }

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(aux);
        }
        catch(IOException e){
            System.out.println("Error opening socket and creating packet");
            System.exit(2);
        }


        //wait for response

        String response = null;
        try {
            response = in.readLine();
        } catch (IOException e) {
            System.out.println("Error receiving response");
            System.exit(3);
        }

        System.out.println(response);

        try{
            // close streams
            socket.shutdownOutput();
            while(in.readLine() != null);
            out.close();
            in.close();

            // close socket
            socket.close();
        }
        catch(IOException e){
            System.out.println("Error closing streams");
            System.exit(4);
        }



    }
}

