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

    public RemoteFile(String fileId) {
        this.fileId = fileId;
        try {
            Files.createDirectories(Path.of(Peer.getServer().getServerName() + "/.rdata"));
            Files.createDirectories(Path.of(Peer.getServer().getServerName() + "/.rdata/" + fileId));
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
    void delete() {
        int sum = 0;
        for (Chunk c : chunks.values()) {
            sum += c.getSize();
        }
        Peer.getServer().getCurrentSize().addAndGet(-sum);

        try {
            Files.walk(Path.of(Peer.getServer().getServerName() + "/.rdata/" + this.fileId))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Files.walk(Path.of(Peer.getServer().getServerName() + "/" + this.fileId))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * removes a chunk
     * @param chunkId
     * @return true if it could remove the chunk
     */
    public boolean deleteChunk(int chunkId) {
        if (chunks.containsKey(chunkId)) {
            chunks.remove(chunkId);
            Path rInfo = Path.of(Peer.getServer().getServerName() + "/.rdata/" + this.fileId + "/" + chunkId);
            Path chunkData = Path.of(Peer.getServer().getServerName() + "/" + this.fileId + "/" + chunkId);
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
