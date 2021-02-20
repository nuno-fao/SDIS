package client;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
    private static DatagramSocket socket;

    public static void main(String[] args) {

        //validate arguments

        if(args.length < 4){
            System.out.println("Usage: java Client <host> <port> <oper> <opnd>*\n");
            System.exit(1);
        }

        //open socket
        try{
            socket = new DatagramSocket();
        }
        catch(SocketException e){
            System.out.println("Error opening socket.\n");
            System.exit(2);
        }

        //create packet and send

        String aux = args[2] + " " + args[3];

        if(args.length == 5){
            aux += " " + args[4];
        }

        byte[] buffer = aux.getBytes(StandardCharsets.UTF_8);

        DatagramPacket packet;

        try{
            if(!args[0].equals("null")){
                packet = new DatagramPacket(buffer,buffer.length,InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
            }
            else{
                packet = new DatagramPacket(buffer,buffer.length,InetAddress.getLocalHost(),Integer.parseInt(args[1]));
            }
            socket.send(packet);

        }
        catch(IOException e){
            System.out.println("Error creating Datagram Packet.\n");
            System.exit(3);
        }

        //wait for response

        byte[] answer = new byte[buffer.length];
        try{
            packet = new DatagramPacket(answer,answer.length);
            socket.setSoTimeout(10000);
            socket.receive(packet);
            String newaux = new String(packet.getData());
            if(newaux.equals("-1")){
                System.out.println("Client: " + args[2] + " " + args[3] + " " + " : ERROR");
            }
            else if(args.length==4){
                System.out.println("Client: " + args[2] + " " + args[3] + " " + " : " + newaux);
            }
            else if (args.length==5){
                System.out.println("Client: " + args[2] + " " + args[3] + " " + args[4] + " : " + newaux);
            }
        }
        catch (IOException e){
            System.out.println("Error receiving response.\n");
            System.exit(4);
        }

        socket.close();


    }
}
