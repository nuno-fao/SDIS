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

/**
 * Used as a worker, processes the received messages and sends the response
 */
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

    /**
     * Processes the read message and sends the response
     */
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
        }
    }



}
