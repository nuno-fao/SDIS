package com.sdis;

import com.sdis.server.ClientConnection;

import java.rmi.RemoteException;

public class Server {
    public static void main(String[] args) throws RemoteException {
        new ClientConnection();
        System.out.println("Running...");
    }
}
