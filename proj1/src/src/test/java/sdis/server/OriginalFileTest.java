package sdis.server;

import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
//https://www.geeksforgeeks.org/sha-256-hash-in-java/

public class OriginalFileTest {
    @Test
    public void TestLoad() throws NoSuchAlgorithmException, IOException {
        String filename = "testfile.txt";
        Path newFilePath = Paths.get(filename);
        try{
            Files.delete(newFilePath);
        }
        catch (Exception e){
            
        }
        Files.createFile(newFilePath);
        Files.write(newFilePath,"qwertyuiopasdfghjklçzxcvbnmqwer".getBytes());

        OriginalFile testOriginalFile = new OriginalFile(filename);

        MessageDigest algo = MessageDigest.getInstance("SHA-256");
        BigInteger number = new BigInteger(1, algo.digest(("testfile.txt32"+ testOriginalFile.getEditionTime()).getBytes(StandardCharsets.UTF_8)));
        StringBuilder hexString = new StringBuilder(number.toString(16));
        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }

        assertEquals(filename, testOriginalFile.getName());
        assertTrue(testOriginalFile.getEditionTime().length() > 0);
        assertEquals(hexString.toString(), testOriginalFile.getFileID());
        assertEquals(32, testOriginalFile.getSize());
    }
}
