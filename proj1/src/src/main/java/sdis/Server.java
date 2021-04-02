package sdis;

import sdis.server.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Server extends UnicastRemoteObject implements RemoteInterface {
    private static Registry registry;
    private static Server server = null;
    private long peerId;
    private String accessPoint;
    private String version;
    private MulticastHolder mc;
    private MulticastHolder mdb;
    private MulticastHolder mdr;
    private ConcurrentHashMap<String, RemoteFile> storedFiles;
    private ConcurrentHashMap<String, File> myFiles;
    private ScheduledExecutorService pool = Executors.newScheduledThreadPool(100);
    private int chunkSize = 64000;
    private String serverName;
    private AtomicInteger agains = new AtomicInteger(0);

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

        this.mc = new MulticastHolder(mc.port, mc.address, this.chunkSize + 500, this.chunkSize, (int) peerId);
        this.mdb = new MulticastHolder(mdb.port, mdb.address, this.chunkSize + 500, this.chunkSize, (int) peerId);
        this.mdr = new MulticastHolder(mdr.port, mdr.address, this.chunkSize + 500, this.chunkSize, (int) peerId);
        new Thread(this.mc).start();
        new Thread(this.mdb).start();
        new Thread(this.mdr).start();


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
                    for (int j = 1; j < files.length; j++) {
                        java.io.File file = (java.io.File) files[j];
                        List<String> info = Files.readAllLines(file.toPath());
                        File f = null;
                        if (info.size() == 2) {
                            List<String> dets = Arrays.asList((new String(info.get(0).getBytes())).split(";"));
                            if (dets.size() != 3) {
                                continue;
                            }
                            if (f == null)
                                f = new File(dets.get(2), Integer.parseInt(dets.get(1)));
                            Chunk c = new Chunk(Integer.parseInt(file.getName()), directory.getName(), Integer.parseInt(dets.get(1)));
                            f.getChunks().put(Integer.parseInt(file.getName()), c);
                            for (String s : info.get(1).split(";")) {
                                c.getPeerList().put(Integer.parseInt(s), true);
                            }
                        } else
                            continue;
                        this.myFiles.put(f.getFileId(), f);
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
                    for (int j = 1; j < files.length; j++) {
                        java.io.File file = (java.io.File) files[j];
                        List<String> info = Files.readAllLines(file.toPath());
                        RemoteFile f = null;
                        if (info.size() == 1) {
                            List<String> dets = Arrays.asList((new String(info.get(0).getBytes())).split(";"));
                            if (dets.size() != 2) {
                                continue;
                            }
                            if (f == null)
                                f = new RemoteFile(directory.getName());
                            f.getChunks().put(Integer.parseInt(file.getName()), new Chunk(Integer.parseInt(file.getName()), directory.getName(), Integer.parseInt(dets.get(1)), Integer.parseInt(dets.get(0))));
                        } else
                            continue;
                        this.storedFiles.put(f.getFileId(), f);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    public MulticastHolder getMc() {
        return this.mc;
    }

    public MulticastHolder getMdb() {
        return this.mdb;
    }

    public MulticastHolder getMdr() {
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

    String getVersion() {
        return this.version;
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

    @Override
    public String Backup(String filename, int replicationDegree) {
        long before = System.currentTimeMillis();
        Path newFilePath = Paths.get(filename);
        if (Files.exists(newFilePath)) {
            File f;
            try {
                f = new File(filename, replicationDegree);
                if (this.myFiles.containsKey(File.getFileInfo(filename)))
                    return "file already backed up";
                for (File file : this.myFiles.values()) {
                    if (filename.compareTo(file.getName()) == 0) {
                        return "ahahahahahaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
                    }
                }
                this.myFiles.put(f.getFileId(), f);
            } catch (IOException e) {
                e.printStackTrace();
                return "Error opening file";
            }
            try {
                InputStream io = new FileInputStream(filename);
                int i = 0;
                while (true) {
                    byte a[] = new byte[this.chunkSize];
                    this.myFiles.get(f.getFileId()).getChunks().put(i, new Chunk(i, f.getFileId(), replicationDegree));

                    int size = io.read(a);
                    if (size == -1) {
                        byte message[] = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, "".getBytes());
                        DatagramPacket packet = new DatagramPacket(message, message.length, this.mdb.getAddress(), this.mdb.getPort());
                        this.backupAux(1, this.pool, packet, f.getFileId(), i, replicationDegree);
                        return "Successfully sent";
                    }
                    byte message[];
                    if (size == Server.getServer().chunkSize)
                        message = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, a);
                    else {
                        byte[] subArray = Arrays.copyOfRange(a, 0, size);
                        message = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, subArray);
                    }
                    DatagramPacket packet = new DatagramPacket(message, message.length, this.mdb.getAddress(), this.mdb.getPort());
                    int finalI = i;
                    this.pool.schedule(() -> this.backupAux(1, this.pool, packet, f.getFileId(), finalI, replicationDegree), new Random().nextInt(401), TimeUnit.MILLISECONDS);
                    i++;
                    if (size < this.chunkSize)
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Backup Time: " + (System.currentTimeMillis() - before));
            return "Successfully sent";
        }
        return "File not found";
    }

    private void backupAux(int i, ScheduledExecutorService pool, DatagramPacket packet, String fileId, int chunkNo, int repDegree) {
        System.out.println(fileId + "//" + chunkNo + " sent");
        this.mdb.send(packet);
        pool.schedule(() -> {
            if (Server.getServer().getMyFiles().get(fileId).getReplicationDegree(chunkNo) < repDegree && i < 16) {
                System.out.println("Against: " + this.agains.getAndIncrement());
                this.backupAux(i * 2, pool, packet, fileId, chunkNo, repDegree);
            }
        }, i, TimeUnit.SECONDS);
    }


    @Override
    public void Restore(String filename) {

    }

    @Override
    public boolean Delete(String filename) {
        long before = System.currentTimeMillis();
        String file = File.getFileInfo(filename);
        if (file == null)
            return false;
        byte message[] = MessageType.createDelete("1.0", (int) this.peerId, file);
        DatagramPacket packet = new DatagramPacket(message, message.length, this.mc.getAddress(), this.mc.getPort());
        try {
            Files.walk(Path.of(Server.getServer().getServerName() + "/.ldata/" + file))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        } catch (IOException e) {
            return false;
        }
        this.deleteAux(0, this.pool, packet);
        this.myFiles.remove(file);
        System.out.println("Delete Time for file " + file + ": " + (System.currentTimeMillis() - before));
        return true;
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
    public void Reclaim(long spaceLeft) {
        /*for (Chunk chunk : this.chunks.values()) {
            String m = MessageType.createRemoved("1.0", (int) Server.getServer().getPeerId(), chunk.getFileId(), chunk.getChunkNo());
            DatagramPacket packet = new DatagramPacket(m.getBytes(), m.length(), Server.getServer().getMc().getAddress(), Server.getServer().getMc().getPort());
            Server.getServer().getPool().execute(() -> Server.getServer().getMc().send(packet));
        }*/
    }

    @Override
    public String State() throws RemoteException {
        String out = "";
        out += ("Backed Up Files Owned by the peer\n");
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
