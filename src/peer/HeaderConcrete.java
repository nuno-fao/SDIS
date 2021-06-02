package peer;

/**
 * Implements Header interface, used to Process the header messages
 */
public class HeaderConcrete implements Header {
    private String fileID, address, messageId;
    private Integer replicationDeg, port, sender, initiator;



    private MessageType messageType;

    private HeaderConcrete() {
    }

    @Override
    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    @Override
    public Integer getInitiator() {
        return this.initiator;
    }

    @Override
    public void setInitiator(Integer initiator) {
        this.initiator=initiator;
    }

    /**
     * Returns a subarray of the input array
     * @param argsList
     * @param startingIndex
     * @return
     */
    private static String[] getSubArray(String[] argsList, int startingIndex) {
        String[] cp = new String[argsList.length - startingIndex - 1];
        System.arraycopy(argsList, startingIndex + 1, cp, 0, argsList.length - startingIndex - 1);
        return cp;
    }

    /**
     * parses the headers
     * @param headerMessage received message
     * @return loist of headers on the received message
     */
    static Header getHeaders(String headerMessage) {
        String[] argsList = headerMessage.stripLeading().replaceAll(" +", " ").split(" ");
        Header localHeader = new HeaderConcrete();
        try {
            localHeader.setMessageType(MessageType.parseMessageType(argsList[0]));
            localHeader.getMessageType().process(localHeader, argsList);

        } catch (ParseError parseError) {
            parseError.printStackTrace();
        }
        return localHeader;
    }

    @Override
    public MessageType getMessageType() {
        return this.messageType;
    }

    @Override
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public void setMessageId(String messageId) {
        this.messageId=messageId;
    }

    @Override
    public String getMessageId() {
        return this.messageId;
    }


    @Override
    public String getFileID() {
        return this.fileID;
    }

    @Override
    public void setFileID(String fileID) {
        this.fileID = fileID;
    }


    @Override
    public Integer getReplicationDeg() {
        return this.replicationDeg;
    }

    @Override
    public void setReplicationDeg(Integer replicationDeg) {
        this.replicationDeg = replicationDeg;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }


}

