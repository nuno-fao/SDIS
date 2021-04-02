package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Handler implements Runnable {
    private DatagramPacket packet;
    private int peerId;

    Handler(DatagramPacket packet, int peerId) {
        this.packet = packet;
        this.peerId = peerId;
    }

    @Override
    public void run() {
        try {
            List<Message> messages = MessageConcrete.getHeaders(new String(this.packet.getData()).substring(0, this.packet.getLength()));
            for (Message message : messages) {
                if (message.getSenderID() == this.peerId)
                    return;
                switch (message.getMessageType()) {
                    case PUTCHUNK -> {
                        String m = MessageType.createStored(message.getVersion(), this.peerId, message.getFileID(), message.getChunkNo());
                        DatagramPacket packet = new DatagramPacket(m.getBytes(), m.length(), Server.getServer().getMc().getAddress(), Server.getServer().getMc().getPort());
                        if (!Server.getServer().getStoredFiles().containsKey(message.getFileID())) {
                            Server.getServer().getStoredFiles().put(message.getFileID(), new RemoteFile(message.getFileID()));
                            try {
                                Files.createDirectories(Paths.get(Server.getServer().getServerName() + "/" + message.getFileID()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!Server.getServer().getStoredFiles().get(message.getFileID()).chunks.containsKey(message.getChunkNo())) {
                            Server.getServer().getStoredFiles().get(message.getFileID()).chunks.put(message.getChunkNo(), new Chunk(message.getChunkNo(), message.getFileID(), message.getReplicationDeg()));
                            Server.getServer().getStoredFiles().get(message.getFileID()).addStored(message.getChunkNo(), message.getSenderID());
                            try {
                                Files.write(Paths.get(Server.getServer().getServerName() + "/" + message.getFileID() + "/" + message.getChunkNo()), message.getBody().getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Server.getServer().getMc().send(packet);
                        break;
                    }
                    case STORED -> {
                        Server.getServer().getPool().schedule(() ->
                        {
                            if (Server.getServer().getMyFiles().containsKey(message.getFileID())) {
                                Server.getServer().getMyFiles().get(message.getFileID()).addStored(message.getChunkNo(), message.getSenderID());
                            } else if (Server.getServer().getStoredFiles().containsKey(message.getFileID()) && Server.getServer().getStoredFiles().get(message.getFileID()).chunks.containsKey(message.getChunkNo())) {
                                Server.getServer().getStoredFiles().get(message.getFileID()).addStored(message.getChunkNo(), message.getSenderID());
                            } else
                                System.out.println("Skipped");
                        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                        break;

                    }
                    case GETCHUNK -> {
                        break;
                    }
                    case DELETE -> {
                        if (Server.getServer().getStoredFiles().containsKey(message.getFileID())) {
                            RemoteFile f = Server.getServer().getStoredFiles().get(message.getFileID());
                            Server.getServer().getStoredFiles().remove(message.getFileID());
                            f.delete();
                        }
                        break;
                    }
                    case REMOVED -> {
                        System.out.println("REMOVED");
                        Chunk chunk = null;
                        if (Server.getServer().getMyFiles().containsKey(message.getFileID())) {
                            System.out.println("My Files");
                            chunk = Server.getServer().getMyFiles().get(message.getFileID()).getChunks().get(message.getChunkNo());

                        } else if (Server.getServer().getStoredFiles().containsKey(message.getFileID())) {
                            System.out.println("Remote Files");
                            chunk = Server.getServer().getStoredFiles().get(message.getFileID()).getChunks().get(message.getChunkNo());

                        }
                        if (chunk != null) {
                            if (chunk.getPeerList() != null) {
                                if (chunk.getPeerList().containsKey(message.getSenderID())) {
                                    chunk.getPeerList().remove(message.getSenderID());
                                    this.chunkUpdate(chunk);
                                }
                            } else {
                                chunk.subtractRealDegree();
                                this.chunkUpdate(chunk);
                            }
                        }
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

    private void chunkUpdate(Chunk chunk) {
        chunk.update();
        if (chunk.getRepDegree() > chunk.getPeerCount()) {
            chunk.getShallSend().set(true);
            Server.getServer().getPool().schedule(() -> {
                if (chunk.getShallSend().get())
                    chunk.backup(Server.getServer().getPool());
            }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
        }
    }
}
