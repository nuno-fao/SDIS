package sdis.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public enum MessageType {
    PUTCHUNK {
        @Override
        public int process(Header h, String[] argsList) throws SenderIdError, FileIDError, ChunkNoError, ReplicationDegError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            this.processReplicationDeg(h, argsList[5]);
            return 6;
        }
    },
    STORED {
        @Override
        public int process(Header h, String[] argsList) throws SenderIdError, FileIDError, ChunkNoError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    },
    GETCHUNK {
        @Override
        public int process(Header h, String[] argsList) throws SenderIdError, FileIDError, ChunkNoError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    },
    DELETE {
        @Override
        public int process(Header h, String[] argsList) throws SenderIdError, FileIDError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            return 4;
        }
    },
    REMOVED {
        @Override
        public int process(Header h, String[] argsList) throws SenderIdError, FileIDError, ChunkNoError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    },
    CHUNK {
        @Override
        public int process(Header h, String[] argsList) throws SenderIdError, FileIDError, ChunkNoError {
            this.processSenderID(h, argsList[2]);
            this.processFileID(h, argsList[3]);
            this.processChunkNo(h, argsList[4]);
            return 5;
        }
    };

    static MessageType parseMessageType(String messageType) throws MessageTypeError {
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
            default -> throw new MessageTypeError();
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

    public abstract int process(Header h, String[] argsList) throws SenderIdError, FileIDError, ChunkNoError, ReplicationDegError;

    void processSenderID(Header h, String senderID) throws SenderIdError {
        try {
            h.setSenderID(Integer.parseInt(senderID));
            if (h.getSenderID() < 0) {
                throw new SenderIdError();
            }
        } catch (Exception e) {
            throw new SenderIdError();
        }
    }

    void processFileID(Header h, String fileID) throws FileIDError {
        if (fileID.length() != 64)
            throw new FileIDError();
        h.setFileID(fileID.toLowerCase());
    }

    void processChunkNo(Header h, String chunkNo) throws ChunkNoError {
        try {
            if (chunkNo.length() > 6)
                throw new ChunkNoError();
            h.setChunkNo(Integer.parseInt(chunkNo));

            if (h.getChunkNo() < 0) {
                throw new ChunkNoError();
            }
        } catch (Exception e) {
            throw new ChunkNoError();
        }
    }

    void processReplicationDeg(Header h, String replicationDeg) throws ReplicationDegError {
        try {
            if (replicationDeg.length() != 1)
                throw new ReplicationDegError();
            h.setReplicationDeg(Integer.parseInt(replicationDeg));
            if (h.getReplicationDeg() < 0) {
                throw new ReplicationDegError();
            }
        } catch (Exception e) {
            throw new ReplicationDegError();
        }
    }
}
