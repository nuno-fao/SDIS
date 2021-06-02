package peer;

public class ChordHelper implements Runnable {
    private Chord chord;

    public ChordHelper(Chord chord) {
        this.chord = chord;
    }

    static int s = 0;

    @Override
    public void run() {
        this.chord.Stabilize();
        this.chord.FixFingers();
        this.chord.CheckPredecessor();
        this.chord.requestSuccessorSuccessor();
    }

}
