package peer;

import peer.tcp.TCPServer;
import peer.tcp.TCPWriter;
import test.RemoteInterface;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.sleep;

public class Peer implements RemoteInterface {
    private UnicastDispatcher dispatcher;
    private ChordHelper chordHelper;
    private ConcurrentHashMap<String, File> localFiles;
    private ConcurrentHashMap<String, File> localCopies;
    private AtomicLong maxSize;
    private AtomicLong currentSize;
    private int peerId;
    private Chord chord;
    private String address;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public Peer(String address, int port, int id, Chord chord) {
        System.out.println(id);
        this.localFiles = new ConcurrentHashMap<>();
        this.localCopies = new ConcurrentHashMap<>();
        this.maxSize = new AtomicLong(-1);
        this.currentSize = new AtomicLong(0);

        this.address = address;

        this.chord = chord;
        this.peerId = id;

        this.dispatcher = new UnicastDispatcher(port, id, chord, this.localFiles, this.localCopies, this.maxSize, this.currentSize);
    }

    public static void main(String args[]) throws IOException {
        String address = args[0];
        int port = Integer.parseInt(args[1]);
        int id = 0;
        System.setProperty("javax.net.ssl.keyStore", "keys/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest((address + ":" + port).getBytes());
            BigInteger num = new BigInteger(1, digest);
            id = num.mod(BigDecimal.valueOf(Math.pow(2, Chord.m)).toBigInteger()).intValue();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Invalid algorithm");
        }

        Chord chord = new Chord(id, address, port);

        Peer peer = new Peer(address, port, id, chord);

        peer.start();

        boolean needToCreateCircle = true;

        for (String arg : args) {
            if (arg.contains(":")) {
                chord.Join(new Node(arg + ":" + id));
                needToCreateCircle = false;
            }
        }

        if (needToCreateCircle) chord.Create();


        peer.setChordHelper(new ChordHelper(chord));

        peer.startChord();

        try {
            sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (port == 6666) {
            System.out.println("sending");
            peer.Backup("test.deb", 3);
        }

    }

    public void setChordHelper(ChordHelper chordHelper) {
        this.chordHelper = chordHelper;
    }

    public void start() {
        new Thread(this.dispatcher).start();
    }

    public void startChord() {
        this.executor.scheduleAtFixedRate(this.chordHelper, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public String Backup(String filename, int replicationDegree) throws RemoteException, IOException {
        TCPServer server = new TCPServer();
        BigInteger fileId = null;
        Path newFilePath = Paths.get(filename);
        BasicFileAttributes attr = null;
        if (Files.exists(newFilePath)) {
            long size = 0;
            try {
                size = Files.size(newFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                attr = Files.readAttributes(newFilePath, BasicFileAttributes.class);
                fileId = File.getHashedString(filename + "" + attr.lastModifiedTime().toMillis());
                System.out.println(fileId);
                File f = new File(fileId.toString(), String.valueOf(this.peerId), filename, attr.size(), replicationDegree);

                f.saveMetadata();
                if (this.localFiles.containsKey(f.getFileId())) {
                    return "File " + filename + " already backed up";
                }
                for (File file : this.localFiles.values()) {
                    if (filename.compareTo(file.getName()) == 0) {
                        //TODO delete the file that is already backed up
                        break;
                    }
                }
                this.localFiles.put(f.getFileId(), f);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        Node d = this.chord.FindSuccessor(fileId.remainder(BigInteger.valueOf((long) Math.pow(2, this.chord.m))).intValue());
        if (d.id == this.chord.n.id) {
            d = this.chord.getSuccessor();
        }
        Address destination = d.address;
        TCPWriter t = new TCPWriter(destination.address, destination.port);
        t.write(MessageType.createPutFile(this.peerId, fileId.toString(), this.address, String.valueOf(server.getPort()), replicationDegree));
        //TODO save file metadata && send PUTFILE message


        server.start();
        final java.io.File myFile = new java.io.File(filename); //sdcard/DCIM.JPG
        byte[] mybytearray = new byte[30000];
        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DataInputStream dis = new DataInputStream(bis);
        OutputStream os;
        try {
            os = server.getSocket().getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeLong(mybytearray.length);
            int read;
            while ((read = dis.read(mybytearray)) != -1) {
                dos.write(mybytearray, 0, read);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "File " + filename + " bakced up successfully";
    }

    @Override
    public boolean Restore(String filename) throws RemoteException {

        String fileID = null;
        for (File file : this.localFiles.values()) {
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
        for (File file : this.localFiles.values()) {
            if (filename.compareTo(file.getName()) == 0) {
                fileID = file.getFileId().toString();
            }
        }
        if (fileID == null) {
            return false;
        }

        //TODO send DELETE message to first chord peer (with replication degree)

        this.localFiles.remove(fileID);

        return true;
    }

    @Override
    public void Reclaim(long newMaxSize) throws RemoteException {
        this.maxSize.set(newMaxSize);

        if (this.currentSize.get() > newMaxSize) {
            List<File> copies = new ArrayList<>(this.localCopies.values());
            copies.sort(new Comparator<>() {
                @Override
                public int compare(File o1, File o2) {
                    return (int) (o2.getFileSize() - o1.getFileSize()); // Sorting from biggest to lower
                }
            });

            for (File file : copies) {
                this.currentSize.addAndGet(-file.getFileSize());
                //TODO delete the file and send PUTFILE with replicationDegree one to successor chord peer
            }

        }


    }

    @Override
    public String State() throws RemoteException {
        String out = "";
        out += ("Peer current storage information\n");
        out += "Max Size: " + this.maxSize.get() + "\n";
        out += "Current Size: " + this.currentSize.get() + "\n";

        if (this.localFiles.size() > 0) {
            out += "\nMy Files: " + "\n";
            for (File f : this.localFiles.values()) {
                out += "\n    Name:               " + f.getName() + "\n";
                out += "    FileID:             " + f.getFileId() + "\n";
                //out += "    Desired Rep Degree: " + ((Chunk) f.getChunks().values().toArray()[0]).getRepDegree() + "\n"; TODO idk if this is needed
            }
        }
        if (this.localCopies.size() > 0) {
            out += "\nStored Files: " + "\n";
            for (File f : this.localCopies.values()) {
                out += "\n    FileID:             " + f.getFileId() + "\n";
                out += "    Size:               " + f.getFileSize() + "\n";
            }
        }
        return out;
    }
}
