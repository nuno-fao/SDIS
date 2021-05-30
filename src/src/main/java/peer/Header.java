package peer;

public interface Header {

    MessageType getMessageType();

    void setMessageType(MessageType messageType);

    void setFirstPeer(int firstPeer);

    int getFirstPeer();

    String getFileID();

    void setFileID(String fileID);

    Integer getReplicationDeg();

    void setReplicationDeg(Integer replicationDeg);

    Integer getPort();

    void setPort(Integer port);

    String getAddress();

    void setAddress(String address);


    int getSender();

    void setSender(int sender);

}
