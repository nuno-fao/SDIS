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
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Used to store the metadata of the local file to backup and make operations on its chunks
 */
public class File {
    String serverName;
    private String fileId;
    private String name;
    private int replicationDegree;
    private int firstChordPeer;

    public File(String name, String serverName, int replicationDegree, int firstChordPeer) throws IOException {
        this.name = name;
        Path file = Path.of(name);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        this.fileId = getHashedString(name + attr.size() + attr.lastModifiedTime().toMillis());
        this.serverName = serverName;
        this.replicationDegree = replicationDegree;
        this.firstChordPeer = firstChordPeer;

        try {
            Files.createDirectories(Path.of(serverName + "/.ldata"));
        } catch (IOException e) {
        }
    }

    public File(String name, String fileId, String serverName, int replicationDegree, int firstChordPeer) throws IOException {
        this.name = name;
        this.fileId = fileId;
        this.serverName = serverName;
        this.replicationDegree = replicationDegree;
        this.firstChordPeer = firstChordPeer;

        try {
            Files.createDirectories(Path.of(serverName + "/.ldata"));
        } catch (IOException e) {
        }
    }

    public File(String serverName, String fileInfo) throws Exception {
        var i = fileInfo.split(";");
        if (i.length != 4) {
            throw new Exception();
        }
        this.fileId = i[0];
        this.name = i[1];
        this.replicationDegree = Integer.parseInt(i[2]);
        this.firstChordPeer = Integer.parseInt(i[3]);
        this.serverName = serverName;

        try {
            Files.createDirectories(Path.of(serverName + "/.ldata"));
        } catch (IOException e) {
        }
    }

    /**
     * @param s
     * @return the hash result of the param s
     */
    private static String getHashedString(String s) {
        MessageDigest algo = null;
        try {
            algo = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger number = new BigInteger(1, algo.digest(s.getBytes(StandardCharsets.UTF_8)));
        StringBuilder out = new StringBuilder(number.toString(16));
        while (out.length() < 64) {
            out.append('0');
        }
        return out.toString();
    }


    /**
     * @param chunkNo
     * @return the perceived replication degree of the chunk (chunkNo)
     */
    public Integer getReplicationDegree(int chunkNo) {
        return this.replicationDegree;
    }

    /**
     * @return file unique fileId
     */
    public String getFileId() {
        return this.fileId;
    }

    /**
     * @return file name/path
     */
    public String getName() {
        return this.name;
    }

    public void saveMetadata() {
        Path path = Paths.get(serverName + "/.ldata" + fileId);
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data = (fileId + ";" + name + ";" + replicationDegree + ";" + firstChordPeer).getBytes();

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
}
