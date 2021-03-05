package client;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {

    private static DatagramSocket socket;
    private static String mcastAddress, serverAdress;
    private static int mcastPort, serverPort;


    public static void main(String[] args) {
        //validate arguments

        if(args.length < 4){
            System.out.println("Usage: java Client <mcast_addr> <mcast_port> <oper> <opnd> * \n");
            System.exit(1);
        }

        mcastAddress = args[0];
        mcastPort = Integer.parseInt(args[1]);

        //open socket
        try{
            socket = new DatagramSocket();
        }
        catch(SocketException e){
            System.out.println("Error opening socket.\n");
            System.exit(2);
        }

        //lookup
        DatagramPacket serverPacket;
        try{

            MulticastSocket mcastSocket = new MulticastSocket(mcastPort);
            InetAddress address = InetAddress.getByName(mcastAddress);
            mcastSocket.joinGroup(address);

            byte[] buffer = new byte[1024];
            serverPacket = new DatagramPacket(buffer, buffer.length);
            mcastSocket.setSoTimeout(10000);
            mcastSocket.receive(serverPacket);

            mcastSocket.leaveGroup(address);
            mcastSocket.close();


            serverAdress =  serverPacket.getAddress().getHostAddress();
            serverPort = Integer.parseInt(new String(serverPacket.getData()).trim());
        }
        catch (IOException e){
            System.out.println("Did not receive reply from server");
            System.exit(2);
        }

        System.out.println("multicast: " + mcastAddress + " " + String.valueOf(mcastPort) + ": " + serverAdress + " " + String.valueOf(serverPort));

        //continue with the client request
        String aux = args[2] + " " + args[3];

        if(args.length == 5){
            aux += " " + args[4];
        }

        byte[] buffer = aux.getBytes(StandardCharsets.UTF_8);

        DatagramPacket packet;

        try{
            packet = new DatagramPacket(buffer,buffer.length,InetAddress.getByName(serverAdress),Integer.parseInt(args[1]));
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
