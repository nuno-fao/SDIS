package peer;

import peer.sockets.TcpUtils;

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
        if (fingerTable.get(0).id >= id && id > n.id) {
            return fingerTable.get(0);
        } else {
            Node nl;
            for (int i = fingerTable.size() - 1; i >= 0; i--) {
                if (n.id < fingerTable.get(i).id && fingerTable.get(i).id < id) {
                    nl = fingerTable.get(i);
                    break;
                }
                if (fingerTable.get(i).id == id) {
                    return fingerTable.get(i);
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
        this.successor = n;
    }

    public void Join(Node nl) {
        predecessor = null;
        successor = FindSuccessor(n.id);
    }

    public void Stabilize() {
        Node x = FindPredecessor(successor);
        if (x.id > n.id && x.id < successor.id) {
            successor = x;
        }
        //todo notify
    }

    public void Notify(Node possiblePredecessor) {
        if (predecessor == null || (possiblePredecessor.id > predecessor.id && possiblePredecessor.id < n.id)) {
            predecessor = possiblePredecessor;
        }
    }

    public void FixFingers() {
        next++;
        if (next > fingerTable.size() - 1) {
            next = 0;
        }
        fingerTable.set(next, FindSuccessor(n.id + (int) Math.pow(2, next)));
    }

    public void CheckPredecessor() {
        if (!TcpUtils.IsAlive(predecessor.address)) {
            predecessor = null;
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
}

