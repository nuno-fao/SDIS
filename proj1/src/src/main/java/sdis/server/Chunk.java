package sdis.server;

public class Chunk {
    String fileID;
    long chunkNo;
    String fileName;

    public Chunk(String fileID, long chunkNo,String fileName) {
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileID() {
        return fileID;
    }

    public long getChunkNo() {
        return chunkNo;
    }
}

class ChunkNotFound extends Throwable {

}

