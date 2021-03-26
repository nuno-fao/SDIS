package sdis.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class Chunk {
    int chunkNo = 0;
    String fileId;
    int repDegree = 0;
    ConcurrentHashMap<Integer,Boolean> peerCount = new ConcurrentHashMap<>();

    public Chunk(int chunkNo, String fileId,int repDegree) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
    }

    public String load(String main_folder){
        try {
            byte f[] = Files.readAllBytes(Paths.get(main_folder+"/"+fileId+"/"+chunkNo));
            return new String(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
