package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Client {
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {

        //validate arguments



        if(args.length < 4){
            System.out.println("Usage: java Client <host> <port> <oper> <opnd>* <cipher>*\n");
            System.exit(1);
        }


        String[] cypherSuites = null;
        if(args[2].equals("lookup")){
            cypherSuites = Arrays.copyOfRange(args, 4, args.length);
        } else if (args[2].equals("register")){
            cypherSuites = Arrays.copyOfRange(args, 5, args.length);
        } else {
            System.out.println("Invalid operation");
            System.exit(2);
        }

        SSLSocket socket = null;

        
        //open socket
        try{
            socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(args[0], Integer.parseInt(args[1]));
            socket.setEnabledCipherSuites(cypherSuites);

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
            System.exit(3);
        }


        //wait for response

        String response = null;
        try {
            response = in.readLine();
        } catch (IOException e) {
            System.out.println("Error receiving response");
            System.exit(4);
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
            System.exit(5);
        }



    }
}

