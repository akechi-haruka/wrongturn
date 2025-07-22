package eu.haruka.wrongturn.objects;

public enum ProtocolNumber {

    UDP((byte) 0x11),
    ;

    private final byte id;

    ProtocolNumber(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static ProtocolNumber byId(byte id) {
        for (ProtocolNumber a : values()) {
            if (a.id == id) {
                return a;
            }
        }
        return null;
    }

}
