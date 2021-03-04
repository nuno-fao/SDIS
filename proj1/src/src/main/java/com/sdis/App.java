package com.sdis;

public class App {
    public static void main(String[] args) throws NewLineError {
        try {
                HeaderConcrete.getHeaders(" 1.0  PUTCHUNK  3 123aeb46ae7de0923432432123aeb46ae7dE0923432432Aefbc4579132321123 8 7 \r\n\r\n  ");
        } catch (IncorrectHeader | MessageTypeError | SenderIdError | FileIDError | ChunkNoError | ReplicationDegError incorrectHeader) {
            incorrectHeader.printStackTrace();
        }
    }
}
