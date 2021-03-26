package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteFile {
    private String fileId;
    ConcurrentHashMap<Integer,Chunk> chunks = new ConcurrentHashMap<>();
    RemoteFile(String fileId){
        this.fileId = fileId;
        try {
            Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.rdata"));
            Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.rdata/"+fileId));
        } catch (IOException e) {
        }
    }
    public void addStored(int chunkNo,int peerId){
        if(chunks.containsKey(chunkNo))
            if(!chunks.get(chunkNo).getPeerList().containsKey(peerId)) {
                chunks.get(chunkNo).getPeerList().put(peerId, true);
                try {
                    Files.write(Paths.get(Server.getServer().getServerName()+"/.rdata/"+fileId+"/"+chunkNo),String.valueOf(chunks.get(chunkNo).getPeerCount()+";"+chunks.get(chunkNo).repDegree).getBytes());
                } catch (IOException e) {
                }
            }
    }
}
