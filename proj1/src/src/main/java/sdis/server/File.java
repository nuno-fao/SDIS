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
    String fileId;
    String name;
    String editionTime;
    long size;
    int repDegree;

    public int getRepDegree() {
        return repDegree;
    }

    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return chunks;
    }

    ConcurrentHashMap<Integer,Chunk> chunks = new ConcurrentHashMap<>();

    public void addStored(int chunkNo,int peerId){
        if(chunks.containsKey(chunkNo))
            if(!chunks.get(chunkNo).getPeerList().containsKey(peerId)) {
                chunks.get(chunkNo).getPeerList().put(peerId, true);
                try {
                    Files.write(Paths.get(Server.getServer().getServerName()+"/.ldata/"+fileId+"/"+chunkNo), (chunks.get(chunkNo).getPeerCount() + ";" + chunks.get(chunkNo).repDegree + ";" + name).getBytes());
                } catch (IOException e) {
                }
            }
    }

    public Integer getReplicationDegree(int chunkNo) {
        return chunks.get(chunkNo).getPeerCount();
    }

    private String getHashedString(String s){
        MessageDigest algo = null;
        try {
            algo = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger number = new BigInteger(1, algo.digest(s.getBytes(StandardCharsets.UTF_8)));
        StringBuilder hexString = new StringBuilder(number.toString(16));
        String out = hexString.toString();
        while (out.length() < 64)
        {
            out+='0';
        }
        return out;
    }

    public File(String name,int repDegree) throws IOException {
        this.name = name;
        Path file = Path.of(name);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        editionTime = ""+attr.lastModifiedTime().toMillis();
        size = attr.size();
        fileId = getHashedString(name+(size)+editionTime);
        this.repDegree = repDegree;

        Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.ldata"));
        Files.createDirectories(Path.of(Server.getServer().getServerName() + "/.ldata/"+ fileId));
    }

    public long getSize() {
        return size;
    }

    public String getFileId() {
        return fileId;
    }

    public String getName() {
        return name;
    }

    public String getEditionTime() {
        return editionTime;
    }

}
