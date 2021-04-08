package sdis.server;

import sdis.Server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class Handler implements Runnable {
    private static AtomicInteger skipped = new AtomicInteger(0);
    static private long time = System.currentTimeMillis();
    private DatagramPacket packet;
    private int peerId;
    private ConcurrentHashMap<String, String> standbyBackupList;

    Handler(DatagramPacket packet, int peerId) {
        this.packet = packet;
        this.peerId = peerId;
        standbyBackupList = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
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
        if (body == null) {
            body = "".getBytes();
        }

        List<Header> headers = HeaderConcrete.getHeaders(head_body[0] + "\r\n\r\n");

        for (Header header : headers) {
            if (header.getSenderID() == this.peerId) {
                return;
            }
            switch (header.getMessageType()) {
                case PUTCHUNK -> {
                    time = System.currentTimeMillis();

                    if (Server.getServer().getMyFiles().containsKey(header.getFileID())) {
                        return;
                    }

                    standbyBackupList.remove(header.getFileID() + header.getChunkNo());

                    byte m[] = MessageType.createStored(header.getVersion(), this.peerId, header.getFileID(), header.getChunkNo());
                    DatagramPacket packet = new DatagramPacket(m, m.length, Server.getServer().getMc().getAddress(), Server.getServer().getMc().getPort());
                    if (Server.getServer().getMaxSize().get() == -1 || Server.getServer().getCurrentSize().get() + body.length <= Server.getServer().getMaxSize().get()) {
                        if (!Server.getServer().getStoredFiles().containsKey(header.getFileID())) {
                            try {
                                Files.createDirectories(Paths.get(Server.getServer().getServerName() + "/" + header.getFileID()));
                            } catch (IOException e) {
                                System.exit(1);
                            }
                            Server.getServer().getStoredFiles().put(header.getFileID(), new RemoteFile(header.getFileID()));
                        }
                        if (!Server.getServer().getStoredFiles().get(header.getFileID()).chunks.containsKey(header.getChunkNo())) {
                            Chunk c = new Chunk(header.getChunkNo(), header.getFileID(), header.getReplicationDeg(), body.length);
                            c.getPeerList().put(peerId, true);
                            c.update("rdata");
                            c.getPeerList().put((int) Server.getServer().getPeerId(), true);

                            Server.getServer().getStoredFiles().get(header.getFileID()).chunks.put(header.getChunkNo(), c);

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

                            fileChannel.write(buffer, 0);
                            buffer.clear();
                            Server.getServer().getCurrentSize().addAndGet(body.length);
                        }

                    }
                    if (Server.getServer().getStoredFiles().containsKey(header.getFileID()) && Server.getServer().getStoredFiles().get(header.getFileID()).getChunks().containsKey(header.getChunkNo())) {
                        Server.getServer().getPool().schedule(() -> Server.getServer().getMc().send(packet), new Random().nextInt(401), TimeUnit.MILLISECONDS);
                    }

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
                    //todo verificar se o initiator tem que ser 1.1 para funcionar com peers 1.1
                    if (Server.getServer().getStoredFiles().containsKey(header.getFileID())) {
                        Path name = Path.of(Server.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo());
                        if (Files.exists(name)) {
                            try {
                                switch (Server.getServer().getVersion()) {
                                    case "1.0" -> {
                                        byte[] file_content;
                                        file_content = Files.readAllBytes(name);
                                        byte[] message = MessageType.createChunk("1.0", (int) Server.getServer().getPeerId(), header.getFileID(), header.getChunkNo(), file_content);
                                        DatagramPacket packet = new DatagramPacket(message, message.length, Server.getServer().getMdr().getAddress(), Server.getServer().getMdr().getPort());
                                        Server.getServer().getPool().schedule(() -> {
                                            if (!(Server.getServer().getChunkQueue().containsKey(header.getFileID() + header.getChunkNo()))) {
                                                Server.getServer().getMdr().send(packet);
                                            }
                                            Server.getServer().getChunkQueue().remove(header.getFileID() + header.getChunkNo());
                                        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                                    }
                                    case "1.1" -> {
                                        Server.getServer().getPool().schedule(() -> {
                                            if (!(Server.getServer().getChunkQueue().containsKey(header.getFileID() + header.getChunkNo()))) {
                                                try {
                                                    ServerSocket s = new ServerSocket(0);
                                                    byte[] message = MessageType.createChunk_1_1("1.1", (int) Server.getServer().getPeerId(), header.getFileID(), header.getChunkNo(), InetAddress.getLocalHost().getHostAddress(), s.getLocalPort());
                                                    DatagramPacket packet = new DatagramPacket(message, message.length, Server.getServer().getMdr().getAddress(), Server.getServer().getMdr().getPort());
                                                    byte[] file_content;
                                                    file_content = Files.readAllBytes(name);
                                                    Socket n_s;
                                                    Server.getServer().getMdr().send(packet);
                                                    n_s = s.accept();
                                                    n_s.getOutputStream().write(file_content);
                                                    n_s.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            Server.getServer().getChunkQueue().remove(header.getFileID() + header.getChunkNo());
                                        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                                    }
                                }
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
                    System.out.println("REMOVED CHUNK " + header.getChunkNo());
                    Chunk chunk = null;
                    boolean isLocalCopy = false;
                    if (Server.getServer().getMyFiles().containsKey(header.getFileID())) {
                        System.out.println("My Files");
                        chunk = Server.getServer().getMyFiles().get(header.getFileID()).getChunks().get(header.getChunkNo());

                    } else if (Server.getServer().getStoredFiles().containsKey(header.getFileID())) {
                        System.out.println("Remote Files");
                        isLocalCopy = true;
                        chunk = Server.getServer().getStoredFiles().get(header.getFileID()).getChunks().get(header.getChunkNo());

                    }
                    if (chunk != null) {
                        if (!isLocalCopy) {
                            if (chunk.getPeerList().containsKey(header.getSenderID())) {
                                chunk.getPeerList().remove(header.getSenderID());
                                chunk.update("ldata");
                                return;
                            }
                        } else if (chunk.getPeerList().containsKey(header.getSenderID())) {
                            chunk.getPeerList().remove(header.getSenderID());
                            chunk.update("rdata");
                        }
                        if (chunk.getRealDegree() < chunk.getRepDegree()) {
                            Path name = Path.of(Server.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo());
                            if (Files.exists(name)) {
                                standbyBackupList.put(header.getFileID() + header.getChunkNo(), "yo");

                                Chunk finalChunk = chunk;
                                Server.getServer().getPool().schedule(() -> {
                                    try {
                                        if (standbyBackupList.containsKey(header.getFileID() + header.getChunkNo())) {
                                            byte[] file_content;
                                            file_content = Files.readAllBytes(name);

                                            byte[] message1 = MessageType.createPutchunk("1.0", (int) Server.getServer().getPeerId(), header.getFileID(), (int) header.getChunkNo(), finalChunk.getRepDegree(), file_content);
                                            byte[] message2 = MessageType.createStored("1.0", (int) Server.getServer().getPeerId(), header.getFileID(), (int) header.getChunkNo());
                                            DatagramPacket packet1 = new DatagramPacket(message1, message1.length, Server.getServer().getMdb().getAddress(), Server.getServer().getMdb().getPort());
                                            DatagramPacket packet2 = new DatagramPacket(message2, message2.length, Server.getServer().getMc().getAddress(), Server.getServer().getMc().getPort());
                                            System.out.println("SENDING CHUNK NO " + finalChunk.getChunkNo());
                                            removeAux(1, Server.getServer().getPool(), packet1, packet2, header.getFileID(), header.getChunkNo(), finalChunk.getRepDegree());
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                }
                case CHUNK -> {
                    Server.getServer().getChunkQueue().put(header.getFileID() + header.getChunkNo(), true);
                    if (!(Server.getServer().getFileRestoring() == null)) {
                        switch (header.getVersion()) {
                            case "1.0" -> {
                                processChunk(header, body.length, body);
                            }
                            case "1.1" -> {
                                if (Server.getServer().getFileRestoring().containsKey(header.getFileID())) {
                                    int read;
                                    byte alloc[];
                                    try {
                                        byte[] tmp_buffer = new byte[64000];
                                        Socket s = new Socket(header.getAddress(), header.getPort());
                                        DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                                        read = in.read(tmp_buffer);
                                        System.out.println("MY read: " + header.getChunkNo() + " " + header.getPort() + " " + read);
                                        alloc = new byte[read];
                                        System.arraycopy(tmp_buffer, 0, alloc, 0, read);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        break;
                                    }
                                    processChunk(header, read, alloc);
                                }
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }
    }

    private void processChunk(Header header, int buffer_length, byte[] buffer) {
        if (!Server.getServer().getFileRestoring().get(header.getFileID()).getChunks().containsKey(header.getChunkNo())) {
            Server.getServer().getFileRestoring().get(header.getFileID()).getChunks().put(header.getChunkNo(), buffer);
        }

        if (Server.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks() == null && buffer_length < Server.getServer().getChunkSize()) {
            Server.getServer().getFileRestoring().get(header.getFileID()).setNumberOfChunks(header.getChunkNo() + 1);
        }

        if (Server.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks() != null && Server.getServer().getFileRestoring().get(header.getFileID()).getChunks().values().size() == Server.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks()) {
            RestoreFile f = Server.getServer().getFileRestoring().get(header.getFileID());
            Server.getServer().getFileRestoring().remove(header.getFileID());
            String folder = Server.getServer().getServerName() + "/" + "restored";
            if (!Files.exists(Path.of(folder))) {
                try {
                    Files.createDirectories(Path.of(folder));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Path path = Paths.get(folder + "/" + Server.getServer().getMyFiles().get(header.getFileID()).getName());
            AsynchronousFileChannel fileChannel = null;
            try {
                fileChannel = AsynchronousFileChannel.open(
                        path, WRITE, CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int iterator = 0; iterator < f.getChunks().size(); iterator++) {
                ByteBuffer l_buffer = ByteBuffer.allocate(f.getChunks().get(iterator).length);

                l_buffer.put(f.getChunks().get(iterator));
                l_buffer.flip();

                fileChannel.write(l_buffer, iterator * Server.getServer().getChunkSize());
                l_buffer.clear();
            }
        }
    }

    private void removeAux(int i, ScheduledExecutorService pool, DatagramPacket packet1, DatagramPacket packet2, String fileId, int chunkNo, int repDegree) {
        Server.getServer().getMdb().send(packet1);

        Server.getServer().getPool().schedule(() -> Server.getServer().getMc().send(packet2), new Random().nextInt(401), TimeUnit.MILLISECONDS);

        System.out.println("TRYING again " + chunkNo);
        pool.schedule(() -> {
            if (Server.getServer().getMyFiles().get(fileId).getReplicationDegree(chunkNo) < repDegree) {
                if (i < 16) {
                    //System.out.println("Against: " + i + " " + this.agains.getAndIncrement() + " " + chunkNo);
                    this.removeAux(i * 2, pool, packet1, packet2, fileId, chunkNo, repDegree);
                } else {
                    System.out.println("Gave up on removed backup subprotocol");
                }
            }
        }, i * 1000L + new Random().nextInt(401), TimeUnit.MILLISECONDS);
    }
}
