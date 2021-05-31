package peer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

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
        }
    }

    public File(String fileInfo, String serverName, long fileSize, String fileId) throws Exception {
        var i = fileInfo.split(";");
        try {
            Files.createDirectories(Path.of(serverName + "/stored"));
        } catch (IOException e) {
        }
        this.fileId = fileId;
        this.fileSize = fileSize;

        this.fileName = i[0];
        this.replicationDegree = Integer.parseInt(i[1]);

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
    public void deleteFile() {
        try {
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
        Path path = Paths.get(this.serverName + "/.locals/" + this.fileId);
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data = (this.fileName + ";" + this.replicationDegree).getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(data.length);

        buffer.put(data);
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
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public String getServerName() {
        return this.serverName;
    }

    public byte[] readCopyContent(){
        Path file = Path.of(serverName+ "/" + "stored" + "/" + fileId);
        try {
            byte[] contents = Files.readAllBytes(file);
            return contents;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
