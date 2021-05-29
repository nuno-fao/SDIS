package peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Used to store metadata from files that the peer has backed up
 */
public class RemoteFile {
    private String fileId;
    private String fileName;
    private String serverName;
    private long fileSize;

    public RemoteFile(String fileId, String serverName, String fileName) {
        this.fileId = fileId;
        this.serverName = serverName;
        this.fileName = fileName;
        try {
            Files.createDirectories(Path.of(serverName));
        } catch (IOException e) {
        }
        try {
            Files.createDirectories(Path.of(serverName + "/stored"));
        } catch (IOException e) {
        }
    }

    public RemoteFile(String fileInfo, String serverName) throws Exception {
        var i = fileInfo.split(";");
        if (i.length != 2) {
            throw new Exception();
        }
        this.fileId = i[0];
        this.fileName = i[1];

        try {
            Files.createDirectories(Path.of(serverName));
        } catch (IOException e) {
        }
        try {
            Files.createDirectories(Path.of(serverName + "/stored"));
        } catch (IOException e) {
        }
    }

    /**
     * deletes the file and metadata
     */
    public void deleteFile() {
        try {
            Files.deleteIfExists(Path.of(this.serverName + "/stored/" + this.fileName));
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

    public void writeToFile(byte[] data) {
        Path path = Paths.get(this.serverName + "/" + this.fileId);
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void saveMetadata() {
        Path path = Paths.get(this.serverName + "/.rdata" + this.fileId);
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data = (this.fileId + ";" + this.fileName).getBytes();

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
