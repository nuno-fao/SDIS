package sdis.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public enum MessageType {
    PUTCHUNK {
        @Override
        public int process(Header h, String[] argsList) throws ParseError, ParseError, ParseError, ParseError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            this.processReplicationDeg(h, argsList[5]);
            return 6;
        }
    },
    STORED {
        @Override
        public int process(Header h, String[] argsList) throws ParseError, ParseError, ParseError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    },
    GETCHUNK {
        @Override
        public int process(Header h, String[] argsList) throws ParseError, ParseError, ParseError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    },
    DELETE {
        @Override
        public int process(Header h, String[] argsList) throws ParseError, ParseError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            return 4;
        }
    },
    REMOVED {
        @Override
        public int process(Header h, String[] argsList) throws ParseError, ParseError, ParseError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    },
    CHUNK {
        @Override
        public int process(Header h, String[] argsList) throws ParseError, ParseError, ParseError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    };

    static MessageType parseMessageType(String messageType) throws ParseError {
        switch (messageType) {
            case "PUTCHUNK" -> {
                return MessageType.PUTCHUNK;
            }
            case "STORED" -> {
                return MessageType.STORED;
            }
            case "GETCHUNK" -> {
                return MessageType.GETCHUNK;
            }
            case "CHUNK" -> {
                return MessageType.CHUNK;
            }
            case "REMOVED" -> {
                return MessageType.REMOVED;
            }
            case "DELETE" -> {
                return MessageType.DELETE;
            }
            default -> throw new ParseError();
        }
    }

    public static byte[] createPutchunk(String version, int senderId, String fileId, int chunkNo, int replicationDegree, byte[] body) {
        byte a[] = (version + " PUTCHUNK " + senderId + " " + fileId + " " + chunkNo + " " + replicationDegree + " \r\n\r\n").getBytes();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(body.length + a.length);
        try {
            outputStream.write(a);
            outputStream.write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public static byte[] createStored(String version, int senderId, String fileId, int chunkNo) {
        return (version + " STORED " + senderId + " " + fileId + " " + chunkNo + " \r\n\r\n").getBytes();
    }

    public static byte[] createDelete(String version, int senderId, String fileId) {
        return (version + " DELETE " + senderId + " " + fileId + " \r\n\r\n").getBytes();
    }

    public static byte[] createRemoved(String version, int senderId, String fileId, int chunkNo) {
        return (version + " REMOVED " + senderId + " " + fileId + " " + chunkNo + " \r\n\r\n").getBytes();
    }

    public abstract int process(Header h, String[] argsList) throws ParseError, ParseError, ParseError, ParseError;

    void processSenderID(Header h, String senderID) throws ParseError {
        try {
            h.setSenderID(Integer.parseInt(senderID));
            if (h.getSenderID() < 0) {
                throw new ParseError();
            }
        } catch (Exception e) {
            throw new ParseError();
        }
    }

    void processFileID(Header h, String fileID) throws ParseError {
        if (fileID.length() != 64)
            throw new ParseError();
        h.setFileID(fileID.toLowerCase());
    }

    void processChunkNo(Header h, String chunkNo) throws ParseError {
        try {
            if (chunkNo.length() > 6)
                throw new ParseError();
            h.setChunkNo(Integer.parseInt(chunkNo));

            if (h.getChunkNo() < 0) {
                throw new ParseError();
            }
        } catch (Exception e) {
            throw new ParseError();
        }
    }

    void processReplicationDeg(Header h, String replicationDeg) throws ParseError {
        try {
            if (replicationDeg.length() != 1)
                throw new ParseError();
            h.setReplicationDeg(Integer.parseInt(replicationDeg));
            if (h.getReplicationDeg() < 0) {
                throw new ParseError();
            }
        } catch (Exception e) {
            throw new ParseError();
        }
    }
}

