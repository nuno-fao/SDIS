package sdis.server;

import java.util.ArrayList;
import java.util.List;

public class BackupFile {
    String fileID;
    List<Chunk> chunks = new ArrayList<Chunk>();

    public List<Chunk> getChunks() {
        return chunks;
    }

    BackupFile(String fileID){
        this.fileID = fileID;
    }

    void addChunk(long chunkNo, String fileName){
        chunks.add(new Chunk(fileID,chunkNo,fileName));
    }

    //todo melhorar o algoritmo
    Chunk getChunk(long chunkNo) throws ChunkNotFound {
        for(int i = 0; i < chunks.size(); i++){
            if(chunkNo == chunks.get(i).getChunkNo()){
                return chunks.get(i);
            }
        }
        throw new ChunkNotFound();
    }

}