package sdis.server;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class File {
    String fileID ;
    String name;
    String editionTime;
    long size;

    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return chunks;
    }

    ConcurrentHashMap<Integer,Chunk> chunks = new ConcurrentHashMap<>();

    public void addStored(int chunkNo,int peerId){
        if(chunks.containsKey(chunkNo))
            chunks.get(chunkNo).peerCount.put(peerId,true);
    }

    public Integer getReplicationDegree(int chunkNo) {
        return chunks.get(chunkNo).peerCount.size();
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

    public File(String name) throws IOException {
        this.name = name;
        Path file = Path.of(name);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        editionTime = ""+attr.lastModifiedTime().toMillis();
        size = attr.size();
        fileID = getHashedString(name+(size)+editionTime);
    }

    public long getSize() {
        return size;
    }

    public String getFileID() {
        return fileID;
    }

    public String getName() {
        return name;
    }

    public String getEditionTime() {
        return editionTime;
    }

}
