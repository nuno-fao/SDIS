package peer;

import java.util.ArrayList;
import java.util.List;


//todo adicionar:
//  Find Sucessor
//  Find Predecessor
//  Notify
//
public class Chord {
    Node successorPredecessor = null;
    Node successor = null;
    Node predecessor = null;
    Node n;
    int m;
    List<Node> fingerTable = new ArrayList<>(m);
    Node[] waitingForResponses = new Node[20];
    int lastWaitingIndexUsed = 0;
    private int next = 0;

    public Chord(int id, String address, int port) {
        this.n = new Node(address, port, id);
    }

    public Node FindSuccessor(Integer id) {
        if (this.n.id < id && this.successor.id >= id) {
            return successor;
        } else {
            Node nl = ClosestPrecedingNode(id);
            return remoteFindSuccessor(nl, id);
        }
    }

    public Node ClosestPrecedingNode(Integer id)
    {
        for (int i = this.fingerTable.size() - 1; i >= 0; i--) {
            Node currentNode = this.fingerTable.get(i);
            if (currentNode.id > this.n.id && currentNode.id <= id) return currentNode;
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
    }

    public void Join(Node nl) {
        this.predecessor = null;
        this.successor = this.FindSuccessor(this.n.id);
    }

    public void Stabilize() {
        Node x = getSuccessorPredecessor();
        if (x.id > this.n.id && x.id <= this.successor.id) {
            this.successor = x;
        }
        notifySuccThatImPred();
    }

    public void Notify(Node possiblePredecessor) {
        if (this.predecessor == null || (possiblePredecessor.id > this.predecessor.id && possiblePredecessor.id <= this.n.id)) {
            this.predecessor = possiblePredecessor;
        }
    }

    public void FixFingers() {
        this.next++;
        if (this.next > this.fingerTable.size() - 1) {
            this.next = 0;
        }
        this.fingerTable.set(this.next, this.FindSuccessor(this.n.id + (int) Math.pow(2, this.next)));
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
            String response = "CHORD GET_PRED " + this.predecessor.address.address + ":" + this.predecessor.address.port + ":" + this.predecessor.id;
            byte[] responseBytes = response.getBytes();
            writer.write(responseBytes);
        }
    }

    public void setSuccessorPredecessor(byte[] message) {
        Node possibleSuccessorPredecessor = parseMessage(message, "GET_PRED");
        if (possibleSuccessorPredecessor != null) this.successorPredecessor = possibleSuccessorPredecessor;
    }

    public Node getSuccessorPredecessor()
    {
        this.successorPredecessor = null;
        TCPWriter writer = new TCPWriter(successor.address.address, successor.address.port);
        String message = "CHORD REQ_PRED " + this.n.id;
        byte[] messageBytes = message.getBytes();
        writer.write(messageBytes);
        while (successorPredecessor == null)
        {
            try
            {
               Thread.sleep(1000); 
            }
            catch (InterruptedException e)
            {

            }
        }
        return successorPredecessor;
    }

    public void notifySuccThatImPred()
    {
        TCPWriter writer = new TCPWriter(this.successor.address.address, this.successor.address.port);
        String message = "CHORD NOTIFY " + this.n.address.address + ":" + this.n.address.port + ":" + this.n.id;
        byte[] messageBytes = message.getBytes();
        writer.write(messageBytes);
    }

    public void setPredecessor(byte[] message) {
        Node possiblePredecessor = parseMessage(message, "NOTIFY");
        if (possiblePredecessor != null) this.Notify(possiblePredecessor);
    }

    private Node parseMessage(byte[] message, String expectedSegment)
    {
        String stringMessage = new String(message);
        String[] strParts = stringMessage.split(" ");
        if (strParts[0].equals("CHORD") && strParts[1].equals(expectedSegment))
        {
            String[] nodeParts = strParts[2].split(":");
            String address = nodeParts[0];
            int port = Integer.parseInt(nodeParts[1]);
            Integer id = Integer.valueOf(nodeParts[2]);
            Node node = new Node(address, port, id);
            return node;
        }
        return null;
    }

    private Node remoteFindSuccessor(Node remoteNode, Integer id)
    {
        int index;
        synchronized (this)
        {
            if (this.lastWaitingIndexUsed == 20) this.lastWaitingIndexUsed = 0;
            index = this.lastWaitingIndexUsed++;   
        }
        String message = "CHORD LOOKUP " + this.n.address.address + ":" + this.n.address.port + ":" + this.n.id + " " + id + " " + index;
        TCPWriter writer = new TCPWriter(remoteNode.address.address, remoteNode.address.port);
        byte[] messageBytes = message.getBytes();
        writer.write(messageBytes);
        waitingForResponses[index] = null;
        while (waitingForResponses[index] == null)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {

            }
        }
        return waitingForResponses[index];
    }

    public void returnSuccessor(byte[] message)
    {
        //process reception of LOOKUP
        //return to original author the successor of the id requested
    }

    public void setSuccessorForId(byte[] message)
    {
        //process reception of NODE
    }
}

class Node {
    public Address address;
    public Integer id;

    public Node(String address, int port, Integer id) {
        this.address = new Address(address, port);
        this.id = id;
    }
}

