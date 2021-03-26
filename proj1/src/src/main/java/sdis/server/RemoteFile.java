package sdis.server;

import java.util.concurrent.ConcurrentHashMap;

public class RemoteFile {
    private String fileId;
    ConcurrentHashMap<Integer,Chunk> chunks = new ConcurrentHashMap<>();
    RemoteFile(String fileId){
        this.fileId = fileId;
    }

    public void addStored(int chunkNo,int peerId){
        if(chunks.containsKey(chunkNo))
            chunks.get(chunkNo).peerCount.put(peerId,true);
    }

    //true if it doesn't already exists
    public boolean addChunk(Chunk chunk){
        if(!chunks.containsKey(chunk.chunkNo)) {
            chunks.put(chunk.chunkNo, chunk);
            return true;
        }
        return false;
    }

    public Chunk getChunk(int chunkNo){
        if(chunks.containsKey(chunkNo))
            return chunks.get(chunkNo);
        return null;
    }

}
