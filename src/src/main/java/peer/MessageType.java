package peer;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Enum with a message type for every kind of message, each enum element knows how to process itself
 */
public enum MessageType {
    PUTFILE {
        @Override
        public int process(Header h, String[] argsList) throws ParseError {
            this.processSender(h, argsList[1]);
            this.proccessInitiator(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processReplicationDeg(h, argsList[4]);
            this.processAddress(h, argsList[5]);
            this.processPort(h, argsList[6]);
            this.processMessageId(h, argsList[7]);
            return 8;
        }
    },
    GETFILE {
        @Override
        public int process(Header h, String[] argsList) throws ParseError {
            this.processSender(h, argsList[1]);
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
            this.processSender(h, argsList[1]);
            this.processFileID(h, argsList[2]);
            this.processReplicationDeg(h, argsList[3]);
            this.processMessageId(h, argsList[4]);
            return 5;
        }
    },
    PUTERROR {
        @Override
        public int process(Header h, String[] argsList) throws ParseError {
            this.proccessInitiator(h, argsList[1]);
            this.processFileID(h, argsList[2]);
            this.processReplicationDeg(h, argsList[3]);
            return 4;
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
            case "PUTERROR" -> {
                return MessageType.PUTERROR;
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
    public static byte[] createPutFile(int senderID, int initiator, String fileId, String address, String port, int replicationDegree, String messageId) {
        return ("PUTFILE" + " " + senderID + " " + initiator + " " + fileId + " " + replicationDegree + " " + address + " " + port + " " + messageId + " \r\n\r\n").getBytes();
    }

    /**
     * @param fileId
     * @param address
     * @param port
     * @return string with a getchunk message
     */
    public static byte[] createGetFile(int senderID, String fileId, String address, String port, String messageId) {
        return ("GETFILE" + " " + senderID + " " + fileId + " " + address + " " + port + " " + messageId + " \r\n\r\n").getBytes();
    }

    /**
     * @param fileId
     * @return string with a delete message
     */
    public static byte[] createDelete(int senderID, String fileId, int replicationDegree, String messageId) {
        return ("DELETE" + " " + senderID + " " + fileId + " " + replicationDegree + " " + messageId + " \r\n\r\n").getBytes();
    }

    /**
     * @param
     * @return string with a delete message
     */
    public static byte[] createPutError(int initiator, String fileId, int replicationDegree) {
        return ("PUTERROR" + " " + initiator + " " + fileId + " " + replicationDegree + " \r\n\r\n").getBytes();
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
        try {
            int s = Integer.parseInt(sender);
            h.setSender(s);
        } catch (Exception e) {
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

    void proccessInitiator(Header h, String initiatorId) throws ParseError {
        try {
            h.setInitiator(Integer.parseInt(initiatorId));
        } catch (Exception e) {
            throw new ParseError();
        }
    }

    public static String generateMessageId() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(("" + System.nanoTime()).getBytes());
            BigInteger num = new BigInteger(1, digest);
            return num.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Invalid algorithm");
        }
        return "";
    }

}

