package peer;

/**
 * Used as an holder for the input address and port
 */
public class Address{
    public String address;
    public int port;
    public Address(String a, int p){
        address = a;
        port = p;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof Address)) return false;

        Address rhs = (Address) obj;

        return this.port == rhs.port && this.address.equals(rhs.address);
    }
}
