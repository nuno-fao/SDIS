package sdis;

import org.junit.Test;
import sdis.server.Address;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

public class ServerTest {

    @Test
    public void main1() {
        try {
            String[] s = {"1.1.0", "1","accesspoint", "224.0.0.1","4003", "224.0.0.2","4003", "224.0.0.3","4003"};
            Server.main(s);

            Server server = new Server("1.1.0", 1,"accesspoint",new Address("224.0.0.1",4003),new Address("224.0.0.2",4003),new Address("224.0.0.3",4003));
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
            String[] s = {"accesspoint", "1","224.0.0.1","4003", "224.0.0.2","4003", "224.0.0.3","4003"};
            Server.main(s);
            fail();
        }
        catch (Exception e){
        }
    }

    @Test
    public void main3() {
        try {
            String[] s = { "1.1.0", "a1","accesspoint","224.0.0.1","4003", "224.0.0.2","4003", "224.0.0.3","4003"};
            Server.main(s);
            fail();
        }
        catch (Exception e){
        }
    }

    @Test
    public void main4() {
        try {
            String[] s = {"1.1.0", "1","accesspoint","4003", "224.0.0.2","4003", "224.0.0.3","4003"};
            Server.main(s);
            fail();
        }
        catch (Exception e){
        }
    }
}