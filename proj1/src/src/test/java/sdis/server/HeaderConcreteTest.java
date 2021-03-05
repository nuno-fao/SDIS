package sdis.server;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HeaderConcreteTest {

    @Test
    public void TestHeaderReaderEmptyRequest() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        try {
            HeaderConcrete.getHeaders(" 1.0  PUTCHUNK  3 123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 8 7 \r\n \r\n\r\n  ");
            fail();
        }
        catch (IncorrectHeader ignored){

        }
    }

    @Test
    public void TestHeaderReaderPutChunkCorrect() throws ChunkNoError, FileIDError, IncorrectHeader, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  PUTCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " +
                "7 " +
                "\r\n\r\n  ").get(0);
        assertEquals("1.0",h.getVersion());
        assertEquals(MessageType.PUTCHUNK,h.getMessageType());
        assertEquals(3,(int)(h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",h.getFileID());
        assertEquals(8,(int)(h.getChunkNo()));
        assertEquals(7,(int)(h.getReplicationDeg()));
    }

    @Test
    public void TestHeaderReaderPutChunkCRLF1() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 ");
            fail();
        }
        catch (IncorrectHeader ignored){

        }
    }
    @Test
    public void TestHeaderReaderPutChunkCRLF2() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 \r\n \r\n");
            fail();
        }
        catch (IncorrectHeader ignored){

        }
    }
    @Test
    public void TestHeaderReaderPutChunkCRLF3() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, IncorrectHeader {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 \r\n");
            fail();
        }
        catch (NewLineError ignored){

        }
    }

    @Test
    public void TestHeaderReaderPutChunkMessageTypeError() throws ReplicationDegError, IncorrectHeader, FileIDError, SenderIdError, ChunkNoError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUN" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (MessageTypeError ignored){

        }
    }


    @Test
    public void TestHeaderReaderPutChunkSenderIdError1() throws ReplicationDegError, IncorrectHeader, MessageTypeError, FileIDError, ChunkNoError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    -1 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (SenderIdError ignored){

        }
    }

    @Test
    public void TestHeaderReaderPutChunkSenderIdError2() throws ReplicationDegError, IncorrectHeader, MessageTypeError, FileIDError, ChunkNoError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    f " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (SenderIdError ignored){

        }
    }

    @Test
    public void TestHeaderReaderPutChunkSenderIdError3() throws ReplicationDegError, IncorrectHeader, MessageTypeError, FileIDError, ChunkNoError, SenderIdError, NewLineError {
        HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    0 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
    }


    @Test
    public void TestHeaderReaderPutChunkFileIDError() throws ReplicationDegError, IncorrectHeader, MessageTypeError, SenderIdError, ChunkNoError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc457913232112 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (FileIDError ignored){

        }
    }


    @Test
    public void TestHeaderReaderPutChunkChunkNoError1() throws ReplicationDegError, IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "1234567 " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (ChunkNoError ignored){

        }
    }

    @Test
    public void TestHeaderReaderPutChunkChunkNoError2() throws ReplicationDegError, IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "f " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (ChunkNoError ignored){

        }
    }
    @Test
    public void TestHeaderReaderPutChunkChunkNoError3() throws ReplicationDegError, IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "-1 " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (ChunkNoError ignored){

        }
    }

    @Test
    public void TestHeaderReaderPutChunkChunkNoError4() throws ReplicationDegError, IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, ChunkNoError, NewLineError {
        HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "0 " +
                    "7 " + "\r\n\r\n");
    }

    @Test
    public void TestHeaderReaderPutChunkChunkNoError5() throws ReplicationDegError, IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "7111111 " +
                    "7 " + "\r\n\r\n");
            fail();
        }
        catch (ChunkNoError ignored){

        }
    }


    @Test
    public void TestHeaderReaderPutChunkReplicationDegError1() throws IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, ChunkNoError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "72 " + "\r\n\r\n");
            fail();
        }
        catch (ReplicationDegError ignored){

        }
    }
    @Test
    public void TestHeaderReaderPutChunkReplicationDegError2() throws IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, ChunkNoError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "f " + "\r\n\r\n");
            fail();
        }
        catch (ReplicationDegError ignored){

        }
    }
    @Test
    public void TestHeaderReaderPutChunkReplicationDegError3() throws IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, ChunkNoError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "-1 "+"\r\n\r\n");
            fail();
        }
        catch (ReplicationDegError ignored){

        }
    }
    @Test
    public void TestHeaderReaderPutChunkReplicationDegError4() throws IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, ChunkNoError, ReplicationDegError, NewLineError {
        HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "0 " +
                    "0 " + "\r\n\r\n");
    }

    @Test
    public void TestHeaderReaderPutChunkIncorrectHeader() throws NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "72 "+"\r\n\r\n");
            fail();
        }
        catch (Exception | FileIDError | ChunkNoError | SenderIdError | IncorrectHeader | ReplicationDegError | MessageTypeError ignored){

        }
    }

    @Test
    public void TestHeaderReaderStoredCorrect() throws ChunkNoError, FileIDError, IncorrectHeader, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  STORED " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 "+"\r\n\r\n  ").get(0);
        assertEquals("1.0",h.getVersion());
        assertEquals(MessageType.STORED,h.getMessageType());
        assertEquals(3,(int)(h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",h.getFileID());
        assertEquals(8,(int)(h.getChunkNo()));
    }
    @Test
    public void TestHeaderReaderStored1() throws FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  STORED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        }
        catch (ChunkNoError | IncorrectHeader ignored){

        }
    }

    @Test
    public void TestHeaderReaderStored2() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, IncorrectHeader {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  STORED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  "+"\r\n\r\n  ");
            fail();
        }
        catch (NewLineError ignored){

        }
    }

    @Test
    public void TestHeaderReaderMultCorrect() throws ChunkNoError, FileIDError, IncorrectHeader, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        List<Header> list = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  STORED " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 "+
                "\r\n  " +
                " 1.0" +
                "  PUTCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " +
                "7 "+"\r\n\r\n  ");
        assertEquals("1.0",list.get(0).getVersion());
        assertEquals(MessageType.STORED,list.get(0).getMessageType());
        assertEquals(3,(int)(list.get(0).getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",list.get(0).getFileID());
        assertEquals(8,(int)(list.get(0).getChunkNo()));


        assertEquals("1.0",list.get(1).getVersion());
        assertEquals(MessageType.PUTCHUNK,list.get(1).getMessageType());
        assertEquals(3,(int)(list.get(1).getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",list.get(1).getFileID());
        assertEquals(8,(int)(list.get(1).getChunkNo()));
        assertEquals(7,(int)(list.get(1).getReplicationDeg()));
    }

    @Test
    public void TestHeaderReaderGetChunkCorrect() throws ChunkNoError, FileIDError, IncorrectHeader, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  GETCHUNK " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 "+"\r\n\r\n  ").get(0);
        assertEquals("1.0",h.getVersion());
        assertEquals(MessageType.GETCHUNK,h.getMessageType());
        assertEquals(3,(int)(h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",h.getFileID());
        assertEquals(8,(int)(h.getChunkNo()));
    }
    @Test
    public void TestHeaderReaderGetChunk1() throws FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  GETCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        }
        catch (ChunkNoError | IncorrectHeader ignored){

        }
    }

    @Test
    public void TestHeaderReaderGetChunk2() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, IncorrectHeader {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  GETCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  "+"\r\n\r\n  ");
            fail();
        }
        catch (NewLineError ignored){

        }
    }

    @Test
    public void TestHeaderReaderChunkCorrect() throws ChunkNoError, FileIDError, IncorrectHeader, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  CHUNK " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 "+"\r\n\r\n  ").get(0);
        assertEquals("1.0",h.getVersion());
        assertEquals(MessageType.CHUNK,h.getMessageType());
        assertEquals(3,(int)(h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",h.getFileID());
        assertEquals(8,(int)(h.getChunkNo()));
    }
    @Test
    public void TestHeaderReaderChunk1() throws FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  CHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        }
        catch (ChunkNoError | IncorrectHeader ignored){

        }
    }

    @Test
    public void TestHeaderReaderChunk2() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, IncorrectHeader {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  CHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  "+"\r\n\r\n  ");
            fail();
        }
        catch (NewLineError ignored){

        }
    }

    @Test
    public void TestHeaderReaderDeleteCorrect() throws ChunkNoError, FileIDError, IncorrectHeader, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  DELETE " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "\r\n\r\n  ").get(0);
        assertEquals("1.0",h.getVersion());
        assertEquals(MessageType.DELETE,h.getMessageType());
        assertEquals(3,(int)(h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",h.getFileID());
    }
    @Test
    public void TestHeaderReaderDelete1() throws SenderIdError, MessageTypeError, ReplicationDegError, NewLineError, IncorrectHeader, ChunkNoError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  CHUNK" +
                    "    3 " +
                    "\r\n\r\n  ");
            fail();
        }
        catch (FileIDError ignored){

        }
    }

    @Test
    public void TestHeaderReaderDelete2() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, IncorrectHeader {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  DELETE" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +"\r\n\r\n  ");
            fail();
        }
        catch (NewLineError ignored){

        }
    }

    @Test
    public void TestHeaderReaderRemovedCorrect() throws ChunkNoError, FileIDError, IncorrectHeader, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  REMOVED " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 "+"\r\n\r\n  ").get(0);
        assertEquals("1.0",h.getVersion());
        assertEquals(MessageType.REMOVED,h.getMessageType());
        assertEquals(3,(int)(h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123",h.getFileID());
        assertEquals(8,(int)(h.getChunkNo()));
    }
    @Test
    public void TestHeaderReaderRemoved1() throws FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, NewLineError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  REMOVED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        }
        catch (ChunkNoError | IncorrectHeader ignored){

        }
    }

    @Test
    public void TestHeaderReaderRemoved2() throws ChunkNoError, FileIDError, SenderIdError, MessageTypeError, ReplicationDegError, IncorrectHeader {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  REMOVED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  "+"\r\n\r\n  ");
            fail();
        }
        catch (NewLineError ignored){

        }
    }

}