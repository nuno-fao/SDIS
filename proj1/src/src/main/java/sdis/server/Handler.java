package sdis.server;

import java.net.DatagramPacket;
import java.util.List;

public class Handler implements Runnable{
    private DatagramPacket packet;
    Handler(DatagramPacket packet){
        this.packet = packet;
    }
    @Override
    public void run() {
        System.out.println(new String(packet.getData()) + packet.getAddress().toString());
        try {
            List<Message> messages = MessageConcrete.getHeaders(packet.getData().toString());
            for (Message message : messages) {
                switch (message.getMessageType()){

                    case PUTCHUNK -> {
                        
                        break;
                    }
                    case STORED -> {
                        break;
                    }
                    case GETCHUNK -> {
                        break;
                    }
                    case DELETE -> {
                        break;
                    }
                    case REMOVED -> {
                        break;
                    }
                    case CHUNK -> {
                        break;
                    }
                }
            }
        } catch (HeaderError headerError) {
            //headerError.printStackTrace();
        }
    }
}
