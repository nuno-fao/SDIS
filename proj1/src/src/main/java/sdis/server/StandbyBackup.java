package sdis.server;

import java.util.Objects;

public class StandbyBackup {
    private String fileId;
    private int chunkNo;
    private boolean cancel;

    public StandbyBackup(String fileId, int chunkNo, boolean cancel) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.cancel=cancel;
    }

    public void cancel(){
        cancel=true;
    }

    public boolean isCanceled() {
        return cancel;
    }

    public boolean isEqual(String fileId,int chunkNo){
        return (this.fileId.equals(fileId) && this.chunkNo==chunkNo);
    }
}
