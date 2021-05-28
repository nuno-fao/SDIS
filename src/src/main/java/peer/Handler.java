package peer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Used as a worker, processes the received messages and sends the response
 */
public class Handler {
    private byte[] message;
    private int peerId;
    private Chord chord;
    private Address address;
    private ConcurrentHashMap<String,File> localFiles = new ConcurrentHashMap<>();

    Handler(byte[] message, int peerId, Chord chord,Address address) {
        this.message = message;
        this.peerId = peerId;
        this.chord = chord;
        this.address = address;
    }

    public void processMessage()
    {
        String stringMessage = new String(this.message);
        if (stringMessage.startsWith("CHORD")) chord.processMessage(this.message);

        String[] header = new String(this.message).stripLeading().split( " ", 2);
        Header headers = HeaderConcrete.getHeaders(header[0]);
        if(localFiles.contains(headers.getFileID())){
            return;
        }
        switch (headers.getMessageType()){
            case GETFILE: {
                new GetFileHandler();
                break;
            }
            case PUTFILE: {
                new PutFileHandler();
                break;
            }
            case DELETE: {
                new DeleteHandler();
                break;
            }
        }
    }
}

class PutFileHandler{
    public PutFileHandler() {

    }
}


class GetFileHandler{
    private String fileId;
    private String address;
    private int port;
    private ConcurrentHashMap<String,File> localFiles;

    public GetFileHandler(String fileId, String address, int port, ConcurrentHashMap<String,File> localFiles) {
        this.fileId = fileId;
        this.address = address;
        this.port = port;
        this.localFiles = localFiles;


    }
}

class DeleteHandler {
    public DeleteHandler() {
    }
}