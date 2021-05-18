package peer;

public class ChordHelper implements Runnable{
    private Chord chord;

    public ChordHelper(Chord chord)
    {
        this.chord = chord;
    }

    @Override
    public void run() {
        chord.Stabilize();
        chord.FixFingers();
        chord.CheckPredecessor();
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
