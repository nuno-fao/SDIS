package peer;

import peer.tcp.TCPServer;
import peer.tcp.TCPWriter;
import test.RemoteInterface;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.file.StandardOpenOption.*;

public class Peer extends UnicastRemoteObject implements RemoteInterface {
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
    private CopyOnWriteArraySet<BigInteger> notStoredFiles = new CopyOnWriteArraySet<>();

    public static void write(String path, ByteBuffer byteBuffer, long offset, boolean truncate, int size) throws IOException {
        AsynchronousFileChannel output;
        if (truncate) {
            output = AsynchronousFileChannel.open(Path.of(path), WRITE, TRUNCATE_EXISTING, CREATE);
        } else {
            output = AsynchronousFileChannel.open(Path.of(path), WRITE, CREATE);
        }
        byteBuffer = byteBuffer.limit(size);
        output.write(byteBuffer, offset, output, new CompletionHandler<Integer, AsynchronousFileChannel>() {
            @Override
            public void completed(Integer result, AsynchronousFileChannel attachment) {
                try {
                    attachment.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousFileChannel attachment) {
                try {
                    attachment.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Peer(String address, int port, int id, Chord chord) throws RemoteException {
        super(0);
        this.localFiles = new ConcurrentHashMap<>();
        this.localCopies = new ConcurrentHashMap<>();
        this.maxSize = new AtomicLong(-1);
        this.currentSize = new AtomicLong(0);

        this.address = address;

        this.chord = chord;
        this.peerId = id;
        System.out.println("Peer: " + id);
        try {
            Files.createDirectories(Path.of(this.peerId + "/"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        //fixme
        try {
            LocateRegistry.createRegistry(1099);
        } catch (Exception e) {
        }

        try {
            Naming.rebind(this.peerId + "", this);
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
            System.out.println("please run rmiregistry, exiting peer with error");
            System.exit(1);
        }

        readMetadata();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ScheduledExecutorService e = Executors.newSingleThreadScheduledExecutor();
            ExecutorService ex = Executors.newFixedThreadPool(10);
            try {
                if (this.localCopies.size() == 0)
                    return;
                for (File file : this.localCopies.values()) {
                    ex.execute(new Thread(() -> {
                        try {
                            sendFile(this.peerId + "/stored/" + file.getFileId(), 1, new BigInteger(file.getFileId()));
                        } catch (Exception ignored) {
                            
                        }
                    }));
                }
                e.schedule(
                        new Thread(() -> {
                            for (File file : this.localCopies.values()) {
                                if (!this.notStoredFiles.contains(new BigInteger(file.getFileId()))) {
                                    file.deleteFile(this.currentSize);
                                    this.localCopies.remove(file.getFileId());
                                }
                            }
                        }), 5, TimeUnit.SECONDS
                );
                e.awaitTermination(6, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }));

        this.dispatcher = new

                UnicastDispatcher(port, id, chord, this.localFiles, this.localCopies, this.maxSize, this.currentSize, this.notStoredFiles);

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
                    if (filename.compareTo(file.getFileName()) == 0) {
                        Delete(new BigInteger(file.getFileId()));
                        break;
                    }
                }
                this.localFiles.put(f.getFileId(), f);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        sendFile(filename, replicationDegree, fileId);

        System.out.println("File " + filename + " backed up successfully");
        return "File " + filename + " backed up successfully";
    }

    private void sendFile(String path, int replicationDegree, BigInteger fileId) throws FileNotFoundException {
        TCPServer server = new TCPServer();
        Node d = this.chord.FindSuccessor(fileId.remainder(BigInteger.valueOf((long) Math.pow(2, this.chord.m))).intValue());

        System.out.println("ID:" + fileId.remainder(BigInteger.valueOf((long) Math.pow(2, this.chord.m))).intValue());
        if (d.id == this.chord.n.id) {
            d = this.chord.getSuccessor();
        }
        Address destination = d.address;
        TCPWriter t = new TCPWriter(destination.address, destination.port);

        t.write(MessageType.createPutFile(this.peerId, this.peerId, fileId.toString(), this.address, String.valueOf(server.getPort()), replicationDegree, MessageType.generateMessageId()));

        server.start();
        final java.io.File myFile = new java.io.File(path); //sdcard/DCIM.JPG
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
    }

    @Override
    public boolean Restore(String filename) throws IOException {
        TCPServer reader = new TCPServer();

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
        t.write(MessageType.createGetFile(this.peerId, fileId.toString(), this.address, String.valueOf(reader.getPort()), MessageType.generateMessageId()));

        reader.start();


        InputStream in;
        int bufferSize = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Files.createDirectories(Path.of(this.peerId + "/restored"));
            bufferSize = reader.getSocket().getReceiveBufferSize();
            in = reader.getSocket().getInputStream();
            DataInputStream clientData = new DataInputStream(in);
            byte[] buffer = new byte[bufferSize];
            int read;
            clientData.readLong();
            out.write(new byte[8]);
            long fileSize = 0;
            while ((read = clientData.read(buffer)) != -1) {
                Peer.write(this.peerId + "/restored/" + filename, ByteBuffer.wrap(buffer.clone(), 0, read), fileSize, false, read);
                out.write(buffer, 0, read);
                fileSize += read;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Received File " + filename);

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
        var out = Delete(fileId);
        System.out.println("Deleted file " + filename);
        return out;
    }

    public boolean Delete(BigInteger fileId) {
        Node d = this.chord.FindSuccessor(fileId.remainder(BigInteger.valueOf((long) Math.pow(2, this.chord.m))).intValue());
        if (d.id == this.chord.n.id) {
            d = this.chord.FindSuccessor(d.id + 1);
        }

        Address destination = d.address;
        TCPWriter t = new TCPWriter(destination.address, destination.port);
        t.write(MessageType.createDelete(this.peerId, fileId.toString(), this.localFiles.get(fileId.toString()).getReplicationDegree(), MessageType.generateMessageId()));

        this.localFiles.remove(fileId.toString());
        return true;
    }

    @Override
    public void Reclaim(long newMaxSize) throws IOException {
        this.maxSize.set(newMaxSize);
        saveMetadata(this.maxSize.get());

        if (this.currentSize.get() > newMaxSize) {
            List<File> copies = new ArrayList<>(this.localCopies.values());
            copies.sort((o1, o2) -> {
                return (int) (o2.getFileSize() - o1.getFileSize()); // Sorting from biggest to lower
            });

            for (File file : copies) {
                sendFile(this.peerId + "/stored/" + file.getFileId(), 1, new BigInteger(file.getFileId()));

                this.localCopies.get(file.getFileId()).deleteFile(this.currentSize);
                this.localCopies.remove(file.getFileId());

                if (this.currentSize.get() <= newMaxSize) {
                    break;
                }
            }
        }
    }

    public void readMetadata() {
        try {
            java.io.File dir = new java.io.File(this.peerId + "/stored");
            java.io.File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (java.io.File child : directoryListing) {
                    if (!child.isDirectory()) {
                        this.currentSize.addAndGet(child.length());
                        this.localCopies.put(child.getName(), new File(child.getName(), this.peerId + "", child.length()));
                    }
                }
            }
            System.out.println("Current Size: " + (long) (this.currentSize.get() / 1024) + " KiB");
        } catch (Exception ignored) {

        }

        try {
            java.io.File dir = new java.io.File(this.peerId + "/.locals");
            java.io.File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (java.io.File child : directoryListing) {
                    if (!child.isDirectory()) {
                        Path path = Paths.get(this.peerId + "/.locals/" + child.getName());
                        AsynchronousFileChannel fileChannel
                                = null;
                        fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

                        ByteBuffer buffer = ByteBuffer.allocate(1024);

                        fileChannel.read(
                                buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {

                                    @Override
                                    public void completed(Integer result, ByteBuffer attachment) {
                                        try {
                                            byte[] res = new byte[result];
                                            System.arraycopy(buffer.array(), 0, res, 0, result);
                                            Peer.this.localFiles.put(child.getName(), new File(new String(res), Peer.this.peerId + "", child.getName()));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void failed(Throwable exc, ByteBuffer attachment) {
                                    }
                                });
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            Path path = Paths.get(this.peerId + "/.data");
            AsynchronousFileChannel fileChannel
                    = null;
            fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

            ByteBuffer buffer = ByteBuffer.allocate(1024);

            fileChannel.read(
                    buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {

                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            byte[] res = new byte[result];
                            System.arraycopy(buffer.array(), 0, res, 0, result);
                            Peer.this.maxSize.set(Integer.parseInt(new String(res)));
                            saveMetadata(Peer.this.maxSize.get());
                            if (Peer.this.maxSize.get() > -1) {
                                System.out.println("Max space:" + Peer.this.maxSize.get());
                            } else {
                                System.out.println("Max space not defined");
                            }
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                        }
                    });
        } catch (Exception e) {
            saveMetadata(this.maxSize.get());
            if (this.maxSize.get() > -1) {
                System.out.println("Max space:" + this.maxSize.get());
            } else {
                System.out.println("Max space not defined");
            }
        }
    }

    public void saveMetadata(long space) {
        Path path = Paths.get(this.peerId + "/.data");
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, TRUNCATE_EXISTING, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data = (space + "").getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(data.length);

        buffer.put(data);
        buffer.flip();

        fileChannel.write(buffer, 0, fileChannel, new CompletionHandler<Integer, AsynchronousFileChannel>() {
            @Override
            public void completed(Integer result, AsynchronousFileChannel attachment) {
                try {
                    attachment.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousFileChannel attachment) {
                try {
                    attachment.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public String State() throws RemoteException {
        String out = "";
        out += ("Peer current storage information\n");
        out += "Max Size: " + this.maxSize.get() + "\n";
        out += "Current Size: " + this.currentSize.get() / 1024 + " KiB\n";

        if (this.localFiles.size() > 0) {
            out += "\nMy Files: " + "\n";
            for (File f : this.localFiles.values()) {
                out += "\n    Name:               " + f.getFileName() + "\n";
                out += "    FileID:             " + f.getFileId() + "\n";
                out += "    Desired Rep Degree: " + f.getReplicationDegree() + "\n";
                out += "    Size:               " + f.getFileSize() / 1024 + " KiB\n";
            }
        }
        if (this.localCopies.size() > 0) {
            out += "\nStored Files: " + "\n";
            for (File f : this.localCopies.values()) {
                out += "\n    FileID:             " + f.getFileId() + "\n";
                out += "    Size:               " + f.getFileSize() / 1024 + " KiB\n";
            }
        }
        return out;
    }
}
