package sdis;


import sdis.server.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Server extends UnicastRemoteObject implements RemoteInterface {
    private static Registry registry;
    private static Server server = null;
    private long peerId;
    private String accessPoint;
    private String version;
    private MulticastDispatcher mc;
    private MulticastDispatcher mdb;
    private MulticastDispatcher mdr;
    private ConcurrentHashMap<String, RemoteFile> storedFiles;
    private ConcurrentHashMap<String, File> myFiles;
    private ScheduledExecutorService pool = Executors.newScheduledThreadPool(10);
    private int chunkSize = 64000;
    private String serverName;
    private AtomicInteger agains = new AtomicInteger(0);
    private ConcurrentHashMap<String, RestoreFile> fileRestoring = new ConcurrentHashMap<>();
    private AtomicLong maxSize = new AtomicLong(-1);
    private AtomicLong currentSize = new AtomicLong(0);
    private ConcurrentHashMap<String, Boolean> chunkQueue = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, File> waitingForPurge = new ConcurrentHashMap<>();

    private Server(String version, long peerId, String accessPoint, Address mc, Address mdb, Address mdr) throws RemoteException {
        super(0);

        try {
            Files.createDirectories(Paths.get(peerId + "_folder"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.accessPoint = accessPoint;
        this.version = version;
        this.peerId = peerId;

        this.serverName = Integer.toString((int) peerId) + "_folder";
        this.storedFiles = new ConcurrentHashMap<>();
        this.myFiles = new ConcurrentHashMap<>();

        this.mc = new MulticastDispatcher(mc.port, mc.address, this.chunkSize + 500, this.chunkSize, (int) peerId);
        this.mdb = new MulticastDispatcher(mdb.port, mdb.address, this.chunkSize + 500, this.chunkSize, (int) peerId);
        this.mdr = new MulticastDispatcher(mdr.port, mdr.address, this.chunkSize + 500, this.chunkSize, (int) peerId);
        new Thread(this.mc).start();
        new Thread(this.mdb).start();
        new Thread(this.mdr).start();

        sendAwake();
    }

    static Server createServer(String version, long peerId, String accessPoint, Address mc, Address mdb, Address mdr) throws RemoteException {
        server = new Server(version, peerId, accessPoint, mc, mdb, mdr);
        server.readStoredInfo();
        return server;
    }

    public static Server getServer() {
        return server;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 9) {
            throw new Exception("Some args are missing!");
        }
        try {
            Integer.parseInt(args[1]);
        } catch (Exception e) {
            throw new Exception("invalid parameter " + args[1] + ", should be a valid number");
        }
        createServer(args[0], Integer.parseInt(args[1]), args[2], new Address(args[3], Integer.parseInt(args[4])), new Address(args[5], Integer.parseInt(args[6])), new Address(args[7], Integer.parseInt(args[8]))).startRemoteObject();

    }

    private void sendAwake() {
        System.out.println("SENDING AWAKE");
        if (this.version.equals("1.1")) {
            byte message[] = MessageType.createAwake("1.1", (int) this.peerId);
            DatagramPacket packet = new DatagramPacket(message, message.length, this.mc.getAddress(), this.mc.getPort());
            System.out.println(this.mc.getAddress());
            this.pool.schedule(() -> this.mc.send(packet), 3, TimeUnit.SECONDS);
        }
    }

    public ConcurrentHashMap<String, Boolean> getChunkQueue() {
        return chunkQueue;
    }

    public AtomicLong getMaxSize() {
        return maxSize;
    }

    public AtomicLong getCurrentSize() {
        return currentSize;
    }

    public long getPeerId() {
        return this.peerId;
    }

    private void readStoredInfo() {
        try {
            if (Files.exists(Path.of(Server.getServer().getServerName() + "/.ldata"))) {
                Stream<java.io.File> directories_s = Files.walk(Path.of(Server.getServer().getServerName() + "/.ldata/"), 1)
                        .map(Path::toFile);
                Object directories[] = directories_s.toArray();
                for (int i = 1; i < directories.length; i++) {
                    java.io.File directory = (java.io.File) directories[i];
                    Stream<java.io.File> files_s = Files.walk(Path.of(Server.getServer().getServerName() + "/.ldata/" + directory.getName()), 1)
                            .map(Path::toFile);
                    Object files[] = files_s.toArray();
                    File f = null;
                    for (int j = 1; j < files.length; j++) {
                        java.io.File file = (java.io.File) files[j];
                        List<String> info = Files.readAllLines(file.toPath());
                        if (info.size() == 2) {
                            List<String> dets = Arrays.asList((new String(info.get(0).getBytes())).split(";"));
                            if (dets.size() != 3) {
                                continue;
                            }
                            if (f == null) {
                                f = new File(dets.get(2), Integer.parseInt(dets.get(1)));
                            }
                            Chunk c = new Chunk(Integer.parseInt(file.getName()), directory.getName(), Integer.parseInt(dets.get(1)));
                            f.putChunk(Integer.parseInt(file.getName()), c);
                            for (String s : info.get(1).split(";")) {
                                c.getPeerList().put(Integer.parseInt(s), true);
                            }
                        } else {
                            continue;
                        }
                        this.myFiles.put(f.getFileId(), f);
                        if(Files.exists(Path.of(Server.getServer().getServerName() + "/.ldata/" +f.getFileId()+"/PURGING"))){
                            this.waitingForPurge.put(f.getFileId(),f);
                        }
                    }
                }
            }

            if (Files.exists(Path.of(Server.getServer().getServerName() + "/.rdata"))) {
                Stream<java.io.File> directories_s = Files.walk(Path.of(Server.getServer().getServerName() + "/.rdata/"), 1)
                        .map(Path::toFile);
                Object directories[] = directories_s.toArray();
                for (int i = 1; i < directories.length; i++) {
                    java.io.File directory = (java.io.File) directories[i];
                    Stream<java.io.File> files_s = Files.walk(Path.of(Server.getServer().getServerName() + "/.rdata/" + directory.getName()), 1)
                            .map(Path::toFile);
                    Object files[] = files_s.toArray();
                    RemoteFile f = null;
                    for (int j = 1; j < files.length; j++) {
                        java.io.File file = (java.io.File) files[j];
                        List<String> info = Files.readAllLines(file.toPath());
                        if (info.size() == 1) {
                            List<String> dets = Arrays.asList((new String(info.get(0).getBytes())).split(";"));
                            if (dets.size() != 3) {
                                continue;
                            }
                            if (f == null) {
                                f = new RemoteFile(directory.getName());
                            }
                            f.getChunks().put(Integer.parseInt(file.getName()), new Chunk(Integer.parseInt(file.getName()), directory.getName(), Integer.parseInt(dets.get(1)), Integer.parseInt(dets.get(0)), Integer.parseInt(dets.get(2))));
                        } else {
                            continue;
                        }
                        this.storedFiles.put(f.getFileId(), f);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (RemoteFile rf : storedFiles.values()) {
            for (Chunk c : rf.getChunks().values()) {
                currentSize.addAndGet(c.getSize());
            }
        }
    }

    public ScheduledExecutorService getPool() {
        return this.pool;
    }

    public int getChunkSize() {
        return this.chunkSize;
    }

    public String getServerName() {
        return this.serverName;
    }

    public MulticastDispatcher getMc() {
        return this.mc;
    }

    public MulticastDispatcher getMdb() {
        return this.mdb;
    }

    public MulticastDispatcher getMdr() {
        return this.mdr;
    }

    public ConcurrentHashMap<String, RemoteFile> getStoredFiles() {
        return this.storedFiles;
    }

    public ConcurrentHashMap<String, File> getMyFiles() {
        return this.myFiles;
    }

    String getAccessPoint() {
        return this.accessPoint;
    }

    public String getVersion() {
        return this.version;
    }

    public ConcurrentHashMap<String, File> getWaitingForPurge() {
        return waitingForPurge;
    }

    private void startRemoteObject() {
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
        }

        try {
            Naming.rebind(this.accessPoint, this);
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<String, RestoreFile> getFileRestoring() {
        return this.fileRestoring;
    }

    @Override
    public String Backup(String filename, int replicationDegree) {
        long before = System.currentTimeMillis();
        Path newFilePath = Paths.get(filename);
        if (Files.exists(newFilePath)) {
            long size = 0;
            try {
                size = Files.size(newFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            File f;
            try {
                f = new File(filename, replicationDegree);
                if (this.myFiles.containsKey(File.getFileInfo(filename))) {
                    return "file already backed up";
                }
                for (File file : this.myFiles.values()) {
                    if (filename.compareTo(file.getName()) == 0) {
                        deleteFile(file.getFileId());
                    }
                }
                this.myFiles.put(f.getFileId(), f);
            } catch (IOException e) {
                e.printStackTrace();
                return "Error opening file";
            }
            try {
                InputStream io = new FileInputStream(filename);
                send(f, replicationDegree, io, 0);

                this.myFiles.get(f.getFileId()).setNumChunks((int) (size / Server.server.getChunkSize() + 1));

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Backup Time: " + (System.currentTimeMillis() - before));
            return "Successfully sent";
        }
        return "File not found";
    }

    private void backupAux(int i, ScheduledExecutorService pool, DatagramPacket packet, String fileId, int chunkNo, int repDegree) {
        this.mdb.send(packet);
        pool.schedule(() -> {
            if (Server.getServer().getMyFiles().get(fileId).getReplicationDegree(chunkNo) < repDegree) {
                if (i < 16) {
                    System.out.println("Against: " + i + " " + this.agains.getAndIncrement() + " " + chunkNo);
                    this.backupAux(i * 2, pool, packet, fileId, chunkNo, repDegree);
                } else {
                    System.out.println("Gave up");
                }
            }
        }, i * 1000 + new Random().nextInt(401), TimeUnit.MILLISECONDS);
    }

    private void send(File f, int replicationDegree, InputStream io, int i) {
        byte a[] = new byte[this.chunkSize];
        this.myFiles.get(f.getFileId()).putChunk(i, new Chunk(i, f.getFileId(), replicationDegree));
        int size = 0;
        try {
            size = io.read(a);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (size == -1 || size == 0) {
            byte message[] = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, "".getBytes());
            DatagramPacket packet = new DatagramPacket(message, message.length, this.mdb.getAddress(), this.mdb.getPort());
            this.backupAux(1, this.pool, packet, f.getFileId(), i, replicationDegree);
            return;
        }
        byte message[];
        if (size == Server.getServer().chunkSize) {
            message = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, a);
        } else {
            byte[] subArray = Arrays.copyOfRange(a, 0, size);
            message = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, subArray);
        }
        DatagramPacket packet = new DatagramPacket(message, message.length, this.mdb.getAddress(), this.mdb.getPort());

        this.backupAux(1, this.pool, packet, f.getFileId(), i, replicationDegree);
        if (size == this.chunkSize) {
            this.pool.schedule(() -> send(f, replicationDegree, io, i + 1), (new Random().nextInt(20)) + 10, TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public boolean Restore(String filename) {
        long before = System.currentTimeMillis();

        String fileID = null;
        for (File file : this.myFiles.values()) {
            if (filename.compareTo(file.getName()) == 0) {
                fileID = file.getFileId();
            }
        }

        if (fileID == null) {
            return false;
        }

        ConcurrentHashMap<Integer, byte[]> receivedChunks = new ConcurrentHashMap<>();

        this.fileRestoring.put(fileID, new RestoreFile(receivedChunks));

        for (Chunk chunk : this.myFiles.get(fileID).getChunks().values()) {
            byte[] message = MessageType.createGetchunk("1.0", (int) this.peerId, fileID, chunk.getChunkNo());
            DatagramPacket packet = new DatagramPacket(message, message.length, this.mc.getAddress(), this.mc.getPort());
            String finalFileID = fileID;
            pool.schedule(() -> RestoreAux(1, this.pool, packet, finalFileID, chunk.getChunkNo(), 1), new Random().nextInt(401), TimeUnit.MILLISECONDS);

        }

        System.out.println("Restore Time for file " + fileID + ": " + (System.currentTimeMillis() - before));
        return true;

    }

    private void RestoreAux(int i, ScheduledExecutorService pool, DatagramPacket packet, String fileID, int chunkNo, int t) {
        this.mc.send(packet);
        pool.schedule(() -> {
            if (i < 5 && this.fileRestoring.get(fileID) != null && !this.fileRestoring.get(fileID).getChunks().containsKey(chunkNo)) {
                this.RestoreAux(i + 1, pool, packet, fileID, chunkNo, t * 2);
            }
        }, new Random().nextInt(401) + t * 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean Delete(String filename) {
        long before = System.currentTimeMillis();
        String file = File.getFileInfo(filename);
        if (file == null) {
            return false;
        }
        if (!deleteFile(file)) {
            return false;
        }
        System.out.println("Delete Time for file " + file + ": " + (System.currentTimeMillis() - before));
        return true;
    }


    private boolean deleteFile(String fileId) {
        if (this.version.equals("1.0")) {
            byte message[] = MessageType.createDelete("1.0", (int) this.peerId, fileId);
            DatagramPacket packet = new DatagramPacket(message, message.length, this.mc.getAddress(), this.mc.getPort());
            try {
                Files.walk(Path.of(Server.getServer().getServerName() + "/.ldata/" + fileId))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            } catch (IOException e) {
                return false;
            }
            this.deleteAux(0, this.pool, packet);
            this.myFiles.remove(fileId);
            return true;
        } else if (this.version.equals("1.1")) {
            if (this.myFiles.containsKey(fileId)) {

                Path path = Paths.get(Server.getServer().getServerName() + "/.ldata/"  + fileId + "/PURGING" );
                AsynchronousFileChannel fileChannel = null;
                try {
                    fileChannel = AsynchronousFileChannel.open(
                            path, WRITE, CREATE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte out[] = "".getBytes();
                ByteBuffer buffer = ByteBuffer.allocate(out.length);

                buffer.put(out);
                buffer.flip();

                Future<Integer> operation = fileChannel.write(buffer, 0);
                buffer.clear();

                byte message[] = MessageType.createDelete("1.1", (int) this.peerId, fileId);
                DatagramPacket packet = new DatagramPacket(message, message.length, this.mc.getAddress(), this.mc.getPort());
                waitingForPurge.put(fileId, this.myFiles.get(fileId));
                this.deleteAux(0, this.pool, packet);
                return true;
            }
            return false;
        }
        return false;
    }

    private void deleteAux(int i, ScheduledExecutorService pool, DatagramPacket packet) {
        this.mc.send(packet);
        pool.schedule(() -> {
            if (i < 5) {
                this.deleteAux(i + 1, pool, packet);
            }
        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
    }

    @Override
    public void Reclaim(long maxSpace) {
        long before = System.currentTimeMillis();

        maxSize.set(maxSpace);

        //System.out.println("MAX SIZE "+maxSize.get());
        //System.out.println("CURRENT SIZE "+currentSize.get());

        if (currentSize.get() > maxSpace) {

            //System.out.println("ENTREI PARA LIMPAR");

            List<Chunk> cleanable = new ArrayList<>();
            for (RemoteFile file : this.storedFiles.values()) {
                cleanable.addAll(file.getChunks().values());
            }
            cleanable.sort(new Comparator<>() {
                @Override
                public int compare(Chunk o1, Chunk o2) {
                    return (o1.getRealDegree() - o1.getRepDegree()) - (o2.getRealDegree() - o2.getRepDegree());
                }
            });

            for (Chunk chunk : cleanable) {
                if (storedFiles.get(chunk.getFileId()).deleteChunk(chunk.getChunkNo())) {

                    //System.out.println("REMOVING CHUNK "+chunk.getChunkNo()+" "+chunk.getSize());
                    currentSize.addAndGet(-chunk.getSize());

                    byte message[] = MessageType.createRemoved("1.0", (int) this.peerId, chunk.getFileId(), chunk.getChunkNo());
                    DatagramPacket packet = new DatagramPacket(message, message.length, this.mc.getAddress(), this.mc.getPort());
                    this.mc.send(packet);
                    //System.out.println("CURRENT SIZE BEFORE BREAK");
                    if (currentSize.get() <= maxSize.get()) {
                        break;
                    }
                }

            }
        }

        System.out.println("Reclaim Time : " + (System.currentTimeMillis() - before));
    }

    @Override
    public String State() throws RemoteException {
        String out = "";
        out += ("Backed Up Files Owned by the peer\n");
        out += "Max Size: " + Server.getServer().getMaxSize().get() + "\n";
        out += "Current Size: " + Server.getServer().getCurrentSize().get() + "\n";
        for (File f : this.myFiles.values()) {
            out += "    Name:               " + f.getName() + "\n";
            out += "    FileID:             " + f.getFileId() + "\n";
            out += "    Desired Rep Degree: " + ((Chunk) f.getChunks().values().toArray()[0]).getRepDegree() + "\n";
            out += "    CHUNKS: \n";
            for (Chunk c : f.getChunks().values()) {
                out += "        CHUNK NO:     " + c.getChunkNo() + "\n";
                out += "            Perceived Rep Degree:    " + c.getPeerCount() + "\n";
            }
        }
        return out;
    }


}
