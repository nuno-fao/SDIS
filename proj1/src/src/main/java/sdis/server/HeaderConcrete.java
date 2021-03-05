package sdis.server;

import java.util.ArrayList;
import java.util.List;

public class HeaderConcrete implements Header {
    String version,fileID;
    Integer senderID,chunkNo,replicationDeg;
    MessageType messageType;

    private static String[] getSubArray(String[] argsList, int startingIndex){
        String[] cp = new String[argsList.length - startingIndex - 1];
        System.arraycopy(argsList, startingIndex + 1, cp, 0, argsList.length - startingIndex - 1);
        return cp;
    }

    public static List<Header> getHeaders(String headerMessage) throws IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, ChunkNoError, ReplicationDegError, NewLineError {
        String[] argsList = headerMessage.stripLeading().replaceAll(" +", " ").split(" ");
        List<Header> outList = new ArrayList<>();
        Header localHeader;

        while (true) {
            try {
                if (argsList.length == 0) {
                    throw new NewLineError();
                }
                localHeader = new HeaderConcrete();
                if (argsList.length < 2) {
                    throw new IncorrectHeader();
                }
                localHeader.setVersion(argsList[0]);
                localHeader.setMessageType(MessageType.parseMessageType(argsList[1]));
                int lIndex = localHeader.getMessageType().process(localHeader, argsList);

                //se chegou ao fim
                if (argsList[lIndex].matches(" *(\r\n){1,2} *")) {
                    if (argsList[lIndex].matches(" *\r\n\r\n *")) {
                        outList.add(localHeader);
                        return outList;
                    }
                }
                else {
                    throw new NewLineError();
                }
                //limpa os elementos já processados para ler o proximo header
                argsList = getSubArray(argsList,lIndex);
                outList.add(localHeader);
            }
            catch (ArrayIndexOutOfBoundsException e){
                throw new IncorrectHeader();
            }
        }
    }

    HeaderConcrete() {}

    @Override
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getFileID() {
        return fileID;
    }

    @Override
    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    @Override
    public Integer getSenderID() {
        return senderID;
    }

    @Override
    public void setSenderID(Integer senderID) {
        this.senderID = senderID;
    }

    @Override
    public Integer getChunkNo() {
        return chunkNo;
    }

    @Override
    public void setChunkNo(Integer chunkNo) {
        this.chunkNo = chunkNo;
    }

    @Override
    public Integer getReplicationDeg() {
        return replicationDeg;
    }

    @Override
    public void setReplicationDeg(Integer replicationDeg) {
        this.replicationDeg = replicationDeg;
    }
}

class MessageTypeError extends Throwable {

}

class SenderIdError extends Throwable{

}

class FileIDError extends Throwable{

}

class ChunkNoError extends Throwable{

}

class ReplicationDegError extends Throwable{

}

class IncorrectHeader extends Throwable{

}


class NewLineError extends Throwable{

}

