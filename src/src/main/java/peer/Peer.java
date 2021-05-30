package peer;

import peer.tcp.TCPServer;
import peer.tcp.TCPWriter;
import test.RemoteInterface;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
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
import java.util.Arrays;
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
            sleep(12000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (port == 6666) {
            //peer.Backup("test.deb", 3);
            peer.Backup(".gitignore", 3);

            try {
                sleep(12000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            peer.Delete(".gitignore");
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
                File f = new File(fileId.toString(), String.valueOf(this.peerId), filename, attr.size(), replicationDegree);

                f.saveMetadata();
                if (this.localFiles.containsKey(f.getFileId())) {
                    return "File " + filename + " already backed up";
                }
                for (File file : this.localFiles.values()) {
                    if (filename.compareTo(file.getServerName()) == 0) {
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

        System.out.println("ID:" + fileId.remainder(BigInteger.valueOf((long) Math.pow(2, this.chord.m))).intValue());
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

        System.out.println("File " + filename + " bakced up successfully");
        return "File " + filename + " bakced up successfully";
    }

    @Override
    public boolean Restore(String filename) throws IOException {

        SSLServerSocket s = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);

        BigInteger fileId = null;
        for (File file : this.localFiles.values()) {
            if (filename.compareTo(file.getServerName()) == 0) {
                fileId = new BigInteger(file.getFileId());
            }
        }

        if (fileId == null) {
            return false;
        }

        Node d = this.chord.FindSuccessor(fileId.remainder(BigInteger.valueOf((long) Math.pow(2, this.chord.m))).intValue());
        if (d.id == this.chord.n.id) {
            d = this.chord.FindSuccessor(d.id + 1);
        }

        Address destination = d.address;
        TCPWriter t = new TCPWriter(destination.address, destination.port);
        t.write(MessageType.createGetFile(this.peerId, fileId.toString(), this.address, String.valueOf(s.getLocalPort())));

        SSLSocket clientSocket;
        clientSocket = (SSLSocket) s.accept();
        System.out.println("is about to read");


        InputStream stream = clientSocket.getInputStream();
        byte[] read = new byte[1024];
        byte[] out = new byte[1024];
        int r = 0, sum = 0;
        while ((r = stream.read(read, 0, 1024)) > 0) {
            out = Arrays.copyOf(out, sum + r);
            System.arraycopy(read, 0, out, sum, r);
            sum += r;
        }

        //localFiles.get(fileId.toString()).writeToFile(out); //TODO WRITE TO FILE

        return true;
    }

    @Override
    public boolean Delete(String filename) throws RemoteException {
        BigInteger fileId = null;
        for (File file : this.localFiles.values()) {
            if (filename.compareTo(file.getFileName()) == 0) {
                fileId = new BigInteger(file.getFileId());
            }
        }
        if (fileId == null) {
            return false;
        }

        Node d = this.chord.FindSuccessor(fileId.remainder(BigInteger.valueOf((long) Math.pow(2, this.chord.m))).intValue());
        if (d.id == this.chord.n.id) {
            d = this.chord.FindSuccessor(d.id + 1);
        }

        Address destination = d.address;
        TCPWriter t = new TCPWriter(destination.address, destination.port);
        t.write(MessageType.createDelete(this.peerId, fileId.toString(), this.localFiles.get(fileId.toString()).getReplicationDegree(),-1));

        this.localFiles.remove(fileId);
        System.out.println("Deleted file "+filename);
        return true;
    }

    @Override
    public void Reclaim(long newMaxSize) throws IOException {
        this.maxSize.set(newMaxSize);

        if (this.currentSize.get() > newMaxSize) {
            List<File> copies = new ArrayList<>(this.localCopies.values());
            copies.sort((o1, o2) -> {
                return (int) (o2.getFileSize() - o1.getFileSize()); // Sorting from biggest to lower
            });

            for (File file : copies) {
                this.currentSize.addAndGet(-file.getFileSize());

                Node successor = this.chord.getSuccessor();
                TCPWriter messageWriter = new TCPWriter(successor.address.address, successor.address.port);  //FIXME não sei se aqui é TCPwriter ou server tbh

                SSLServerSocket s = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);

                byte[] contents = MessageType.createPutFile(this.peerId, file.getFileId(), this.address, Integer.toString(s.getLocalPort()), 1);
                messageWriter.write(contents);

                s.accept().getOutputStream().write(file.readCopyContent());

                this.localCopies.get(file.getFileId()).deleteFile();
                this.localCopies.remove(file.getFileId());


                if (this.currentSize.get() <= newMaxSize) {
                    break;
                }
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
                out += "\n    Name:               " + f.getServerName() + "\n";
                out += "    FileID:             " + f.getFileId() + "\n";
                out += "    Desired Rep Degree: " + f.getReplicationDegree() + "\n";
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
