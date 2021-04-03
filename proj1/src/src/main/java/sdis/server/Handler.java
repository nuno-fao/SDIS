package sdis.server;

import sdis.Server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class Handler implements Runnable {
    private static AtomicInteger skipped = new AtomicInteger(0);
    static private long time = System.currentTimeMillis();
    private DatagramPacket packet;
    private int peerId;

    Handler(DatagramPacket packet, int peerId) {
        this.packet = packet;
        this.peerId = peerId;
    }

    private static String readContent(Path file) {
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    file, StandardOpenOption.READ);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        Future<Integer> operation = fileChannel.read(buffer, 0);

        // run other code as operation continues in background
        try {
            operation.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        String fileContent = new String(buffer.array()).trim();
        buffer.clear();
        return fileContent;
    }

    @Override
    public void run() {
        try {
            String[] head_body = new String(this.packet.getData()).stripLeading().split("\r\n\r\n", 2);
            byte body[] = null;
            byte tmp[] = this.packet.getData();
            int i = 0;
            for (; i < this.packet.getLength() - 3; i++) {
                if (tmp[i] == 0xd && tmp[i + 1] == 0xa && tmp[i + 2] == 0xd && tmp[i + 3] == 0xa) {
                    break;
                }
            }
            i += 4;
            if (head_body.length > 1) {
                if (this.packet.getLength() > i) {
                    body = Arrays.copyOfRange(this.packet.getData(), i, this.packet.getLength());
                }
            }
            List<Header> headers = HeaderConcrete.getHeaders(head_body[0] + "\r\n\r\n");

            for (Header header : headers) {
                if (header.getSenderID() == this.peerId)
                    return;
                switch (header.getMessageType()) {
                    case PUTCHUNK -> {
                        //System.out.println("Time: "+System.currentTimeMillis() - this.time);
                        byte m[] = MessageType.createStored(header.getVersion(), this.peerId, header.getFileID(), header.getChunkNo());
                        DatagramPacket packet = new DatagramPacket(m, m.length, Server.getServer().getMc().getAddress(), Server.getServer().getMc().getPort());
                        if (!Server.getServer().getStoredFiles().containsKey(header.getFileID())) {
                            try {
                                Files.createDirectories(Paths.get(Server.getServer().getServerName() + "/" + header.getFileID()));
                            } catch (IOException e) {
                                System.exit(1);
                            }
                            Server.getServer().getStoredFiles().put(header.getFileID(), new RemoteFile(header.getFileID()));
                        }
                        if (!Server.getServer().getStoredFiles().get(header.getFileID()).chunks.containsKey(header.getChunkNo())) {
                            Server.getServer().getStoredFiles().get(header.getFileID()).chunks.put(header.getChunkNo(), new Chunk(header.getChunkNo(), header.getFileID(), header.getReplicationDeg()));
                            Server.getServer().getStoredFiles().get(header.getFileID()).addStored(header.getChunkNo(), header.getSenderID());

                            Path path = Paths.get(Server.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo());
                            AsynchronousFileChannel fileChannel = null;
                            try {
                                fileChannel = AsynchronousFileChannel.open(
                                        path, WRITE, CREATE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            ByteBuffer buffer = ByteBuffer.allocate(body.length);

                            buffer.put(body);
                            buffer.flip();

                            Future<Integer> operation = fileChannel.write(buffer, 0);
                            buffer.clear();

                            /*//run other code as operation continues in background
                            try {
                                operation.get();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }*/
                            /*try {
                                Files.write(Paths.get(Server.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo()), body);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }*/
                        }
                        Server.getServer().getPool().schedule(() -> Server.getServer().getMc().send(packet), new Random().nextInt(401), TimeUnit.MILLISECONDS);
                    }
                    case STORED -> {
                        if (Server.getServer().getMyFiles().containsKey(header.getFileID())) {
                            Server.getServer().getMyFiles().get(header.getFileID()).addStored(header.getChunkNo(), header.getSenderID());
                        } else if (Server.getServer().getStoredFiles().containsKey(header.getFileID()) && Server.getServer().getStoredFiles().get(header.getFileID()).chunks.containsKey(header.getChunkNo())) {
                            Server.getServer().getStoredFiles().get(header.getFileID()).addStored(header.getChunkNo(), header.getSenderID());
                        } else {
                            //System.out.println("Skipped " + header.getFileID() + "/" + header.getChunkNo() + " : " + skipped.getAndIncrement());
                        }

                    }
                    case GETCHUNK -> {

                        if (Server.getServer().getStoredFiles().containsKey(header.getFileID())) {
                            //RemoteFile f = Server.getServer().getStoredFiles().get(header.getFileID());
                            //Chunk toSend = Server.getServer().getStoredFiles().get(header.getFileID()).getChunks().get(header.getChunkNo());
                            //toSend.getChunk(Server.getServer().getPool());

                            Path name = Path.of(Server.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo());
                            if (Files.exists(name)) {
                                try {
                                    byte[] file_content;
                                    file_content = Files.readAllBytes(name);
                                    byte[] message = MessageType.createChunk("1.0", (int) Server.getServer().getPeerId(), header.getFileID(), header.getChunkNo(), file_content);
                                    DatagramPacket packet = new DatagramPacket(message, message.length, Server.getServer().getMdr().getAddress(), Server.getServer().getMdr().getPort());
                                    Server.getServer().getPool().schedule(() -> Server.getServer().getMdr().send(packet), new Random().nextInt(401), TimeUnit.MILLISECONDS);
                                } catch (IOException ignored) {
                                }
                            }
                        }
                    }
                    case DELETE -> {
                        if (Server.getServer().getStoredFiles().containsKey(header.getFileID())) {
                            RemoteFile f = Server.getServer().getStoredFiles().get(header.getFileID());
                            Server.getServer().getStoredFiles().remove(header.getFileID());
                            f.delete();
                        }
                    }
                    case REMOVED -> {
                        System.out.println("REMOVED");
                        Chunk chunk = null;
                        if (Server.getServer().getMyFiles().containsKey(header.getFileID())) {
                            System.out.println("My Files");
                            chunk = Server.getServer().getMyFiles().get(header.getFileID()).getChunks().get(header.getChunkNo());

                        } else if (Server.getServer().getStoredFiles().containsKey(header.getFileID())) {
                            System.out.println("Remote Files");
                            chunk = Server.getServer().getStoredFiles().get(header.getFileID()).getChunks().get(header.getChunkNo());

                        }
                        if (chunk != null) {
                            if (chunk.getPeerList() != null) {
                                if (chunk.getPeerList().containsKey(header.getSenderID())) {
                                    chunk.getPeerList().remove(header.getSenderID());
                                    this.chunkUpdate(chunk);
                                }
                            } else {
                                chunk.subtractRealDegree();
                                this.chunkUpdate(chunk);
                            }
                        }
                    }
                    case CHUNK -> {
                        if(Server.getServer().getFileRestoring().containsKey(header.getFileID())){
                            if(!Server.getServer().getFileRestoring().get(header.getFileID()).getChunks().containsKey(header.getChunkNo())){
                                Server.getServer().getFileRestoring().get(header.getFileID()).getChunks().put(header.getChunkNo(),body);
                                System.out.println("Recebi 1");
                            }

                            if(Server.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks()==null && body.length<64000){
                                System.out.println("Ultimo chunk");
                                Server.getServer().getFileRestoring().get(header.getFileID()).setNumberOfChunks(header.getChunkNo()+1);
                            }

                            if(Server.getServer().getFileRestoring().get(header.getFileID()).getChunks().values().size() == Server.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks()){
                                Path path = Paths.get("omiguelcheiramal" + Server.getServer().getMyFiles().get(header.getFileID()).getName());
                                System.out.println("FINISHED RECEIVING");
                                AsynchronousFileChannel fileChannel = null;
                                try {
                                    fileChannel = AsynchronousFileChannel.open(
                                            path, WRITE, CREATE);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                int iterator=0;
                                for(byte[] bytes : Server.getServer().getFileRestoring().get(header.getFileID()).getChunks().values()){
                                    System.out.println("ESCREVENDO YO");
                                    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);

                                    buffer.put(bytes);
                                    buffer.flip();

                                    Future<Integer> operation = fileChannel.write(buffer, iterator* 64000L);
                                    buffer.clear();

                                    iterator++;
                                }
                            }
                        }
                    }
                }
            }
        } catch (ParseError parseError) {
            parseError.printStackTrace();
        }
    }

    private void chunkUpdate(Chunk chunk) {
        chunk.update("rdata");
        if (chunk.getRepDegree() > chunk.getPeerCount()) {
            chunk.getShallSend().set(true);
            Server.getServer().getPool().schedule(() -> {
                if (chunk.getShallSend().get())
                    chunk.backup(Server.getServer().getPool());
            }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
        }
    }
}
