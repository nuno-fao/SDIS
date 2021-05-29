package peer;

import java.io.IOError;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Chord {
    private Node successorPredecessor = null;
    private Node successor = null;
    private Node predecessor = null;
    private Node sucessorSuccessor = null;
    Node n;
    public static int m = 5;
    Node[] fingerTable = new Node[m];
    Node[] waitingForResponses = new Node[50];
    int lastWaitingIndexUsed = 0;
    private int next = -1;

    public Chord(int id, String address, int port) {
        this.n = new Node(address, port, id);
    }

    public boolean isBetween(Integer smaller, Integer comp, Integer bigger)
    {
        if (bigger > smaller)
        {
            return comp > smaller && comp <= bigger;
        }
        else if (smaller > bigger)
        {
            return comp <= bigger || comp > smaller;
        }
        else
        {
            if (smaller == bigger && smaller == this.n.id) return true;
            return this.n.equals(this.successor) && this.predecessor == null;
        } 
    }

    public Node FindSuccessor(Integer id) {
        Integer processedId = id % ((int) (Math.pow(2, m)));
        if (processedId == this.n.id) return this.n;
        if (isBetween(n.id, processedId, successor.id)) {
            return successor;
        } else {
            Node nl = ClosestPrecedingNode(processedId);
            return remoteFindSuccessor(nl, processedId);
        }
    }

    public Node ClosestPrecedingNode(Integer id)
    {
        for (int i = this.fingerTable.length - 1; i >= 0; i--) {
            Node currentNode = this.fingerTable[i];
            if (currentNode == null) continue;
            if (isBetween(n.id, currentNode.id, id)) return currentNode;
        }
        return this.n;
    }

    public Node FindPredecessor(Node retriever) {
        System.exit(11);
        return null;
    }

    public void Create() {
        this.predecessor = null;
        this.successor = this.n;
        this.fingerTable[0] = this.successor;
    }

    public void Join(Node nl) {
        this.predecessor = null;
        this.successor = remoteFindSuccessor(nl, this.n.id);
        this.fingerTable[0] = this.successor;
    }

    public void Stabilize() {
        Node x = getSuccessorPredecessor();
        if (x != null && isBetween(n.id, x.id, successor.id)) {
            this.successor = x;
            this.fingerTable[0] = this.successor;
        }
        notifySuccThatImPred();
    }

    public void Notify(Node possiblePredecessor) {
        if (this.predecessor == null || isBetween(this.predecessor.id, possiblePredecessor.id, n.id)) {
            if (!possiblePredecessor.equals(this.n)) this.predecessor = possiblePredecessor;
        }
    }

    public void FixFingers() {
        this.next++;
        if (this.next > this.fingerTable.length - 1) {
            this.next = 0;
        }
        this.fingerTable[this.next] = this.FindSuccessor(this.n.id + (int) Math.pow(2, this.next));
    }

    public void CheckPredecessor() {
        if (predecessor != null)
        {
            Address address = new Address(this.predecessor.address.address, this.predecessor.address.port);
            if (!TCPWriter.IsAlive(address))
            {
                this.predecessor = null;
            }
        }
    }



    //Methods that aren't part of the paper's pseudocode

    public Node getSuccessor() {
        return successor;
    }

    public void getPredecessor(byte[] message) {
        Node messageSender = parseMessage(message, "REQ_PRED");
        if (messageSender != null)
        {
            TCPWriter writer = new TCPWriter(messageSender.address.address, messageSender.address.port);
            String response;
            if (this.predecessor != null)
            {
                response = "CHORD GET_PRED " + this.predecessor.toString();
            }
            else
            {
                response = "CHORD GET_PRED " + this.n.toString();
            }
            byte[] responseBytes = response.getBytes();
            writer.write(responseBytes);
            writer.close();
        }
    }

    public void setSuccessorPredecessor(byte[] message) {
        Node possibleSuccessorPredecessor = parseMessage(message, "GET_PRED");
        if (possibleSuccessorPredecessor != null) this.successorPredecessor = possibleSuccessorPredecessor;
    }

    public Node getSuccessorPredecessor()
    {
        this.successorPredecessor = null;
        try
        {
            TCPWriter writer = new TCPWriter(successor.address.address, successor.address.port, true);
            String message = "CHORD REQ_PRED " + this.n.toString();
            byte[] messageBytes = message.getBytes();
            writer.write(messageBytes);
            writer.close();

            while (this.successorPredecessor == null)
            {
                try
                {
                Thread.sleep(50); 
                }
                catch (InterruptedException e)
                {

                }
            }
            return successorPredecessor;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public void notifySuccThatImPred()
    {
        try
        {
            TCPWriter writer = new TCPWriter(this.successor.address.address, this.successor.address.port, true);
            String message = "CHORD NOTIFY " + this.n.toString();
            byte[] messageBytes = message.getBytes();
            writer.write(messageBytes);
            writer.close();
        }
        catch (IOException e)
        {
            if (this.sucessorSuccessor != null)
            {
                this.successor = this.sucessorSuccessor;
                this.fingerTable[0] = this.successor;
            }
        }
    }

    public void setPredecessor(byte[] message) {
        Node possiblePredecessor = parseMessage(message, "NOTIFY");
        if (possiblePredecessor != null) this.Notify(possiblePredecessor);
    }

    public Node parseMessage(byte[] message, String expectedSegment)
    {
        String stringMessage = new String(message);
        String[] strParts = stringMessage.split(" ");
        if (strParts[0].equals("CHORD") && strParts[1].equals(expectedSegment))
        {
            Node node = new Node(strParts[2]);
            return node;
        }
        return null;
    }

    public Node remoteFindSuccessor(Node remoteNode, Integer id)
    {
        try
        {
            TCPWriter writer = new TCPWriter(remoteNode.address.address, remoteNode.address.port, true);
            int index;
            synchronized (this)
            {
                if (this.lastWaitingIndexUsed == 50) this.lastWaitingIndexUsed = 0;
                index = this.lastWaitingIndexUsed++;   
            }
            String message = "CHORD LOOKUP " + this.n.toString() + " " + id + " " + index;
            byte[] messageBytes = message.getBytes();
            writer.write(messageBytes, true);
            writer.close();
            waitingForResponses[index] = null;
            while (waitingForResponses[index] == null)
            {
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {

                }
            }
            return waitingForResponses[index];
        }
        catch (IOException e)
        {
            try
            {
                TCPWriter writer = new TCPWriter(successor.address.address, successor.address.port, true);
                int index;
                synchronized (this)
                {
                    if (this.lastWaitingIndexUsed == 50) this.lastWaitingIndexUsed = 0;
                    index = this.lastWaitingIndexUsed++;   
                }
                String message = "CHORD LOOKUP " + this.n.toString() + " " + id + " " + index;
                byte[] messageBytes = message.getBytes();
                writer.write(messageBytes, true);
                writer.close();
                waitingForResponses[index] = null;
                while (waitingForResponses[index] == null)
                {
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e2)
                    {

                    }
                }
                return waitingForResponses[index];
            }
            catch (IOException e3)
            {
                this.successor = this.sucessorSuccessor;
                this.fingerTable[0] = this.successor;
                TCPWriter writer = new TCPWriter(successor.address.address, successor.address.port);
                int index;
                synchronized (this)
                {
                    if (this.lastWaitingIndexUsed == 50) this.lastWaitingIndexUsed = 0;
                    index = this.lastWaitingIndexUsed++;   
                }
                String message = "CHORD LOOKUP " + this.n.toString() + " " + id + " " + index;
                byte[] messageBytes = message.getBytes();
                writer.write(messageBytes);
                writer.close();
                waitingForResponses[index] = null;
                while (waitingForResponses[index] == null)
                {
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e2)
                    {

                    }
                }
                return waitingForResponses[index];
            }
            
        }
        
    }

    public void returnSuccessor(byte[] message)
    {
        Node messageAuthor = parseMessage(message, "LOOKUP");
        if (messageAuthor != null)
        {
            String stringMessage = new String(message);
            String[] messageParts = stringMessage.split(" ");
            Integer id = Integer.valueOf(messageParts[3]);
            int index = Integer.parseInt(messageParts[4]);
            Node requestedNode = FindSuccessor(id);

            String response = "CHORD NODE " + requestedNode.toString() + " " + index;
            TCPWriter writer = new TCPWriter(messageAuthor.address.address, messageAuthor.address.port);
            writer.write(response.getBytes());
            writer.close();
        }
    }

    public void setSuccessorForId(byte[] message)
    {
        Node nodeRequested = parseMessage(message, "NODE");
        if (nodeRequested != null)
        {
            String stringMessage = new String(message);
            String[] messageParts = stringMessage.split(" ");
            int index = Integer.parseInt(messageParts[3]);
            this.waitingForResponses[index] = nodeRequested;
        }
    }

    public void requestSuccessorSuccessor()
    {
        try
        {
            TCPWriter writer = new TCPWriter(this.successor.address.address, this.successor.address.port, true);
            String message = "CHORD REQ_SUCC " + this.n.toString();
            writer.write(message.getBytes());
            writer.close();
        }
        catch (IOException e)
        {
            if (this.sucessorSuccessor != null)
            {
                this.successor = this.sucessorSuccessor;
                this.fingerTable[0] = this.successor;
            }
        }

    }

    public void setSuccessorSuccessor(byte[] message)
    {
        Node successorSuccessor = this.parseMessage(message, "GET_SUCC");
        if (successorSuccessor != null) this.sucessorSuccessor = successorSuccessor;
    }

    public void getSuccessor(byte[] message)
    {
        Node messageSender = this.parseMessage(message, "REQ_SUCC");
        if (messageSender != null)
        {
            TCPWriter writer = new TCPWriter(messageSender.address.address, messageSender.address.port);
            String response = "CHORD GET_SUCC " + this.successor.toString();
            writer.write(response.getBytes());
            writer.close();
        }
    }

    public void processMessage(byte[] message)
    {
        String stringMessage = new String(message);
        System.out.println(stringMessage);
        if (!stringMessage.startsWith("CHORD")) return;

        String secondSegment = stringMessage.split(" ")[1];
        switch (secondSegment)
        {
            case "LOOKUP":
            {
                returnSuccessor(message);
                break;
            }
            case "NODE":
            {
                setSuccessorForId(message);
                break;
            }
            case "REQ_PRED":
            {
                getPredecessor(message);
                break;
            }
            case "GET_PRED":
            {
                setSuccessorPredecessor(message);
                break;
            }
            case "NOTIFY":
            {
                setPredecessor(message);
                break;
            }
            case "REQ_SUCC":
            {
                getSuccessor(message);
                break;
            }
            case "GET_SUCC":
            {
                setSuccessorSuccessor(message);
                break;
            }
            default: return;
        }
    }
}

