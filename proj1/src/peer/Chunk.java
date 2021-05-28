package peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardOpenOption.*;

/**
 * Used to store the chunk metadata
 */
public class Chunk {
    private int repDegree = 0;
    private int realDegree = 0;
    private int chunkNo = 0;
    private String fileId;
    private ConcurrentHashMap<Integer, Boolean> peerCount = null;
    private AtomicBoolean shallSend = new AtomicBoolean(true);
    private int size = 0;

    public Chunk(int chunkNo, String fileId, int repDegree) {

        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.peerCount = new ConcurrentHashMap<>();
    }

    Chunk(int chunkNo, String fileId, int repDegree, int size) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.peerCount = new ConcurrentHashMap<>();
        this.size = size;
    }

    public Chunk(int chunkNo, String fileId, int repDegree, int realDegree, int size) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.realDegree = realDegree;
        this.size = size;
    }

    /**
     *
     * @return chunk perceived degree
     */
    public int getRealDegree() {
        return realDegree;
    }

    /**
     *
     * @return chunk size in bytes
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size of the chunk
     * @param size
     */
    void setSize(int size) {
        this.size = size;
    }

    /**
     *
     * @return the fileId of the chunk respective file
     */
    public String getFileId() {
        return this.fileId;
    }

    /**
     * Substracts 1 to the chunk perceived degree
     */
    void subtractRealDegree() {
        this.realDegree--;
    }

    /**
     * Adds 1 to the chunk perceived degree
     */
    synchronized public void  addToRealDegree(){
        realDegree++;
    }

    /**
     *
     * @return the id of the chunk(chunkNo)
     */
    public int getChunkNo() {
        return this.chunkNo;
    }

    /**
     *
     * @return the hashmap with the peers that had stored the chunk(used as set)
     */
    public ConcurrentHashMap<Integer, Boolean> getPeerList() {
        if (this.peerCount == null) {
            this.peerCount = new ConcurrentHashMap<>();
        }
        return this.peerCount;
    }

    /**
     *
     * @return the number of peers that stored the chunk
     */
    public int getPeerCount() {
        if (this.peerCount != null) {
            return this.peerCount.size();
        }
        return this.realDegree;
    }

    /**
     *
     * @return the desired chunk degree
     */
    public int getRepDegree() {
        return this.repDegree;
    }

    /**
     * Sets the chunk desired degree
     * @param repDegree
     */
    void setRepDegree(int repDegree) {
        this.repDegree = repDegree;
    }

    /**
     *
     * @return true if the perceived degree is lower than the desired degree
     */
    boolean shallSend() {
        return getPeerList().size() < repDegree;
    }

    /**
     * Writes the chunk data to the respective file(remote file)
     */
    synchronized void updateRdata() {
        if (repDegree != -1) {
            Path path = Paths.get(Peer.getServer().getServerName() + "/.rdata/" + this.fileId + "/" + this.chunkNo);
            AsynchronousFileChannel fileChannel = null;
            try {
                fileChannel = AsynchronousFileChannel.open(
                        path, WRITE, TRUNCATE_EXISTING, CREATE);
            } catch (IOException e) {
            }

            byte out[] = (this.getPeerCount() + ";" + this.repDegree + ";" + size).getBytes();
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

    /**
     * Writes the chunk data to the respective file(local file)
     */
    synchronized void updateLdata(String filename) {
        if (repDegree != -1) {
            Path path = Paths.get(Peer.getServer().getServerName() + "/.ldata/" + this.fileId + "/" + this.chunkNo);
            AsynchronousFileChannel fileChannel = null;
            try {
                fileChannel = AsynchronousFileChannel.open(
                        path, WRITE, TRUNCATE_EXISTING, CREATE);
            } catch (IOException e) {
            }

            StringBuilder sb = new StringBuilder();
            sb.append((getPeerCount() + ";" + getRepDegree() + ";" + filename + "\n"));
            for (Iterator<Integer> it = getPeerList().keys().asIterator(); it.hasNext(); ) {
                sb.append(it.next() + ";");
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
