package peer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * used to store the data of a file while restoring it
 */
public class RestoreFile {
    private ConcurrentHashMap<Integer, byte[] > chunks;
    Integer numberOfChunks;
    Integer key;

    public void setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
    }

    public RestoreFile(ConcurrentHashMap<Integer, byte[]> chunks,Integer key) {
        this.chunks = chunks;
        numberOfChunks= null;
        this.key = key;
    }

    public ConcurrentHashMap<Integer, byte[]> getChunks() {
        return chunks;
    }

    public Integer getNumberOfChunks() {
        return numberOfChunks;
    }
}
