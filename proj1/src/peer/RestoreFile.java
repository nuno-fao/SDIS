package peer;

import java.util.concurrent.ConcurrentHashMap;

public class RestoreFile {
    ConcurrentHashMap<Integer, byte[] > chunks;
    Integer numberOfChunks;

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
