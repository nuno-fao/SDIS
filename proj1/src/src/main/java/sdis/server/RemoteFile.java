package sdis.server;

import sdis.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        if (this.chunks.containsKey(chunkNo))
            if (!this.chunks.get(chunkNo).getPeerList().containsKey(peerId)) {
                this.chunks.get(chunkNo).getPeerList().put(peerId, true);
                try {
                    Files.write(Paths.get(Server.getServer().getServerName() + "/.rdata/" + this.fileId + "/" + chunkNo), (this.chunks.get(chunkNo).getPeerCount() + ";" + this.chunks.get(chunkNo).repDegree).getBytes());
                } catch (IOException e) {
                }
            }
    }

    void delete() {
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
