package peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Enum with a message type for every kind of message, each enum element knows how to process itself
 */
public enum MessageType {
    PUTFILE {
        @Override
        public int process(Header h, String[] argsList) throws ParseError {
            this.processFileID(h, argsList[1]);
            this.processReplicationDeg(h, argsList[2]);
            this.processAddress(h, argsList[3]);
            this.processPort(h, argsList[4]);
            return 5;
        }
    },
    GETFILE {
        @Override
        public int process(Header h, String[] argsList) throws  ParseError {
            this.processFileID(h, argsList[1]);
            this.processAddress(h, argsList[2]);
            this.processPort(h, argsList[3]);
            return 4;
        }
    },
    DELETE {
        @Override
        public int process(Header h, String[] argsList) throws ParseError {
            this.processFileID(h, argsList[1]);
            return 2;
        }
    };

    /**
     * @param messageType
     * @return enum element depending on the messageType
     * @throws ParseError
     */
    static MessageType parseMessageType(String messageType) throws ParseError {
        switch (messageType) {
            case "PUTFILE" -> {
                return MessageType.PUTFILE;
            }
            case "GETFILE" -> {
                return MessageType.GETFILE;
            }
            case "DELETE" -> {
                return MessageType.DELETE;
            }
            default -> throw new ParseError();
        }
    }

    /**
     * @param fileId
     * @param replicationDegree
     * @param address
     * @param port
     * @return string with a putchunk message
     */
    public static byte[] createPutFile(String fileId, String address, String port, int replicationDegree){
        return ("PUTFILE" + " " + fileId + " " + replicationDegree + " " + address + " " + port + " \r\n\r\n").getBytes();
    }

    /**
     * @param fileId
     * @param address
     * @param port
     * @return string with a getchunk message
     */
    public static byte[] createGetFile(String fileId, String address, String port) {
        return ("GETCHUNK" + " " + fileId + " " + address + " " + port + " \r\n\r\n").getBytes();
    }

    /**
     * @param fileId
     * @return string with a delete message
     */
    public static byte[] createDelete(String fileId) {
        return ("DELETE" + " " + fileId + " \r\n\r\n").getBytes();
    }

    public abstract int process(Header h, String[] argsList) throws ParseError;


    void processFileID(Header h, String fileID) throws ParseError {
        if (fileID.length() != 64) {
            throw new ParseError();
        }
        h.setFileID(fileID.toLowerCase());
    }

    void processReplicationDeg(Header h, String replicationDeg) throws ParseError {
        try {
            h.setReplicationDeg(Integer.parseInt(replicationDeg.trim()));
            if (h.getReplicationDeg() < 0) {
                throw new ParseError();
            }
        } catch (Exception e) {
            throw new ParseError();
        }
    }

    void processAddress(Header h, String address) throws ParseError {
        h.setAddress(address);
    }

    void processPort(Header h, String port) throws ParseError {
        try {
            h.setPort(Integer.valueOf(port));
        } catch (Exception e) {
            throw new ParseError();
        }
    }

}

