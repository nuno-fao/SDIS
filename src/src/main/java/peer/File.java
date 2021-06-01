package peer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Used to store metadata from files that the peer has backed up
 */
public class File {
    private String fileId;
    private String fileName;
    private String serverName;
    private long fileSize;
    private int replicationDegree;

    public File(String fileId, String serverName, String fileName, long fileSize, int replicationDegree) {
        this.fileId = fileId;
        this.serverName = serverName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.replicationDegree = replicationDegree;
        try {
            Files.createDirectories(Path.of(serverName + "/stored"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File(String fileId, String serverName, long fileSize) {
        this.fileId = fileId;
        this.serverName = serverName;
        this.fileSize = fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public File(String fileInfo, String serverName, String fileId) throws Exception {
        var i = fileInfo.split(";");
        try {
            Files.createDirectories(Path.of(serverName + "/stored"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileId = fileId;
        this.fileSize = this.fileSize;

        this.fileName = i[0];
        this.replicationDegree = Integer.parseInt(i[1]);
        this.fileSize = Integer.parseInt(i[2]);
    }

    /**
     * @param s
     * @return the hash result of the param s
     */
    public static BigInteger getHashedString(String s) {
        MessageDigest algo = null;
        try {
            algo = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger number = new BigInteger(1, algo.digest(s.getBytes(StandardCharsets.UTF_8)));
        return number;
    }

    /**
     * deletes the file and metadata
     */
    public void deleteFile(AtomicLong currentSize) {
        try {
            currentSize.getAndAdd(-this.fileSize);
            Files.deleteIfExists(Path.of(this.serverName + "/stored/" + this.fileId));
            Files.deleteIfExists(Path.of(this.serverName + "/.locals/" + this.fileId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileName() {
        return this.fileName;
    }

    /**
     * @return file ID
     */
    public String getFileId() {
        return this.fileId;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public void saveMetadata() {
        try {
            Files.createDirectories(Path.of(this.serverName + "/.locals"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = (this.fileName + ";" + this.replicationDegree + ";" + this.fileSize).getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        buffer.flip();
        try {
            Peer.write(this.serverName + "/.locals/" + this.fileId, buffer, 0, true, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getReplicationDegree() {
        return this.replicationDegree;
    }

    public byte[] readCopyContent() {
        Path file = Path.of(this.serverName + "/" + "stored" + "/" + this.fileId);
        try {
            byte[] contents = Files.readAllBytes(file);
            return contents;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
