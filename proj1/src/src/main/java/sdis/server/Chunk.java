package sdis.server;

import sdis.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Chunk {
    int repDegree = 0;
    private int realDegree = 0;
    private int chunkNo = 0;
    private String fileId;
    private ConcurrentHashMap<Integer, Boolean> peerCount = null;


    public Chunk(int chunkNo, String fileId, int repDegree) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.peerCount = new ConcurrentHashMap<>();
    }

    public Chunk(int chunkNo, String fileId, int repDegree, int realDegree) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
        this.repDegree = repDegree;
        this.realDegree = realDegree;
    }

    public int getChunkNo() {
        return this.chunkNo;
    }

    synchronized ConcurrentHashMap<Integer, Boolean> getPeerList() {
        return this.peerCount;
    }

    public int getPeerCount() {
        if (this.peerCount != null)
            return this.peerCount.size();
        return this.realDegree;
    }


    public int getRepDegree() {
        return this.repDegree;
    }

    public void backup(ScheduledExecutorService pool) {
        Path name = Path.of(Server.getServer().getServerName() + "/" + this.fileId + "/" + this.chunkNo);
        if (Files.exists(name)) {
            try {
                byte file_content[];
                file_content = Files.readAllBytes(name);
                String body = MessageType.createPutchunk("1.0", (int) Server.getServer().getPeerId(), this.fileId, this.chunkNo, this.repDegree, new String(file_content));
                DatagramPacket packet = new DatagramPacket(body.getBytes(), body.length(), Server.getServer().getMc().getAddress(), Server.getServer().getMc().getPort());
                this.backup(pool, 0, packet);
            } catch (IOException e) {
                return;
            }
        }
    }

    private void backup(ScheduledExecutorService pool, int i, DatagramPacket packet) {
        Server.getServer().getMdb().send(packet);
        pool.schedule(() -> {
            if (Server.getServer().getMyFiles().get(this.fileId).getReplicationDegree(this.chunkNo) < this.repDegree && i < 16)
                this.backup(pool, i * 2, packet);
        }, i, TimeUnit.SECONDS);
    }

}
