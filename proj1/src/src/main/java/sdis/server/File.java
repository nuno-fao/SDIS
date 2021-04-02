package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class File {
    private String fileId;
    private String name;
    private String editionTime;
    private long size;
    private ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap<>();

    public File(String name, int repDegree) throws IOException {
        this.name = name;
        Path file = Path.of(name);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        this.editionTime = "" + attr.lastModifiedTime().toMillis();
        this.size = attr.size();
        this.fileId = getHashedString(name + (this.size) + this.editionTime);

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
        StringBuilder hexString = new StringBuilder(number.toString(16));
        String out = hexString.toString();
        while (out.length() < 64) {
            out += '0';
        }
        return out;
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

    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return this.chunks;
    }

    void addStored(int chunkNo, int peerId) {
        if (this.chunks.containsKey(chunkNo))
            if (!this.chunks.get(chunkNo).getPeerList().containsKey(peerId)) {
                this.chunks.get(chunkNo).getPeerList().put(peerId, true);
                try {
                    Files.write(Paths.get(Server.getServer().getServerName() + "/.ldata/" + this.fileId + "/" + chunkNo), (this.chunks.get(chunkNo).getPeerCount() + ";" + this.chunks.get(chunkNo).repDegree + ";" + this.name).getBytes());
                } catch (IOException e) {
                }
            }
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
