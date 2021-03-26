package sdis.server;

import java.util.ArrayList;
import java.util.List;

public class MessageConcrete implements Message {
    String version,fileID;
    Integer senderID,chunkNo,replicationDeg;
    MessageType messageType;
    String body = "";

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    private static String[] getSubArray(String[] argsList, int startingIndex){
        String[] cp = new String[argsList.length - startingIndex - 1];
        System.arraycopy(argsList, startingIndex + 1, cp, 0, argsList.length - startingIndex - 1);
        return cp;
    }

    public static List<Message> getHeaders(String headerMessage) throws IncorrectHeader, MessageTypeError, SenderIdError, FileIDError, ChunkNoError, ReplicationDegError, NewLineError {
        String[] argsList = headerMessage.stripLeading().replaceAll(" +", " ").split(" ");
        List<Message> outList = new ArrayList<>();
        Message localMessage;

        while (true) {
            try {

                if (argsList.length == 0) {
                    throw new NewLineError();
                }
                localMessage = new MessageConcrete();
                if (argsList.length < 2) {
                    throw new IncorrectHeader();
                }
                localMessage.setVersion(argsList[0]);
                localMessage.setMessageType(MessageType.parseMessageType(argsList[1]));
                int lIndex = localMessage.getMessageType().process(localMessage, argsList);

                //se chegou ao fim
                if (argsList[lIndex].matches(" *(\r\n){1,2}.*")) {
                    if (argsList[lIndex].matches(" *\r\n\r\n.*")) {
                        outList.add(localMessage);

                        if(argsList.length > lIndex){
                            for (Message m:outList) {
                                if(m.getMessageType() == MessageType.PUTCHUNK || m.getMessageType() == MessageType.CHUNK) {
                                    String[] sList = headerMessage.split("\r\n");
                                    String s = "";
                                    for (int i = 2; i < sList.length - 1; i++) {
                                        s += sList[i] + "\r\n";
                                    }
                                    s += sList[sList.length - 1];
                                    m.setBody(s);
                                }
                            }
                        }
                        return outList;
                    }
                }
                else {
                    throw new NewLineError();
                }
                //limpa os elementos já processados para ler o proximo header
                argsList = getSubArray(argsList,lIndex);
                outList.add(localMessage);
            }
            catch (ArrayIndexOutOfBoundsException e){
                throw new IncorrectHeader();
            }
        }
    }

    MessageConcrete() {}


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

class HeaderError extends Throwable{

}

class MessageTypeError extends HeaderError {

}

class SenderIdError extends HeaderError{

}

class FileIDError extends HeaderError{

}

class ChunkNoError extends HeaderError{

}

class ReplicationDegError extends HeaderError{

}

class IncorrectHeader extends HeaderError{

}

class NewLineError extends HeaderError{

}

