package peer;

public interface Header {
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

    Integer getPort();

    void setPort(Integer port);

    String getAddress();

    void setAddress(String address);

}
