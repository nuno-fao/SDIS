package peer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
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

/**
 * Used to store the metadata of the local file to backup and make operations on its chunks
 */
public class File {
    private String fileId;
    private String name;
    private String editionTime = "";
    private ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap<>();
    private int numChunks = -1;
    private long time;
    String serverName;
    Integer key;

    public File(String name, String serverName,Integer key) throws IOException {
        this.name = name;
        Path file = Path.of(name);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        this.fileId = getHashedString(name + attr.size() + attr.lastModifiedTime().toMillis());
        this.time = System.currentTimeMillis();
        this.serverName = serverName;
        this.key = key;

        Files.createDirectories(Path.of(serverName + "/.ldata"));
        Files.createDirectories(Path.of(serverName+ "/.ldata/" + this.fileId));
    }
    public File(String name, String fileId, String serverName,Integer key) throws IOException {
        this.name = name;
        this.fileId = fileId;
        this.time = System.currentTimeMillis();
        this.serverName = serverName;
        this.key = key;

        Files.createDirectories(Path.of(serverName + "/.ldata"));
        Files.createDirectories(Path.of(serverName + "/.ldata/" + this.fileId));
    }

    /**
     *
     * @param s
     * @return the hash result of the param s
     */
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

    /**
     * Reads the file and retrieves the modification date and the size
     * @param name (path of the file)
     * @return
     */
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

    /**
     * returns the time the file was initiated
     * @return
     */
    public long getTime() {
        return this.time;
    }

    /**
     * sets the time the flie was initiated
     * @param time
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * @returnthe number of chunks the file has
     */
    public int getNumChunks() {
        return this.numChunks;
    }

    /**
     * Sets the number of chunks of the file
     * @param numChunks
     */
    synchronized public void setNumChunks(int numChunks) {
        this.numChunks = numChunks;
    }

    /**
     *
     * @return the hash map that maps the chunk ID to the Chunk instance
     */
    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return this.chunks;
    }

    /**
     * Puts a chunk c with mapped with the key
     * @param key
     * @param c
     */
    public void putChunk(int key, Chunk c) {
        chunks.put(key, c);
    }

    /**
     * Adds the peer to the chunk's peerlist and updates the chunk metadata file
     * @param chunkNo
     * @param peerId
     */
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


                Path path = Paths.get(serverName + "/.ldata/" + this.fileId + "/" + chunkNo);
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
                buffer.clear();
            }
        }
    }

    /**
     * removes peer (peerId) from every chunk
     * @param peerId
     */
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

    /**
     * rewrites every ldata info of every chunk
     */
    public void updateChunks(){
        for(Chunk chunk:chunks.values()){
            chunk.updateLdata(this.name);
        }
    }

    /**
     * checks if the peerId has a chunk of this file
     * @param peerId
     * @return
     */
    public boolean peerHasChunks(int peerId){
        for(Chunk chunk:chunks.values()){
            if(chunk.getPeerList().containsKey(peerId)){
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param chunkNo
     * @return the perceived replication degree of the chunk (chunkNo)
     */
    public Integer getReplicationDegree(int chunkNo) {
        return this.chunks.get(chunkNo).getPeerCount();
    }

    /**
     *
     * @return file unique fileId
     */
    public String getFileId() {
        return this.fileId;
    }

    /**
     *
     * @return file name/path
     */
    public String getName() {
        return this.name;
    }

    /**
     *
     * @return file edition time
     */
    String getEditionTime() {
        return this.editionTime;
    }

}
