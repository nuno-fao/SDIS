package sdis.server.TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPDispatcher {
    private ServerSocket serverSocket;

    public void start(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                new TCPDispatcher(this.serverSocket.accept()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        this.serverSocket.close();
    }

    private static class TCPDispatcher extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        TCPDispatcher(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(
                    new InputStreamReader(this.clientSocket.getInputStream()));

            String inputLine = null;
            while (true) {
                try {
                    if (!((inputLine = this.in.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (".".equals(inputLine)) {
                    this.out.println("bye");
                    break;
                }
                this.out.println(inputLine);
            }

            this.in.close();
            this.out.close();
            this.clientSocket.close();
        }
    }
