package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class File {
    private AtomicInteger s = new AtomicInteger(0);
    private String fileId;
    private String name;
    private String editionTime;
    private long size;
    private ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap<>();
    private int numChunks = -1;
    private long time;

    public File(String name, int repDegree) throws IOException {
        this.name = name;
        Path file = Path.of(name);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        this.editionTime = "" + attr.lastModifiedTime().toMillis();
        this.size = attr.size();
        this.fileId = getHashedString(name + (this.size) + this.editionTime);
        this.time = System.currentTimeMillis();

        Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.ldata"));
        Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.ldata/" + this.fileId));
    }

    private static String getHashedString(String s) {
        MessageDigest algo = null;
        try {
            algo = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger number = new BigInteger(1, algo.digest(s.getBytes(StandardCharsets.UTF_8)));
        StringBuilder out = new StringBuilder(number.toString(16));
        while (out.length() < 64) {
            out.append('0');
        }
        return out.toString();
    }

    public static String getFileInfo(String name) {
        if (Files.exists(Path.of(name))) {
            Path file = Path.of(name);

            BasicFileAttributes attr = null;
            try {
                attr = Files.readAttributes(file, BasicFileAttributes.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String editionTime = "" + attr.lastModifiedTime().toMillis();
            long size = attr.size();
            return getHashedString(name + (size) + editionTime);
        }
        return null;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getNumChunks() {
        return this.numChunks;
    }

    synchronized public void setNumChunks(int numChunks) {
        this.numChunks = numChunks;
    }

    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return this.chunks;
    }

    public void putChunk(int key, Chunk c) {
        chunks.put(key, c);
    }

    void addStored(int chunkNo, int peerId) {
        if (this.chunks.containsKey(chunkNo)) {
            Chunk c = this.chunks.get(chunkNo);
            if (!c.getPeerList().containsKey(peerId)) {
                c.getPeerList().put(peerId, true);
                StringBuilder sb = new StringBuilder();
                sb.append((c.getPeerCount() + ";" + c.getRepDegree() + ";" + this.name + "\n"));
                for (Iterator<Integer> it = c.getPeerList().keys().asIterator(); it.hasNext(); ) {
                    sb.append(it.next() + ";");
                }


                Path path = Paths.get(Server.getServer().getServerName() + "/.ldata/" + this.fileId + "/" + chunkNo);
                AsynchronousFileChannel fileChannel = null;
                try {
                    fileChannel = AsynchronousFileChannel.open(
                            path, WRITE, CREATE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte out[] = sb.toString().getBytes();
                ByteBuffer buffer = ByteBuffer.allocate(out.length);

                buffer.put(out);
                buffer.flip();

                Future<Integer> operation = fileChannel.write(buffer, 0);
                buffer.clear();
            }
        }
    }

    public void removePeerFromChunks(int peerId){
        for(Chunk chunk:chunks.values()){
            if (chunk.getPeerList().containsKey(peerId)) {
                chunk.getPeerList().remove(peerId);
                if(chunk.getPeerList().size()==0){
                    chunks.remove(chunk.getChunkNo());
                }
            }
        }
    }

    public boolean peerHasChunks(int peerId){
        for(Chunk chunk:chunks.values()){
            if(chunk.getPeerList().containsKey(peerId)){
                return true;
            }
        }
        return false;
    }

    public Integer getReplicationDegree(int chunkNo) {
        return this.chunks.get(chunkNo).getPeerCount();
    }

    long getSize() {
        return this.size;
    }

    public String getFileId() {
        return this.fileId;
    }

    public String getName() {
        return this.name;
    }

    String getEditionTime() {
        return this.editionTime;
    }

}
