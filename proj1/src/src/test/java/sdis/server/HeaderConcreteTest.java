package sdis.server;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HeaderConcreteTest {

    @Test
    public void TestHeaderReaderEmptyRequest() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders(" 1.0  PUTCHUNK  3 123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 8 7 \r\n \r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkCorrect() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  PUTCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " +
                "7 " +
                "\r\n\r\n  ").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.PUTCHUNK, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
        assertEquals(8, (int) (h.getChunkNo()));
        assertEquals(7, (int) (h.getReplicationDeg()));
    }

    @Test
    public void TestHeaderReaderPutChunkCorrect2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  PUTCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " +
                "7 " +
                "\r\n\r\n").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.PUTCHUNK, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
        assertEquals(8, (int) (h.getChunkNo()));
        assertEquals(7, (int) (h.getReplicationDeg()));
    }

    @Test
    public void TestHeaderReaderPutChunkCorrect3() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  GETCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " +
                "\r\n\r\n").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.GETCHUNK, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
        assertEquals(8, (int) (h.getChunkNo()));
    }

    @Test
    public void TestHeaderReaderPutChunkCRLF1() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkCRLF2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 \r\n \r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkCRLF3() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 \r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkMessageTypeError() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUN" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }


    @Test
    public void TestHeaderReaderPutChunkParseError11() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    -1 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError21() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    f " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError31() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  PUTCHUNK" +
                "    0 " +
                "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                "8 " +
                "7 " + "\r\n\r\n");
    }


    @Test
    public void TestHeaderReaderPutChunkParseError1() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc457913232112 " +
                    "8 " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }


    @Test
    public void TestHeaderReaderPutChunkParseError12() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "1234567 " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError22() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "f " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError32() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "-1 " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError42() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  PUTCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                "0 " +
                "7 " + "\r\n\r\n");
    }

    @Test
    public void TestHeaderReaderPutChunkParseError5() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "7111111 " +
                    "7 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }


    @Test
    public void TestHeaderReaderPutChunkParseError13() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "72 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "f " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError3() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "8 " +
                    "-1 " + "\r\n\r\n");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderPutChunkParseError4() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  PUTCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                "0 " +
                "0 " + "\r\n\r\n");
    }

    @Test
    public void TestHeaderReaderPutChunkParseError() throws ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  PUTCHUNK" +
                    "72 " + "\r\n\r\n");
            fail();
        } catch (Exception | ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderStoredCorrect() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  STORED " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " + "\r\n\r\n  ").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.STORED, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
        assertEquals(8, (int) (h.getChunkNo()));
    }

    @Test
    public void TestHeaderReaderStored1() throws ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  STORED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderStored2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  STORED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  " + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderMultCorrect() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        List<Header> list = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  STORED " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " +
                "\r\n  " +
                " 1.0" +
                "  PUTCHUNK" +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " +
                "7 " + "\r\n\r\n  ");
        assertEquals("1.0", list.get(0).getVersion());
        assertEquals(MessageType.STORED, list.get(0).getMessageType());
        assertEquals(3, (int) (list.get(0).getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", list.get(0).getFileID());
        assertEquals(8, (int) (list.get(0).getChunkNo()));


        assertEquals("1.0", list.get(1).getVersion());
        assertEquals(MessageType.PUTCHUNK, list.get(1).getMessageType());
        assertEquals(3, (int) (list.get(1).getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", list.get(1).getFileID());
        assertEquals(8, (int) (list.get(1).getChunkNo()));
        assertEquals(7, (int) (list.get(1).getReplicationDeg()));
    }

    @Test
    public void TestHeaderReaderGetChunkCorrect() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  GETCHUNK " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " + "\r\n\r\n  ").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.GETCHUNK, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
        assertEquals(8, (int) (h.getChunkNo()));
    }

    @Test
    public void TestHeaderReaderGetChunk1() throws ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  GETCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderGetChunk2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  GETCHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  " + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderChunkCorrect() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  CHUNK " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " + "\r\n\r\n  ").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.CHUNK, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
        assertEquals(8, (int) (h.getChunkNo()));
    }

    @Test
    public void TestHeaderReaderChunk1() throws ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  CHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderChunk2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  CHUNK" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  " + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderDeleteCorrect() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  DELETE " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "\r\n\r\n  ").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.DELETE, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
    }

    @Test
    public void TestHeaderReaderDelete1() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  CHUNK" +
                    "    3 " +
                    "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderDelete2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  DELETE" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderRemovedCorrect() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        Header h = HeaderConcrete.getHeaders("" +
                " 1.0" +
                "  REMOVED " +
                "    3 " +
                "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 " +
                "8 " + "\r\n\r\n  ").get(0);
        assertEquals("1.0", h.getVersion());
        assertEquals(MessageType.REMOVED, h.getMessageType());
        assertEquals(3, (int) (h.getSenderID()));
        assertEquals("123aeb46ae7de0923432432123aeb46ae7de0923432432aefbc4579132321123", h.getFileID());
        assertEquals(8, (int) (h.getChunkNo()));
    }

    @Test
    public void TestHeaderReaderRemoved1() throws ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  REMOVED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 "
                    + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

    @Test
    public void TestHeaderReaderRemoved2() throws ParseError, ParseError, ParseError, ParseError, ParseError, ParseError {
        try {
            HeaderConcrete.getHeaders("" +
                    " 1.0" +
                    "  REMOVED" +
                    "    3 " +
                    "123aeb46ae7de0923432432123aeb46ae7de0923432432Aefbc4579132321123 " +
                    "2 " +
                    "9  " + "\r\n\r\n  ");
            fail();
        } catch (ParseError ignored) {

        }
    }

}