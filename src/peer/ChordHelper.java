package peer;

public class ChordHelper implements Runnable {
    private Chord chord;

    public ChordHelper(Chord chord) {
        this.chord = chord;
    }

    static int s = 0;

    @Override
    public void run() {
        System.out.println("1: "+this.chord.getSuccessor().id);
        System.out.println(this.chord.getPredecessor());
        System.out.println(s++);
        this.chord.Stabilize();
        this.chord.FixFingers();
        this.chord.CheckPredecessor();
        this.chord.requestSuccessorSuccessor();
    }

}
