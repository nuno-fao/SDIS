package peer;

public class ChordHelper implements Runnable{
    private Chord chord;

    public ChordHelper(Chord chord)
    {
        this.chord = chord;
    }

    @Override
    public void run() {
        while (true)
        {
            chord.Stabilize();
            chord.FixFingers();
            chord.CheckPredecessor();
            chord.requestSuccessorSuccessor();
            try
            {
                Thread.sleep(1000); 
            }
            catch (InterruptedException e)
            {
                System.out.println("Couldn't sleep!");
                e.printStackTrace();
            }
        }
    }
    
}
