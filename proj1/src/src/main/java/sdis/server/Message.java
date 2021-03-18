package sdis.server;

public interface Message {
    MessageType getMessageType();

    void setMessageType(MessageType messageType);

    String getVersion();

    void setVersion(String version);

    String getFileID();

    void setFileID(String fileID);

    Integer getSenderID();

    void setSenderID(Integer senderID);

    Integer getChunkNo();

    void setChunkNo(Integer chunkNo);

    Integer getReplicationDeg();

    void setReplicationDeg(Integer replicationDeg);

    String getBody();
    void setBody(String body);
}
