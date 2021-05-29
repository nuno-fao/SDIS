package peer;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    private ConcurrentHashMap<String,File> localFiles;
    private ConcurrentHashMap<String, RemoteFile> localCopies;
    private AtomicLong maxSize;
    private AtomicLong currentSize;

    Handler(byte[] message, int peerId, Chord chord,Address address, ConcurrentHashMap<String,File> localFiles, ConcurrentHashMap<String,RemoteFile> localCopies,AtomicLong maxSize, AtomicLong currentSize) {
        this.message = message;
        this.peerId = peerId;
        this.chord = chord;
        this.address = address;
        this.localFiles = localFiles;
        this.localCopies = localCopies;
        this.maxSize = maxSize;
        this.currentSize = currentSize;
    }

    public void processMessage()
    {
        String stringMessage = new String(this.message);
        if (stringMessage.startsWith("CHORD")) {
            chord.processMessage(this.message);
            return;
        }
        System.out.println(new String(this.message));


        Header headers = HeaderConcrete.getHeaders(new String(this.message));
        switch (headers.getMessageType()){
            case GETFILE: {
                new GetFileHandler(peerId,headers.getFileID(),headers.getAddress(),headers.getPort(),localCopies,chord);
                break;
            }
            case PUTFILE: {
                new PutFileHandler(chord,headers.getFileID(),peerId,headers.getReplicationDeg(),address,new Address(headers.getAddress(),headers.getPort()),localFiles,localCopies);
                break;
            }
            case DELETE: {
                new DeleteHandler(peerId,headers.getFileID(),headers.getReplicationDeg(),localCopies,chord);
                break;
            }
        }
    }
}

class PutFileHandler{
    private String fileId;
    private Address local,remote;
    private int peerId,repDegree;
    private Chord chord;
    private ConcurrentHashMap<String, RemoteFile> localCopies;
    public PutFileHandler(Chord chord,String fileId,int peerId,int repDegree,Address localAddress,Address remoteAddress,ConcurrentHashMap<String,File> localFiles,ConcurrentHashMap<String,RemoteFile> localCopies) {
        this.fileId = fileId;
        this.peerId = peerId;
        this.local = localAddress;
        this.remote = remoteAddress;
        this.chord = chord;
        this.repDegree = repDegree;
        this.localCopies = localCopies;
        if(localFiles.contains(fileId)){
            return;
        }
        try {
            Receive();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            localCopies.put(fileId,new RemoteFile("s","s"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void PropagateSend(byte[] data,int replicationDegree) throws IOException {
        Node successor = chord.getSuccessor();
        TCP messageWriter = new TCP(successor.address.address,successor.address.port);

        SSLServerSocket s = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);

        byte[] contents = MessageType.createPutFile(fileId, local.address, Integer.toString(s.getLocalPort()),replicationDegree);
        messageWriter.write(contents);

        s.accept().getOutputStream().write(data);
    }

    private void Receive() throws IOException {
        TCP reader = new TCP(remote.address, remote.port);
        InputStream s = reader.getSocket().getInputStream();
        byte[] read = new byte[1024];
        byte[] out = new byte[1024];
        int r = 0, sum = 0;
        while ((r = s.read(read,0, 1024)) > 0) {
            out = Arrays.copyOf(out,sum+r);
            System.arraycopy(read,0,out,sum,r);
            sum += r;
        }
        SaveFile(out,sum);
        if(repDegree>1) {
            PropagateSend(out,repDegree--);
        }
    }

    private void SaveFile(byte[] data, int l){
        try {
            Files.createDirectories(Paths.get(peerId + "/" + "stored"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path path = Paths.get(peerId + "/" + "stored" + "/" + fileId);
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteBuffer buffer = ByteBuffer.allocate(l);
        buffer.put(data,0,l);
        buffer.flip();
        fileChannel.write(
                buffer,0, fileChannel, new CompletionHandler<Integer, Closeable>() {

                    @Override
                    public void completed(Integer result, Closeable closeable) {
                        try {
                            closeable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void failed(Throwable exc, Closeable closeable) {
                        try {
                            closeable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}


class GetFileHandler{

    private String fileId;
    private String address;
    private int port;
    private ConcurrentHashMap<String,RemoteFile> localCopies;
    private int peerId;
    private Chord chord;

    public GetFileHandler(int peerId, String fileId, String address, int port, ConcurrentHashMap<String,RemoteFile> localCopies, Chord chord) {
        this.fileId = fileId;
        this.address = address;
        this.port = port;
        this.localCopies = localCopies;
        this.peerId = peerId;
        this.chord = chord;

        if(!hasCopy()){
            resendMessage();
        }
        else{
            sendFile();
        }
    }

    public boolean hasCopy(){
        return localCopies.contains(fileId);
    }

    public void resendMessage(){
        Node successor = chord.getSuccessor();
        TCP tcp = new TCP(successor.address.address,successor.address.port);
        byte[] contents = MessageType.createGetFile(fileId,address,Integer.toString(port));
        tcp.write(contents);
    }

    public void sendFile(){
        TCP tcp = new TCP(address,port);
        Path file = Path.of(peerId+ "/" + "stored" + "/" + fileId);
        try {
            byte[] contents = Files.readAllBytes(file);
            tcp.write(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class DeleteHandler {

    private String fileId;
    private int replicationDegree;
    private ConcurrentHashMap<String,RemoteFile> localCopies;
    private int peerId;
    private Chord chord;

    public DeleteHandler(int peerId, String fileId, int replicationDegree, ConcurrentHashMap<String,RemoteFile> localCopies, Chord chord) {
        this.fileId = fileId;
        this.replicationDegree=replicationDegree;
        this.localCopies = localCopies;
        this.peerId = peerId;
        this.chord = chord;

        if(!hasCopy()){
            resendMessage();
        }
        else{
            deleteFile();
            if(replicationDegree > 0){
                resendMessage();
            }
        }
    }

    public boolean hasCopy(){
        return localCopies.contains(fileId);
    }

    public void resendMessage(){
        Node successor = chord.getSuccessor();
        TCP tcp = new TCP(successor.address.address,successor.address.port);
        byte[] contents = MessageType.createDelete(fileId,replicationDegree);
        tcp.write(contents);
    }

    public void deleteFile(){
        localCopies.get(fileId).deleteFile();
        replicationDegree--;
        localCopies.remove(fileId);
    }

}