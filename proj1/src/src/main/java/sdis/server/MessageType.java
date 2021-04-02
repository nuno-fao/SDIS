package sdis.server;

public enum MessageType {
    PUTCHUNK{
        @Override
        public int process(Message h, String []argsList) throws SenderIdError, FileIDError, ChunkNoError, ReplicationDegError {
            processSenderID(h,argsList[2]);
            processFileID(h,argsList[3]);
            processChunkNo(h,argsList[4]);
            processReplicationDeg(h,argsList[5]);
            return 6;
        }
    },
    STORED{
        @Override
        public int process(Message h, String []argsList) throws SenderIdError, FileIDError, ChunkNoError {
            processSenderID(h,argsList[2]);
            processFileID(h,argsList[3]);
            processChunkNo(h,argsList[4]);
            return 5;
        }
    },
    GETCHUNK{
        @Override
        public int process(Message h, String []argsList) throws SenderIdError, FileIDError, ChunkNoError {
            processSenderID(h,argsList[2]);
            processFileID(h,argsList[3]);
            processChunkNo(h,argsList[4]);
            return 5;
        }
    },
    DELETE{
        @Override
        public int process(Message h, String []argsList) throws SenderIdError, FileIDError {
            processSenderID(h,argsList[2]);
            processFileID(h,argsList[3]);
            return 4;
        }
    },
    REMOVED{
        @Override
        public int process(Message h, String []argsList) throws SenderIdError, FileIDError, ChunkNoError {
            processSenderID(h,argsList[2]);
            processFileID(h,argsList[3]);
            processChunkNo(h,argsList[4]);
            return 5;
        }
    },
    CHUNK{
        @Override
        public int process(Message h, String []argsList) throws SenderIdError, FileIDError, ChunkNoError {
            processSenderID(h,argsList[2]);
            processFileID(h,argsList[3]);
            processChunkNo(h,argsList[4]);
            return 5;
        }
    };

    public abstract int process(Message h, String []argsList) throws SenderIdError, FileIDError, ChunkNoError, ReplicationDegError;
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
    void processSenderID(Message h, String senderID) throws SenderIdError {
        try {
            h.setSenderID(Integer.parseInt(senderID));
            if(h.getSenderID()<0){
                throw new SenderIdError();
            }
        } catch (Exception e) {
            throw new SenderIdError();
        }
    }

    void processFileID(Message h, String fileID) throws FileIDError {
        if(fileID.length() != 64)
            throw new FileIDError();
        h.setFileID(fileID.toLowerCase());
    }
    void processChunkNo(Message h, String chunkNo) throws ChunkNoError {
        try {
            if(chunkNo.length()>6)
                throw new ChunkNoError();
            h.setChunkNo(Integer.parseInt(chunkNo));

            if(h.getChunkNo()<0){
                throw new ChunkNoError();
            }
        } catch (Exception e) {
            throw new ChunkNoError();
        }
    }
    void processReplicationDeg(Message h, String replicationDeg) throws ReplicationDegError {
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

    public static String createPutchunk(String version, int senderId, String fileId, int chunkNo, int replicationDegree, String body){
        return version+" PUTCHUNK "+senderId+" "+fileId+" "+chunkNo+" "+replicationDegree+" \r\n\r\n"+body;
    }
    public static String createStored(String version, int senderId, String fileId, int chunkNo){
        return version+" STORED "+senderId+" "+fileId+" "+chunkNo+" \r\n\r\n";
    }
}
