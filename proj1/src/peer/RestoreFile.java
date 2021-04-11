package peer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RestoreFile {
    private ConcurrentHashMap<Integer, byte[] > chunks;
    Integer numberOfChunks;
    AtomicInteger writing = new AtomicInteger(0);

    public void setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
    }

    public RestoreFile(ConcurrentHashMap<Integer, byte[]> chunks) {
        this.chunks = chunks;
        numberOfChunks= null;
    }

    public ConcurrentHashMap<Integer, byte[]> getChunks() {
        return chunks;
    }

    public Integer getNumberOfChunks() {
        return numberOfChunks;
    }
}
