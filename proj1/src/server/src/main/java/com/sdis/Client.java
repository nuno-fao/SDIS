package com.sdis;

import java.rmi.Naming;

public class Client {
    public static void main(String[] args) throws Exception{
        ClientInterface server = (ClientInterface) Naming.lookup("//192.168.1.97/peerserver");
        System.out.println(server.getMessage());
    }
}
