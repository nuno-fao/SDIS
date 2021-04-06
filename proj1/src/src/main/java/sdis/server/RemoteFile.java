package sdis.server;

import sdis.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteFile {
    ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap<>();
    private String fileId;

    public RemoteFile(String fileId) {
        this.fileId = fileId;
        try {
            Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.rdata"));
            Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.rdata/" + fileId));
        } catch (IOException e) {
        }
    }

    void addStored(int chunkNo, int peerId) {
        if (this.chunks.containsKey(chunkNo)) {
            if (!this.chunks.get(chunkNo).getPeerList().containsKey(peerId)) {
                this.chunks.get(chunkNo).getPeerList().put(peerId, true);
                this.chunks.get(chunkNo).update("rdata");
            }
        }
    }

    void delete() {
        int sum = 0;
        for (Chunk c : chunks.values()) {
            sum += c.getSize();
        }
        Server.getServer().getCurrentSize().addAndGet(-sum);

        try {
            Files.walk(Path.of(Server.getServer().getServerName() + "/.rdata/" + this.fileId))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Files.walk(Path.of(Server.getServer().getServerName() + "/" + this.fileId))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return this.chunks;
    }

    public String getFileId() {
        return this.fileId;
    }
}
