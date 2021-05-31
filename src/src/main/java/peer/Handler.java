package peer;

import peer.tcp.TCPReader;
import peer.tcp.TCPWriter;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Used as a worker, processes the received messages and sends the response
 */
public class Handler {
    private byte[] message;
    private int peerId;
    private Chord chord;
    private Address address;
    private ConcurrentHashMap<String, File> localFiles;
    private ConcurrentHashMap<String, File> localCopies;
    private AtomicLong maxSize;
    private AtomicLong currentSize;
    private ConcurrentHashMap<String, Boolean> receivedMessages;
    private CopyOnWriteArraySet<BigInteger> notStoredFiles;

    Handler(byte[] message, int peerId, Chord chord, Address address, ConcurrentHashMap<String, File> localFiles, ConcurrentHashMap<String, File> localCopies, AtomicLong maxSize, AtomicLong currentSize, ConcurrentHashMap<String, Boolean> receivedMessages, CopyOnWriteArraySet<BigInteger> notStoredFiles) {
        this.message = message;
        this.peerId = peerId;
        this.chord = chord;
        this.address = address;
        this.localFiles = localFiles;
        this.localCopies = localCopies;
        this.maxSize = maxSize;
        this.currentSize = currentSize;
        this.receivedMessages = receivedMessages;
        this.notStoredFiles = notStoredFiles;
    }

    public void processMessage() {
        String stringMessage = new String(this.message);
        if (stringMessage.startsWith("CHORD")) {
            this.chord.processMessage(this.message);
            return;
        }

        System.out.println(stringMessage);
        Header headers = HeaderConcrete.getHeaders(new String(this.message));

        if (headers.getMessageType() != MessageType.PUTERROR) {
            if (this.receivedMessages.containsKey(headers.getMessageId())) {
                System.out.println(new String(this.message));
                if (headers.getMessageType() == MessageType.PUTFILE) {
                    Node successor = this.chord.FindSuccessor(headers.getInitiator());
                    TCPWriter tcpWriter = new TCPWriter(successor.address.address, successor.address.port);
                    byte[] contents;
                    contents = MessageType.createPutError(headers.getInitiator(), headers.getFileID(), headers.getReplicationDeg());
                    tcpWriter.write(contents);
                }
                return;
            } else {
                this.receivedMessages.put(headers.getMessageId(), true);
            }
        }

        switch (headers.getMessageType()) {
            case GETFILE: {
                new GetFileHandler(this.peerId, headers.getFileID(), headers.getAddress(), headers.getPort(), headers.getMessageId(), this.localCopies, this.chord);
                break;
            }
            case PUTFILE: {
                new PutFileHandler(this.chord, headers.getInitiator(), headers.getFileID(), this.peerId, headers.getReplicationDeg(), this.address, new Address(headers.getAddress(), headers.getPort()), headers.getMessageId(), this.localFiles, this.localCopies, this.maxSize, this.currentSize);
                break;
            }
            case DELETE: {
                new DeleteHandler(this.peerId, headers.getFileID(), headers.getReplicationDeg(), headers.getMessageId(), this.localCopies, this.chord, this.currentSize);
                break;
            }
            case PUTERROR: {
                new PutErrorHandler(this.peerId, headers.getInitiator(), headers.getFileID(), headers.getReplicationDeg(), this.chord, this.notStoredFiles);
            }
        }
    }
}

