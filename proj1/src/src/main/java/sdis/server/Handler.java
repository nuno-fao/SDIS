package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Handler implements Runnable{
    private DatagramPacket packet;
    private int peerId;
    Handler(DatagramPacket packet, int peerId){
        this.packet = packet;
        this.peerId = peerId;
    }
    @Override
    public void run() {
        try {
            List<Message> messages = MessageConcrete.getHeaders(new String(packet.getData()).substring(0,packet.getLength()));
            for (Message message : messages) {
                if(message.getSenderID() == peerId)
                    return;
                switch (message.getMessageType()){
                    case PUTCHUNK -> {
                        String m = MessageType.createStored(message.getVersion(),peerId,message.getFileID(),message.getChunkNo());
                        DatagramPacket packet = new DatagramPacket(m.getBytes(), m.length());
                        packet.setAddress(Server.getServer().getMc().getAddress());
                        packet.setPort(Server.getServer().getMc().getPort());
                        if(!Server.getServer().getStoredFiles().containsKey(message.getFileID())){
                            Server.getServer().getStoredFiles().put(message.getFileID(),new RemoteFile(message.getFileID()));
                            try {
                                Files.createDirectories(Paths.get(Server.getServer().getServerName()+"/"+message.getFileID()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if(!Server.getServer().getStoredFiles().get(message.getFileID()).chunks.containsKey(message.getChunkNo())){
                            Server.getServer().getStoredFiles().get(message.getFileID()).chunks.put(message.getChunkNo(),new Chunk(message.getChunkNo(),message.getFileID(),message.getReplicationDeg()));
                            Server.getServer().getStoredFiles().get(message.getFileID()).addStored(message.getChunkNo(),message.getSenderID());
                            try {
                                Files.write(Paths.get(Server.getServer().getServerName()+"/"+message.getFileID()+"/"+message.getChunkNo()),message.getBody().getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Server.getServer().getMc().send(packet);
                        break;
                    }
                    case STORED -> {
                        Server.getServer().getPool().schedule(() -> {
                            if(Server.getServer().getMyFiles().containsKey(message.getFileID())){
                            Server.getServer().getMyFiles().get(message.getFileID()).addStored(message.getChunkNo(),message.getSenderID());
                        }
                        else if(Server.getServer().getStoredFiles().containsKey(message.getFileID()) && Server.getServer().getStoredFiles().get(message.getFileID()).chunks.containsKey(message.getChunkNo())){
                            Server.getServer().getStoredFiles().get(message.getFileID()).addStored(message.getChunkNo(),message.getSenderID());
                        }
                        else
                                System.out.println("Skipped");
                        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
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
            headerError.printStackTrace();
        }
    }
}
