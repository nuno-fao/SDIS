package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardOpenOption.*;

public class Chunk {
    private int repDegree = 0;
    private int realDegree = 0;
    private int chunkNo = 0;
    private String fileId;
    private ConcurrentHashMap<Integer, Boolean> peerCount = null;
    private AtomicBoolean shallSend = new AtomicBoolean(true);
    private int size = 0;

    public int getRealDegree() {
        return realDegree;
    }

    public Chunk(int chunkNo, String fileId, int repDegree) {

        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.peerCount = new ConcurrentHashMap<>();
    }

    Chunk(int chunkNo, String fileId, int repDegree, int size) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.peerCount = new ConcurrentHashMap<>();
        this.size = size;
    }

    public Chunk(int chunkNo, String fileId, int repDegree, int realDegree, int size) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.realDegree = realDegree;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public String getFileId() {
        return this.fileId;
    }

    void subtractRealDegree() {
        this.realDegree--;
    }

    public int getChunkNo() {
        return this.chunkNo;
    }

    public ConcurrentHashMap<Integer, Boolean> getPeerList() {
        if (this.peerCount == null) {
            this.peerCount = new ConcurrentHashMap<>();
        }
        return this.peerCount;
    }

    public int getPeerCount() {
        if (this.peerCount != null) {
            return this.peerCount.size();
        }
        return this.realDegree;
    }


    public int getRepDegree() {
        return this.repDegree;
    }


    synchronized void updateRdata() {
        Path path = Paths.get(Server.getServer().getServerName() + "/.rdata/" + this.fileId + "/" + this.chunkNo);
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, TRUNCATE_EXISTING, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte out[] = (this.getPeerCount() + ";" + this.repDegree + ";" + size).getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(out.length);

        buffer.put(out);
        buffer.flip();

        Future<Integer> operation = fileChannel.write(buffer, 0);
        buffer.clear();
    }

    synchronized void updateLdata(String filename) {
        Path path = Paths.get(Server.getServer().getServerName() + "/.ldata/" + this.fileId + "/" + this.chunkNo);
        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(
                    path, WRITE, TRUNCATE_EXISTING, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        sb.append((getPeerCount() + ";" + getRepDegree() + ";" + filename + "\n"));
        for (Iterator<Integer> it = getPeerList().keys().asIterator(); it.hasNext(); ) {
            sb.append(it.next() + ";");
        }

        byte out[] = sb.toString().getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(out.length);

        buffer.put(out);
        buffer.flip();

        Future<Integer> operation = fileChannel.write(buffer, 0);
        buffer.clear();
    }


}
