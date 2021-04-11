package peer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
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

        List<Header> headers = HeaderConcrete.getHeaders(head_body[0] + " \r\n\r\n");

        for (Header header : headers) {
            if (header.getSenderID() == this.peerId) {
                return;
            }
            if (!header.getVersion().equals("1.0") && Peer.getServer().getVersion().equals("1.0")) {
                System.out.println("Incoming message "+ header.getMessageType()+" with Version " + header.getVersion() + " from peer "+header.getSenderID()+" not supported");
                continue;
            }
            switch (header.getMessageType()) {
                case PUTCHUNK -> {

                    byte m[] = MessageType.createStored(header.getVersion(), this.peerId, header.getFileID(), header.getChunkNo());
                    DatagramPacket n_packet = new DatagramPacket(m, m.length, Peer.getServer().getMc().getAddress(), Peer.getServer().getMc().getPort());
                    boolean hasFile = Peer.getServer().getStoredFiles().containsKey(header.getFileID());
                    if (hasFile && Peer.getServer().getStoredFiles().get(header.getFileID()).getChunks().containsKey(header.getChunkNo())) {
                            Peer.getServer().getPool().schedule(() -> Peer.getServer().getMc().send(n_packet), new Random().nextInt(401), TimeUnit.MILLISECONDS);
                            break;
                    }

                    if (Peer.getServer().getVersion().equals("1.0")) {
                        putchunkAnswer(body, header, 401, n_packet);
                    } else if (Peer.getServer().getVersion().equals("1.1")) {           
                        byte[] finalBody = body;
                        Peer.getServer().getPool().schedule(() -> {
                            Chunk c = Peer.getServer().getWaitingForPutchunk().get(header.getFileID()+header.getChunkNo());
                            if(c!=null){
                                c.setRepDegree(header.getReplicationDeg());
                                if(c.shallSend()){
                                    putchunkAnswer(finalBody, header, 0, n_packet);
                                }
                                Peer.getServer().getWaitingForPutchunk().remove(header.getFileID()+header.getChunkNo());
                            }
                            else
                                putchunkAnswer(finalBody, header, 0, n_packet);
                        }, new Random().nextInt(801), TimeUnit.MILLISECONDS);
                    }
                }
                case STORED -> {
                    if (Peer.getServer().getMyFiles().containsKey(header.getFileID())) {
                        Peer.getServer().getMyFiles().get(header.getFileID()).addStored(header.getChunkNo(), header.getSenderID());
                    } else if (Peer.getServer().getStoredFiles().containsKey(header.getFileID()) && Peer.getServer().getStoredFiles().get(header.getFileID()).chunks.containsKey(header.getChunkNo())) {
                        Peer.getServer().getStoredFiles().get(header.getFileID()).addStored(header.getChunkNo(), header.getSenderID());
                    } else if (Peer.getServer().getVersion().equals("1.1")) {
                        if(Peer.getServer().getWaitingForPutchunk().containsKey(header.getFileID()+header.getChunkNo())){
                            Peer.getServer().getWaitingForPutchunk().get(header.getFileID()+header.getChunkNo()).getPeerList().put(header.getSenderID(),true);
                        }
                        else{
                            Chunk c = new Chunk(header.getChunkNo(), header.getFileID(), -1);
                            Peer.getServer().getWaitingForPutchunk().put(header.getFileID()+header.getChunkNo(), c);
                        }
                    }
                }
                case GETCHUNK -> {
                    Peer.getServer().getChunkQueue().remove(header.getFileID() + header.getChunkNo());
                    if (Peer.getServer().getStoredFiles().containsKey(header.getFileID())) {
                        Path name = Path.of(Peer.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo());
                        if (Files.exists(name)) {
                            try {
                                switch (Peer.getServer().getVersion()) {
                                    case "1.0" -> {
                                        byte[] file_content;
                                        file_content = Files.readAllBytes(name);
                                        byte[] message = MessageType.createChunk("1.0", (int) Peer.getServer().getPeerId(), header.getFileID(), header.getChunkNo(), file_content);
                                        DatagramPacket packet = new DatagramPacket(message, message.length, Peer.getServer().getMdr().getAddress(), Peer.getServer().getMdr().getPort());
                                        Peer.getServer().getPool().schedule(() -> {
                                            if (!(Peer.getServer().getChunkQueue().containsKey(header.getFileID() + header.getChunkNo()))) {
                                                Peer.getServer().getMdr().send(packet);
                                            }
                                            Peer.getServer().getChunkQueue().remove(header.getFileID() + header.getChunkNo());
                                        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                                    }
                                    case "1.1" -> {
                                        Peer.getServer().getPool().schedule(() -> {
                                            if (!(Peer.getServer().getChunkQueue().containsKey(header.getFileID() + header.getChunkNo()))) {
                                                try {
                                                    ServerSocket s = new ServerSocket(0);
                                                    s.setSoTimeout(2000);
                                                    byte[] message = MessageType.createChunk_1_1("1.1", (int) Peer.getServer().getPeerId(), header.getFileID(), header.getChunkNo(), InetAddress.getLocalHost().getHostAddress(), s.getLocalPort());
                                                    DatagramPacket packet = new DatagramPacket(message, message.length, Peer.getServer().getMdr().getAddress(), Peer.getServer().getMdr().getPort());
                                                    byte[] file_content;
                                                    file_content = Files.readAllBytes(name);
                                                    Socket n_s;
                                                    Peer.getServer().getMdr().send(packet);
                                                    n_s = s.accept();
                                                    n_s.getOutputStream().write(file_content);
                                                    n_s.close();
                                                } catch (IOException e) {
                                                }
                                            }
                                            Peer.getServer().getChunkQueue().remove(header.getFileID() + header.getChunkNo());
                                        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                                    }
                                }
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
                case DELETE -> {

                    if (Peer.getServer().getStoredFiles().containsKey(header.getFileID())) {
                        RemoteFile f = Peer.getServer().getStoredFiles().get(header.getFileID());
                        Peer.getServer().getStoredFiles().remove(header.getFileID());
                        f.delete();

                        if (Peer.getServer().getVersion().equals("1.1")) {
                            byte message[] = MessageType.createPurged("1.1", this.peerId, f.getFileId());
                            DatagramPacket packet = new DatagramPacket(message, message.length, Peer.getServer().getMc().getAddress(), Peer.getServer().getMc().getPort());
                            this.sendMessage(0, Peer.getServer().getPool(), packet);
                        }
                    }


                }
                case PURGED -> {
                    if (Peer.getServer().getWaitingForPurge().containsKey(header.getFileID())) {
                        Peer.getServer().getWaitingForPurge().get(header.getFileID()).removePeerFromChunks(header.getSenderID());
                        Peer.getServer().getWaitingForPurge().get(header.getFileID()).updateChunks();

                        if (Peer.getServer().getWaitingForPurge().containsKey(header.getFileID()) &&  Peer.getServer().getWaitingForPurge().get(header.getFileID()).getChunks().size() == 0) {
                            Peer.getServer().getWaitingForPurge().remove(header.getFileID());

                            try {
                                Files.walk(Path.of(Peer.getServer().getServerName() + "/.ldata/" + header.getFileID()))
                                        .sorted(Comparator.reverseOrder())
                                        .map(Path::toFile)
                                        .forEach(java.io.File::delete);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
                case AWAKE -> {
                    System.out.println("PEER " + header.getSenderID() + " AWOKE");
                    for (File file : Peer.getServer().getWaitingForPurge().values()) {
                        if (file.peerHasChunks(header.getSenderID())) {
                            byte message[] = MessageType.createDelete("1.1", this.peerId, file.getFileId());
                            DatagramPacket packet = new DatagramPacket(message, message.length, Peer.getServer().getMc().getAddress(), Peer.getServer().getMc().getPort());
                            this.sendMessage(0, Peer.getServer().getPool(), packet);
                        }
                    }
                }
                case REMOVED -> {
                    System.out.println("REMOVED CHUNK " + header.getChunkNo());
                    Chunk chunk = null;
                    boolean isLocalCopy = false;
                    if (Peer.getServer().getMyFiles().containsKey(header.getFileID())) {
                        System.out.println("My Files");
                        chunk = Peer.getServer().getMyFiles().get(header.getFileID()).getChunks().get(header.getChunkNo());

                    } else if (Peer.getServer().getStoredFiles().containsKey(header.getFileID())) {
                        System.out.println("Remote Files");
                        isLocalCopy = true;
                        chunk = Peer.getServer().getStoredFiles().get(header.getFileID()).getChunks().get(header.getChunkNo());

                    }
                    if (chunk != null) {
                        if (!isLocalCopy) {
                            if (chunk.getPeerList().containsKey(header.getSenderID())) {
                                chunk.getPeerList().remove(header.getSenderID());
                                chunk.updateLdata(Peer.getServer().getMyFiles().get(header.getFileID()).getName());
                                return;
                            }
                        } else if (chunk.getPeerList().containsKey(header.getSenderID())) {
                            chunk.getPeerList().remove(header.getSenderID());
                            chunk.updateRdata();
                        }
                        if (chunk.getRealDegree() < chunk.getRepDegree()) {
                            Path name = Path.of(Peer.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo());
                            if (Files.exists(name)) {
                                standbyBackupList.put(header.getFileID() + header.getChunkNo(), "yo");

                                Chunk finalChunk = chunk;
                                Peer.getServer().getPool().schedule(() -> {
                                    try {
                                        if (standbyBackupList.containsKey(header.getFileID() + header.getChunkNo())) {
                                            byte[] file_content;
                                            file_content = Files.readAllBytes(name);

                                            byte[] message1 = MessageType.createPutchunk("1.0", (int) Peer.getServer().getPeerId(), header.getFileID(), (int) header.getChunkNo(), finalChunk.getRepDegree(), file_content);
                                            byte[] message2 = MessageType.createStored("1.0", (int) Peer.getServer().getPeerId(), header.getFileID(), (int) header.getChunkNo());
                                            DatagramPacket packet1 = new DatagramPacket(message1, message1.length, Peer.getServer().getMdb().getAddress(), Peer.getServer().getMdb().getPort());
                                            DatagramPacket packet2 = new DatagramPacket(message2, message2.length, Peer.getServer().getMc().getAddress(), Peer.getServer().getMc().getPort());
                                            System.out.println("SENDING CHUNK NO " + finalChunk.getChunkNo());
                                            removeAux(1, Peer.getServer().getPool(), packet1, packet2, header.getFileID(), header.getChunkNo(), finalChunk.getRepDegree());
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
                    Peer.getServer().getChunkQueue().put(header.getFileID() + header.getChunkNo(), true);
                    if (Peer.getServer().getFileRestoring() != null && Peer.getServer().getFileRestoring().get(header.getFileID()) != null) {

                        switch (header.getVersion()) {
                            case "1.0" -> {
                                processChunk(header, body.length, body);
                            }
                            case "1.1" -> {
                                if (Peer.getServer().getFileRestoring().containsKey(header.getFileID())) {
                                    int read = 0;
                                    byte alloc[];
                                    try {
                                        byte[] tmp_buffer = new byte[64000];
                                        Socket s = new Socket(header.getAddress(), header.getPort());
                                        DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                                        read = in.read(tmp_buffer);
                                        if(read == -1)
                                            break;
                                        alloc = new byte[read];
                                        System.arraycopy(tmp_buffer, 0, alloc, 0, read);
                                    } catch (IOException e) {
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

    private void putchunkAnswer(byte[] body, Header header, int waitTime, DatagramPacket packet) {
        if (Peer.getServer().getMyFiles().containsKey(header.getFileID())) {
            return;
        }
        boolean hasSpace = (Peer.getServer().getMaxSize().get() == -1 || Peer.getServer().getCurrentSize().get() + body.length <= Peer.getServer().getMaxSize().get());
        boolean hasFile = Peer.getServer().getStoredFiles().containsKey(header.getFileID());

        standbyBackupList.remove(header.getFileID() + header.getChunkNo());
        
        if (hasSpace) {
                if (waitTime == 0) {
                    Peer.getServer().getMc().send(packet);
                } else {
                    Peer.getServer().getPool().schedule(() -> Peer.getServer().getMc().send(packet), new Random().nextInt(waitTime), TimeUnit.MILLISECONDS);
                }

                if (!hasFile) {
                    try {
                        Files.createDirectories(Paths.get(Peer.getServer().getServerName() + "/" + header.getFileID()));
                    } catch (IOException e) {
                        System.exit(1);
                    }
                    Peer.getServer().getStoredFiles().put(header.getFileID(), new RemoteFile(header.getFileID()));
                }

                Chunk c;
                if (Peer.getServer().getWaitingForPutchunk().containsKey(header.getFileID())) {
                    c = Peer.getServer().getWaitingForPutchunk().get(header.getFileID()+header.getChunkNo());
                    c.setSize(body.length);
                } else {
                    c = new Chunk(header.getChunkNo(), header.getFileID(), header.getReplicationDeg(), body.length);
                }
                c.getPeerList().put(peerId, true);
                c.updateRdata();
                c.getPeerList().put((int) Peer.getServer().getPeerId(), true);

                Peer.getServer().getStoredFiles().get(header.getFileID()).chunks.put(header.getChunkNo(), c);

                Path path = Paths.get(Peer.getServer().getServerName() + "/" + header.getFileID() + "/" + header.getChunkNo());
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

                fileChannel.write(buffer, 0, fileChannel, new CompletionHandler<Integer, AsynchronousFileChannel>() {
                @Override
                public void completed(Integer result, AsynchronousFileChannel attachment) {
                    try {
                        attachment.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable exc, AsynchronousFileChannel attachment) {
                    try {
                        attachment.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
                buffer.clear();
                Peer.getServer().getCurrentSize().addAndGet(body.length);
            }
    }

    synchronized private void processChunk(Header header, int buffer_length, byte[] buffer) {
        if(Peer.getServer().getFileRestoring().containsKey(header.getFileID())){
        if ((buffer_length < Peer.getServer().getChunkSize()) && (header.getChunkNo() < Peer.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks() - 1)) {
            return;
        }
        if (!Peer.getServer().getFileRestoring().get(header.getFileID()).getChunks().containsKey(header.getChunkNo())) {
            Peer.getServer().getFileRestoring().get(header.getFileID()).getChunks().put(header.getChunkNo(), buffer);
            Integer totalC = Peer.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks();
            Integer numC = Peer.getServer().getFileRestoring().get(header.getFileID()).getChunks().size();
            if(totalC != null && (numC%5 == 0 || numC.equals(totalC))){
                System.out.println("Restore "+numC*100/totalC+"% done");
            }
        }


        if (Peer.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks() != null && Peer.getServer().getFileRestoring().get(header.getFileID()).getChunks().values().size() == Peer.getServer().getFileRestoring().get(header.getFileID()).getNumberOfChunks()) {
            RestoreFile f = Peer.getServer().getFileRestoring().get(header.getFileID());
            String folder = Peer.getServer().getServerName() + "/" + "restored";
            if (!Files.exists(Path.of(folder))) {
                try {
                    Files.createDirectories(Path.of(folder));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String[] a = Peer.getServer().getMyFiles().get(header.getFileID()).getName().split("/");
            Path path = Paths.get(folder + "/" + a[a.length-1]);
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

                fileChannel.write(l_buffer, iterator * Peer.getServer().getChunkSize(), fileChannel, new CompletionHandler<Integer, AsynchronousFileChannel>() {
                    @Override
                    public void completed(Integer result, AsynchronousFileChannel attachment) {
                        try {
                            attachment.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failed(Throwable exc, AsynchronousFileChannel attachment) {
                        try {
                            attachment.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                l_buffer.clear();
            }
            Peer.getServer().getFileRestoring().remove(header.getFileID());
        }
    }
    }

    private void removeAux(int i, ScheduledExecutorService pool, DatagramPacket packet1, DatagramPacket
            packet2, String fileId, int chunkNo, int repDegree) {
        Peer.getServer().getMdb().send(packet1);

        Peer.getServer().getPool().schedule(() -> Peer.getServer().getMc().send(packet2), new Random().nextInt(401), TimeUnit.MILLISECONDS);

        System.out.println("TRYING again " + chunkNo);
        pool.schedule(() -> {
            if (Peer.getServer().getMyFiles().get(fileId).getReplicationDegree(chunkNo) < repDegree) {
                if (i < 16) {
                    //System.out.println("Against: " + i + " " + this.agains.getAndIncrement() + " " + chunkNo);
                    this.removeAux(i * 2, pool, packet1, packet2, fileId, chunkNo, repDegree);
                } else {
                    System.out.println("Gave up on removed backup subprotocol");
                }
            }
        }, i * 1000L + new Random().nextInt(401), TimeUnit.MILLISECONDS);
    }

    private void sendMessage(int i, ScheduledExecutorService pool, DatagramPacket packet) {
        Peer.getServer().getMc().send(packet);
        pool.schedule(() -> {
            if (i < 5) {
                this.sendMessage(i + 1, pool, packet);
            }
        }, new Random().nextInt(401), TimeUnit.MILLISECONDS);
    }

}