class PutFileHandler {
    private String fileId;
    private Address local, remote;
    private int peerId, repDegree, initiator;
    private Chord chord;
    private ConcurrentHashMap<String, File> localCopies, localFiles;
    private String messageId;
    private AtomicLong maxSize, currentSize;

    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public PutFileHandler(Chord chord, int initiator, String fileId, int peerId, int repDegree, Address localAddress, Address remoteAddress, String messageId, ConcurrentHashMap<String, File> localFiles, ConcurrentHashMap<String, File> localCopies, AtomicLong maxSize, AtomicLong currentSize) {
        this.fileId = fileId;
        this.peerId = peerId;
        this.local = localAddress;
        this.remote = remoteAddress;
        this.chord = chord;
        this.repDegree = repDegree;
        this.localCopies = localCopies;
        this.localFiles = localFiles;
        this.messageId = messageId;
        this.initiator = initiator;
        this.currentSize = currentSize;
        this.maxSize = maxSize;

        File f = null;
        try {
            f = this.Receive();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (f != null) {
            try {
                localCopies.put(f.getFileId(), f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void PropagateSend(byte[] data, int replicationDegree) throws IOException {
        Node successor = this.chord.getSuccessor();
        TCPWriter messageWriter = new TCPWriter(successor.address.address, successor.address.port);

        SSLServerSocket s = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);
        s.setNeedClientAuth(true);

        System.out.println("Propagating with RepDegree: " + replicationDegree);

        byte[] contents = MessageType.createPutFile(this.peerId, this.initiator, this.fileId, this.local.address, Integer.toString(s.getLocalPort()), replicationDegree, this.messageId);
        messageWriter.write(contents);

        s.accept().getOutputStream().write(data);
    }

    private File Receive() throws IOException {
        if (this.repDegree > 0) {
            TCPReader reader = new TCPReader(this.remote.address, this.remote.port);

            InputStream in;
            int bufferSize = 0;
            int fileSize = 0;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean shallWrite = !this.localFiles.containsKey(this.fileId) && !this.localCopies.containsKey(this.fileId);

            File file = new File(this.fileId, String.valueOf(this.peerId), null, bufferSize, this.repDegree);
            try {
                bufferSize = reader.getSocket().getReceiveBufferSize();
                in = reader.getSocket().getInputStream();
                DataInputStream clientData = new DataInputStream(in);
                OutputStream output = null;
                long size = clientData.readLong();
                shallWrite &= (this.maxSize.get() == -1 || this.maxSize.get() > this.currentSize.get() + size);
                if (shallWrite)
                    output = new FileOutputStream(this.peerId + "/stored/" + this.fileId);
                byte[] buffer = new byte[bufferSize];
                int read;
                out.write(longToBytes(size));
                while ((read = clientData.read(buffer)) != -1) {
                    fileSize += read;
                    if (shallWrite)
                        output.write(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            file.setFileSize(fileSize);
            if (!shallWrite) {
                System.out.println("batata doce");
                System.out.println(out.size());
                this.PropagateSend(out.toByteArray(), this.repDegree);
                return this.localFiles.get(this.fileId);
            } else if (this.repDegree > 1) {
                this.PropagateSend(out.toByteArray(), this.repDegree - 1);
                this.currentSize.getAndAdd(fileSize);
            } else {
                this.currentSize.getAndAdd(fileSize);
            }
            return file;
        }
        return null;
    }
}


class GetFileHandler {

    private String fileId;
    private String address;
    private int port;
    private ConcurrentHashMap<String, File> localCopies;
    private int peerId;
    private Chord chord;
    private String messageId;

    public GetFileHandler(int peerId, String fileId, String address, int port, String messageId, ConcurrentHashMap<String, File> localCopies, Chord chord) {
        this.fileId = fileId;
        this.address = address;
        this.port = port;
        this.localCopies = localCopies;
        this.peerId = peerId;
        this.chord = chord;
        this.messageId = messageId;


        if (!this.hasCopy()) {
            this.resendMessage();
        } else {
            this.sendFile();
        }
    }

    public boolean hasCopy() {
        return this.localCopies.containsKey(this.fileId);
    }

    public void resendMessage() {
        Node successor = this.chord.getSuccessor();
        TCPWriter tcpWriter = new TCPWriter(successor.address.address, successor.address.port);
        byte[] contents;
        contents = MessageType.createGetFile(this.peerId, this.fileId, this.address, Integer.toString(this.port), this.messageId);

        tcpWriter.write(contents);
    }

    public void sendFile() {
        try {
            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.address, this.port);
            final java.io.File myFile = new java.io.File(this.peerId + "/stored/" + this.fileId); //sdcard/DCIM.JPG
            byte[] mybytearray = new byte[30000];
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            OutputStream os;
            try {
                os = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                dos.writeLong(mybytearray.length);
                int read;
                while ((read = dis.read(mybytearray)) != -1) {
                    dos.write(mybytearray, 0, read);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class DeleteHandler {

    private String fileId;
    private int replicationDegree;
    private ConcurrentHashMap<String, File> localCopies;
    private int peerId;
    private Chord chord;
    private String messageId;
    private AtomicLong currentSize;

    public DeleteHandler(int peerId, String fileId, int replicationDegree, String messageId, ConcurrentHashMap<String, File> localCopies, Chord chord, AtomicLong currentSize) {
        this.fileId = fileId;
        this.replicationDegree = replicationDegree;
        this.localCopies = localCopies;
        this.peerId = peerId;
        this.chord = chord;
        this.messageId = messageId;
        this.currentSize = currentSize;

        if (!this.hasCopy()) {
            this.resendMessage();
        } else {
            this.deleteFile();
            System.out.println("Deleting file " + fileId);
            if (this.replicationDegree > 0) {
                this.resendMessage();
            }
        }
    }

    public boolean hasCopy() {
        return this.localCopies.containsKey(this.fileId);
    }

    public void resendMessage() {
        Node successor = this.chord.getSuccessor();
        TCPWriter tcpWriter = new TCPWriter(successor.address.address, successor.address.port);
        byte[] contents;
        contents = MessageType.createDelete(this.peerId, this.fileId, this.replicationDegree, this.messageId);
        tcpWriter.write(contents);
    }

    public void deleteFile() {
        this.localCopies.get(this.fileId).deleteFile(this.currentSize);
        this.replicationDegree--;
        this.localCopies.remove(this.fileId);
    }

}

class PutErrorHandler {
    private int peerId, replicationDeg, initiator;
    private String fileId;
    private Chord chord;

    public PutErrorHandler(int peerId, int initiator, String fileId, Integer replicationDeg, Chord chord, CopyOnWriteArraySet<BigInteger> notStoredFiles) {
        this.peerId = peerId;
        this.replicationDeg = replicationDeg;
        this.initiator = initiator;
        this.fileId = fileId;
        this.chord = chord;

        if (this.peerId == this.initiator) {
            System.out.println("Error maintaining replication degree, missing " + replicationDeg + " copies of file with ID " + this.fileId);
            notStoredFiles.add(new BigInteger(this.fileId));
        } else {
            // resendMessage();
        }
    }

    public void resendMessage() {
        Node successor = this.chord.FindSuccessor(this.initiator);
        TCPWriter tcpWriter = new TCPWriter(successor.address.address, successor.address.port);
        byte[] contents;
        contents = MessageType.createPutError(this.initiator, this.fileId, this.replicationDeg);
        tcpWriter.write(contents);
    }
}