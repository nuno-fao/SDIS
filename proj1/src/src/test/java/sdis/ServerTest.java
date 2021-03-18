package sdis;

import org.junit.Test;

import java.rmi.RemoteException;

import static org.junit.Assert.*;

public class ServerTest {

    @Test
    public void main1() {
        try {
            String[] s = {"java", "accesspoint", "1.1.0", "1"};
            Server.main(s);

            Server server = new Server("accesspoint", "1.1.0", 1);
            assertEquals("accesspoint",server.getAccessPoint());
            assertEquals("1.1.0",server.getVersion());
            assertEquals(1,server.getPeerId());
        }
        catch (Exception e){
            fail();
        }
    }
    @Test
    public void main2() {
        try {
            String[] s = {"java", "accesspoint", "1"};
            Server.main(s);
            fail();
        }
        catch (Exception e){
        }
    }

    @Test
    public void main3() {
        try {
            String[] s = {"java", "accesspoint", "1.1.0", "a1"};
            Server.main(s);
            fail();
        }
        catch (Exception e){
        }
    }
}