package peer;

import test.RemoteInterface;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Peer implements RemoteInterface {
    private UnicastDispatcher dispatcher;
    private ChordHelper chordHelper;
    private ConcurrentHashMap<String,File> localFiles;
    private ConcurrentHashMap<String, File> localCopies;
    private AtomicLong maxSize;
    private AtomicLong currentSize;
    private int peerId;
    private Chord chord;
    private String address;

    public Peer(String address,int port, int id, Chord chord){
        localFiles = new ConcurrentHashMap<>();
        localCopies = new ConcurrentHashMap<>();
        maxSize = new AtomicLong(-1);
        currentSize = new AtomicLong(0);

        this.address = address;

        this.chord = chord;
        this.peerId = id;

        this.dispatcher = new UnicastDispatcher(port, id, chord, localFiles, localCopies, maxSize, currentSize);
    }

    public void setChordHelper(ChordHelper chordHelper){
        this.chordHelper = chordHelper;
    }

    public void start(){
        new Thread(dispatcher).start();
    }

    public void startChord(){
        new Thread(chordHelper).start();
    }

    public static void main(String args[]) throws IOException {
        String address = args[0];
        int port = Integer.parseInt(args[1]);
        int id = 0;
        System.setProperty("javax.net.ssl.keyStore", "keys/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest((address + ":" + port).getBytes());
            BigInteger num = new BigInteger(1, digest);
            id = num.mod(BigDecimal.valueOf(Math.pow(2, Chord.m)).toBigInteger()).intValue();
        }
        catch (NoSuchAlgorithmException e)
        {
            System.out.println("Invalid algorithm");
        }

        Chord chord = new Chord(id, address, port);

        Peer peer = new Peer(address,port, id, chord);

        peer.start();

        boolean needToCreateCircle = true;

        for (String arg : args) {
            if (arg.contains(":"))
            {
                chord.Join(new Node(arg+":"+id));
                needToCreateCircle = false;
            } 
        }

        if (needToCreateCircle) chord.Create();

        ExecutorService pool = Executors.newFixedThreadPool(10);

        peer.setChordHelper( new ChordHelper(chord));

        peer.startChord();

        System.out.println(port);
        if(port == 8000){
            peer.Backup(".gitignore",1);
        }
        else {
        }

    }

    @Override
    public String Backup(String filename, int replicationDegree) throws RemoteException,IOException {
        SSLServerSocket s = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);
        BigInteger fileId = null;
        Path newFilePath = Paths.get(filename);
        if(Files.exists(newFilePath)){
            long size = 0;
            try {
                size = Files.size(newFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try{
                var attr = Files.readAttributes(newFilePath, BasicFileAttributes.class);
                fileId = File.getHashedString(filename+""+attr.lastModifiedTime().toMillis());
                System.out.println(fileId);
                File f = new File(fileId.toString(),String.valueOf(peerId),filename, attr.size(),replicationDegree);

                f.saveMetadata();
                if(localFiles.containsKey(f.getFileId())){
                    return "File "+filename + " already backed up";
                }
                for (File file : localFiles.values()) {
                    if (filename.compareTo(file.getName()) == 0) {
                        //TODO delete the file that is already backed up
                        break;
                    }
                }
                localFiles.put(f.getFileId(),f);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        Node d = chord.FindSuccessor(fileId.remainder(BigInteger.valueOf((long) Math.pow(2, chord.m))).intValue());
        if(d.id == chord.n.id){
            //todo verificar se o find
            d = chord.FindSuccessor(d.id+1);
        }
        Address destination = d.address;
        TCP t = new TCP(destination.address,destination.port);
        t.write(MessageType.createPutFile(peerId,fileId.toString(),address, String.valueOf(s.getLocalPort()),replicationDegree));
        //TODO save file metadata && send PUTFILE message



        byte[] data = Files.readAllBytes(newFilePath);

        SSLSocket clientSocket;
        clientSocket = (SSLSocket) s.accept();
        System.out.println("is about to write");

        clientSocket.getOutputStream().write(data);

        return "File "+filename + " bakced up successfully";
    }

    @Override
    public boolean Restore(String filename) throws RemoteException {

        String fileID = null;
        for (File file : localFiles.values()) {
            if (filename.compareTo(file.getName()) == 0) {
                fileID = file.getFileId().toString();
            }
        }

        if (fileID == null) {
            return false;
        }

        //TODO send GETFILE message to first chord peer and write file
        return true;
    }

    @Override
    public boolean Delete(String filename) throws RemoteException {
        String fileID = null;
        for (File file : localFiles.values()) {
            if (filename.compareTo(file.getName()) == 0) {
                fileID = file.getFileId().toString();
            }
        }
        if (fileID == null) {
            return false;
        }

        //TODO send DELETE message to first chord peer (with replication degree)

        localFiles.remove(fileID);

        return true;
    }

    @Override
    public void Reclaim(long newMaxSize) throws RemoteException {
        maxSize.set(newMaxSize);

        if(currentSize.get()>newMaxSize){
            List<File> copies = new ArrayList<>(localCopies.values());
            copies.sort(new Comparator<>() {
                @Override
                public int compare(File o1, File o2) {
                    return (int)(o2.getFileSize() - o1.getFileSize()); // Sorting from biggest to lower
                }
            });

            for(File file : copies){
                currentSize.addAndGet(-file.getFileSize());
                //TODO delete the file and send PUTFILE with replicationDegree one to successor chord peer
            }

        }


    }

    @Override
    public String State() throws RemoteException {
        String out = "";
        out += ("Peer current storage information\n");
        out += "Max Size: " + maxSize.get() + "\n";
        out += "Current Size: " + currentSize.get() + "\n";

        if(localFiles.size()>0) {
            out += "\nMy Files: " + "\n";
            for (File f : localFiles.values()) {
                out += "\n    Name:               " + f.getName() + "\n";
                out += "    FileID:             " + f.getFileId() + "\n";
                //out += "    Desired Rep Degree: " + ((Chunk) f.getChunks().values().toArray()[0]).getRepDegree() + "\n"; TODO idk if this is needed
            }
        }
        if(localCopies.size()>0) {
            out += "\nStored Files: " + "\n";
            for (File f : localCopies.values()) {
                out += "\n    FileID:             " + f.getFileId() + "\n";
                out += "    Size:               " + f.getFileSize() + "\n";
            }
        }
        return out;
    }
}
