package eu.haruka.wrongturn.objects;

public enum AddressFamily {

    IPv4((byte) 0x01),
    IPv6((byte) 0x02),
    ;

    private final byte id;

    AddressFamily(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static AddressFamily byId(byte id) {
        for (AddressFamily a : values()) {
            if (a.id == id) {
                return a;
            }
        }
        return null;
    }

}
