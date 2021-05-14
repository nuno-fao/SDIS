package peer;

import java.util.ArrayList;
import java.util.List;


//todo adicionar:
//  Find Sucessor
//  Find Predecessor
//  Notify
//
public class Chord {
    Node successor = null;
    Node predecessor = null;
    Node n;
    List<Node> fingerTable = new ArrayList<>(5);
    private int next = 0;

    public Chord(int id, String address, int port) {
        this.n = new Node(address, port, id);
    }

    public Node FindSuccessor(Integer id) {
        if (this.fingerTable.get(0).id > id && id >= this.n.id) {
            return this.fingerTable.get(0);
        } else {
            Node nl;
            for (int i = this.fingerTable.size() - 1; i >= 0; i--) {
                if (this.n.id < this.fingerTable.get(i).id && this.fingerTable.get(i).id < id) {
                    nl = this.fingerTable.get(i);
                    break;
                }
            }
            //todo node nl.find_successor(id)
        }
        return null;
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
        Node x = this.FindPredecessor(this.successor);
        if (x.id > this.n.id && x.id < this.successor.id) {
            this.successor = x;
        }
        //todo notify
    }

    public void Notify(Node possiblePredecessor) {
        if (this.predecessor == null || (possiblePredecessor.id > this.predecessor.id && possiblePredecessor.id < this.n.id)) {
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
        //if (!TcpUtils.IsAlive(predecessor.address)) {
        //    predecessor = null;
        //}
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

