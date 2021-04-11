package peer;

import java.util.ArrayList;
import java.util.List;

public class HeaderConcrete implements Header {
    private String version, fileID, address;
    private Integer senderID, chunkNo, replicationDeg, port;

    private MessageType messageType;

    private HeaderConcrete() {
    }

    private static String[] getSubArray(String[] argsList, int startingIndex) {
        String[] cp = new String[argsList.length - startingIndex - 1];
        System.arraycopy(argsList, startingIndex + 1, cp, 0, argsList.length - startingIndex - 1);
        return cp;
    }

    static List<Header> getHeaders(String headerMessage) {
        String[] argsList = headerMessage.stripLeading().replaceAll(" +", " ").split(" ");
        List<Header> outList = new ArrayList<>();
        Header localHeader;
        int i = 0;
        while (true) {
            try {

                if (argsList.length == 0) {
                    throw new ParseError();
                }
                localHeader = new HeaderConcrete();
                if (argsList.length < 2) {
                    throw new ParseError();
                }
                localHeader.setVersion(argsList[0]);
                localHeader.setMessageType(MessageType.parseMessageType(argsList[1]));
                int lIndex = localHeader.getMessageType().process(localHeader, argsList);

                //se chegou ao fim
                if (argsList[lIndex].matches(" *(\r\n){1,2}[\\s\\S]*")) {
                    if (argsList[lIndex].matches(" *\r\n\r\n[\\s\\S]*")) {
                        outList.add(localHeader);
                        return outList;
                    }
                } else {
                    throw new ParseError();
                }
                //limpa os elementos já processados para ler o proximo header
                argsList = getSubArray(argsList, lIndex);
                outList.add(localHeader);
            } catch (Exception | ParseError e) {
                return new ArrayList<>();
            }
            i++;
            if(i>200)
                return outList;
        }
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
    public String getVersion() {
        return this.version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
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
    public Integer getSenderID() {
        return this.senderID;
    }

    @Override
    public void setSenderID(Integer senderID) {
        this.senderID = senderID;
    }

    @Override
    public Integer getChunkNo() {
        return this.chunkNo;
    }

    @Override
    public void setChunkNo(Integer chunkNo) {
        this.chunkNo = chunkNo;
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

