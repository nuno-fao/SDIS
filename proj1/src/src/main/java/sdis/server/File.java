package sdis.server;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class File {
    String fileID = "";
    String name;
    String editionTime;
    long size;

    private String getHashedString(String s){
        MessageDigest algo = null;
        try {
            algo = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger number = new BigInteger(1, algo.digest(s.getBytes(StandardCharsets.UTF_8)));
        StringBuilder hexString = new StringBuilder(number.toString(16));
        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }
        return hexString.toString();
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
