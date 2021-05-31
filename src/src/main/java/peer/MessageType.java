package peer;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Enum with a message type for every kind of message, each enum element knows how to process itself
 */
public enum MessageType {
    PUTFILE {
        @Override
        public int process(Header h, String[] argsList) throws ParseError {
            this.processSender(h,argsList[1]);
            this.processFileID(h, argsList[2]);
            this.processReplicationDeg(h, argsList[3]);
            this.processAddress(h, argsList[4]);
            this.processPort(h, argsList[5]);
            this.processMessageId(h, argsList[6]);
            return 7;
        }
    },
    GETFILE {
        @Override
        public int process(Header h, String[] argsList) throws  ParseError {
            this.processSender(h,argsList[1]);
            this.processFileID(h, argsList[2]);
            this.processAddress(h, argsList[3]);
            this.processPort(h, argsList[4]);
            this.processMessageId(h, argsList[5]);
            return 6;
        }
    },
    DELETE {
        @Override
        public int process(Header h, String[] argsList) throws ParseError {
            this.processSender(h,argsList[1]);
            this.processFileID(h, argsList[2]);
            this.processReplicationDeg(h, argsList[3]);
            this.processMessageId(h, argsList[4]);
            return 5;
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
    public static byte[] createPutFile(int senderID,String fileId, String address, String port, int replicationDegree, String messageId){
        return ("PUTFILE" + " " + senderID + " " + fileId + " " + replicationDegree + " " + address + " " + port + " " + messageId + " \r\n\r\n").getBytes();
    }

    /**
     * @param fileId
     * @param address
     * @param port
     * @return string with a getchunk message
     */
    public static byte[] createGetFile(int senderID,String fileId, String address, String port, String messageId) {
        return ("GETFILE" + " " + senderID + " " + fileId + " " + address + " " + port + " " + messageId + " \r\n\r\n").getBytes();
    }

    /**
     * @param fileId
     * @return string with a delete message
     */
    public static byte[] createDelete(int senderID,String fileId, int replicationDegree, String messageId) {
        return ("DELETE" + " " + senderID + " " + fileId + " "+ replicationDegree +  " "+ messageId + " \r\n\r\n").getBytes();
    }

    public abstract int process(Header h, String[] argsList) throws ParseError;


    void processFileID(Header h, String fileID) throws ParseError {
       /* if (fileID.length() != 64) {
            throw new ParseError();
        }*/
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

    void processSender(Header h, String sender) throws ParseError {
        try{
            int s = Integer.parseInt(sender);
            h.setSender(s);
        }
        catch (Exception e){
            throw new ParseError();
        }
    }

    void processPort(Header h, String port) throws ParseError {
        try {
            h.setPort(Integer.valueOf(port));
        } catch (Exception e) {
            throw new ParseError();
        }
    }
    void processMessageId(Header h, String messageId) throws ParseError {
        try {
            h.setMessageId(messageId);
        } catch (Exception e) {
            throw new ParseError();
        }
    }

    public static String generateMessageId(){
        MessageDigest algo = null;
        try {
            algo = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new String(algo.digest(String.valueOf(System.nanoTime()).getBytes(StandardCharsets.UTF_8)));
    }

}

