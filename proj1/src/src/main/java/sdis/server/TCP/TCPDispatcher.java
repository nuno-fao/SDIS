package sdis.server.TCP;

public class TCPDispatcher {
    /*private ServerSocket serverSocket;

    private void start(final int port) {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        while (true) try {
            new DownloadWorker(this.serverSocket.accept()).start();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            this.serverSocket.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }*/
}
