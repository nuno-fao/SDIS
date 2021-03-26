package sdis;

import sdis.server.*;
import sdis.server.File;

import java.io.*;
import java.net.DatagramPacket;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;

public class Server extends UnicastRemoteObject implements RemoteInterface  {
    String accessPoint;
    String version;
    long peerId;
    MulticastHolder mc;
    MulticastHolder mdb;
    MulticastHolder mdr;
    ConcurrentHashMap<String, RemoteFile> storedFiles ;
    ConcurrentHashMap<String,File> myFiles ;


    ScheduledExecutorService pool = Executors.newScheduledThreadPool(100);

    public synchronized ScheduledExecutorService getPool() {
        return pool;
    }

    int chunkSize = 64000;

    public int getChunkSize() {
        return chunkSize;
    }

    String serverName ;

    public String getServerName() {
        return serverName;
    }

    static Server server = null;

    public static Server createServer(String version, long peerId, String accessPoint, Address mc, Address mdb, Address mdr) throws RemoteException {
        server = new Server(version,peerId,accessPoint,mc,mdb,mdr);
        return server;
    }
    public static Server getServer(){
        return server;
    }

    public MulticastHolder getMc() {
        return mc;
    }

    public MulticastHolder getMdb() {
        return mdb;
    }

    public MulticastHolder getMdr() {
        return mdr;
    }

    public ConcurrentHashMap<String, RemoteFile> getStoredFiles() {
        return storedFiles;
    }

    public ConcurrentHashMap<String, File> getMyFiles() {
        return myFiles;
    }

    private Server(String version, long peerId, String accessPoint, Address mc, Address mdb, Address mdr) throws RemoteException {
        super(0);
        this.accessPoint = accessPoint;
        this.version = version;
        this.peerId = peerId;

        this.serverName = Integer.toString((int) peerId)+"_folder";
        this.storedFiles = new ConcurrentHashMap<>();
        this.myFiles = new ConcurrentHashMap<>();

        this.mc = new MulticastHolder(mc.port, mc.address, chunkSize+500,chunkSize, (int) peerId);
        this.mdb = new MulticastHolder(mdb.port, mdb.address, chunkSize+500,chunkSize, (int) peerId);
        this.mdr = new MulticastHolder(mdr.port, mdr.address, chunkSize+500,chunkSize, (int) peerId);
        new Thread(this.mc).start();
        new Thread(this.mdb).start();
        new Thread(this.mdr).start();

        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
        }

        try {
            Files.createDirectories(Paths.get(peerId+"_folder"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Bind this object instance to the name "RmiServer"
        try {
            System.out.println(accessPoint);
            Naming.rebind(accessPoint, this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        for (File t: myFiles.values()) {
            System.out.println(t.getFileId());
        };
    }

    public static void main(String[] args) throws Exception{
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
    public String getAccessPoint() {
        return accessPoint;
    }

    public String getVersion() {
        return version;
    }

    public long getPeerId() {
        return peerId;
    }

    public void startRemoteObject(){
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
        }

        try {
            Naming.rebind(accessPoint, this);
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Backup(String filename, int replicationDegree) {
        long before = System.currentTimeMillis();
        Path newFilePath = Paths.get(filename);
        if(Files.exists(newFilePath)){
            File f;
            try {
                f = new File(filename,replicationDegree);
                this.myFiles.put(f.getFileId(),f);
            } catch (IOException e) {
                return;
            }

            try {
                BufferedReader io = Files.newBufferedReader(newFilePath);
                int  i = 0;
                while (true) {
                    char a[] = new char[chunkSize];
                    myFiles.get(f.getFileId()).getChunks().put(i,new Chunk(i,f.getFileId(),replicationDegree));
                    int size = io.read(a,0,chunkSize);
                    if( size == -1){
                        String message = MessageType.createPutchunk("1.0", (int) this.peerId,f.getFileId(),i,replicationDegree,"");
                        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length());
                        packet.setAddress(mdb.getAddress());
                        packet.setPort(mdb.getPort());
                        send(1,pool,packet,f.getFileId(),i,replicationDegree);
                        return;
                    }
                    String message;
                    if(size == chunkSize) {
                        message = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, new String(a));
                    }
                    else {
                        message = MessageType.createPutchunk("1.0", (int) this.peerId, f.getFileId(), i, replicationDegree, new String(a).substring(0,size));
                    }
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(),mdb.getAddress(),mdb.getPort());
                    packet.setLength(message.length());
                    int finalI = i;
                    pool.execute(() -> send(1,pool,packet,f.getFileId(), finalI,replicationDegree));
                    i++;
                    if(size < chunkSize)
                        break;
                }

            } catch (IOException e) {
            }
        }
        System.out.println("Backup Time: "+(System.currentTimeMillis()-before));
    }

    private void send(int i, ScheduledExecutorService pool, DatagramPacket packet,String fileId,int chunkNo,int repDegree){
        mdb.send(packet);
            pool.schedule(() -> {
                if(Server.getServer().getMyFiles().get(fileId).getReplicationDegree(chunkNo) < repDegree && i<16)
                    send(i*2,pool,packet,fileId,chunkNo,repDegree);
            },i,TimeUnit.SECONDS);
    }



    @Override
    public void Restore(String filename) {
    }

    @Override
    public void Delete(String filename) {

    }

    @Override
    public void Reclaim(long spaceLeft) {
    }

    @Override
    public String State() throws RemoteException {
        String out = "";
        out+=("Backed Up Files Owned by the peer\n");
        for (File f:myFiles.values()) {
            out+="    Name:               "+f.getName()+"\n";
            out+="    FileID:             "+f.getFileId()+"\n";
            out+="    Desired Rep Degree: "+f.getRepDegree()+"\n";
            out+="    CHUNKS: \n";
            for (Chunk c : f.getChunks().values()) {
                out+="        CHUNK NO:     "+c.getChunkNo()+"\n";
                out+="            Perceived Rep Degree:    "+c.getPeerCount()+"\n";
            }
        }
        return out;
    }


}