class Node {
    public Address address;
    public Integer id;

    public Node(String address, int port, Integer id) {
        this.address = new Address(address, port);
        this.id = id;
    }

    public Node(String stringRepresentation)
    {
        int colonCount = 0;
        for (int i = 0; i < stringRepresentation.length(); i++)
        {
            if (stringRepresentation.charAt(i) == ':') colonCount++;
        }

        if (colonCount == 2)
        {
            String[] parts = stringRepresentation.split(":");
            String address = parts[0];
            int port = Integer.parseInt(parts[1]);
            Integer id = Integer.valueOf(parts[2]);
            
            this.address = new Address(address, port);
            this.id = id; 
        }
        else
        {
            String[] parts = stringRepresentation.split(":");
            String address = parts[0];
            int port = Integer.parseInt(parts[1]);

            try
            {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] digest = md.digest((address + ":" + port).getBytes());
                BigInteger num = new BigInteger(1, digest);
                this.id = num.mod(BigDecimal.valueOf(Math.pow(2, Chord.m)).toBigInteger()).intValue();
            }
            catch (NoSuchAlgorithmException e)
            {
                System.out.println("Invalid algorithm");
            }
            this.address = new Address(address, port);
        }

    }

    @Override
    public String toString() {
        return address.address + ":" + address.port + ":" + id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof Node)) return false;

        Node rhs = (Node) obj;

        return this.id.equals(rhs.id) && this.address.equals(rhs.address);

    }
}

