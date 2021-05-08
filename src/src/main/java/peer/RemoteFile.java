package peer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to store metadata from files that the peer has backed up
 */
public class RemoteFile {
    ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap<>();
    private String fileId;
    private String serverName;
    Integer key;

    public RemoteFile(String fileId,String serverName,Integer key) {
        this.fileId = fileId;
        this.serverName = serverName;
        this.key = key;
        try {
            Files.createDirectories(Path.of(serverName + "/.rdata"));
            Files.createDirectories(Path.of(serverName + "/.rdata/" + fileId));
        } catch (IOException e) {
        }
    }

    /**
     * adds peer to the chunk's count
     * @param chunkNo
     * @param peerId
     */
    void addStored(int chunkNo, int peerId) {
        if (this.chunks.containsKey(chunkNo)) {
            if (!this.chunks.get(chunkNo).getPeerList().containsKey(peerId)) {
                this.chunks.get(chunkNo).getPeerList().put(peerId, true);
            }
            this.chunks.get(chunkNo).updateRdata();
        }
    }

    /**
     * deletes the chunks and the metadata stored on the disk
     */
    int delete() {
        int sum = 0;
        for (Chunk c : chunks.values()) {
            sum += c.getSize();
        }

        try {
            Files.walk(Path.of(serverName + "/.rdata/" + this.fileId))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Files.walk(Path.of(serverName + "/" + this.fileId))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return  sum;
    }

    /**
     * removes a chunk
     * @param chunkId
     * @return true if it could remove the chunk
     */
    public boolean deleteChunk(int chunkId) {
        if (chunks.containsKey(chunkId)) {
            chunks.remove(chunkId);
            Path rInfo = Path.of(serverName + "/.rdata/" + this.fileId + "/" + chunkId);
            Path chunkData = Path.of(serverName + "/" + this.fileId + "/" + chunkId);
            if (Files.exists(rInfo) && Files.exists(chunkData)) {
                try {
                    Files.delete(rInfo);
                    Files.delete(chunkData);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


    /**
     *
     * @return map that maps the chunkNo with the respective chunk
     */
    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return this.chunks;
    }

    /**
     *
     * @return file ID
     */
    public String getFileId() {
        return this.fileId;
    }
}
