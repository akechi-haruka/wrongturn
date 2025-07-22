package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnLogBack;
import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public abstract class TurnAttribute {

    private static HashMap<Short, Class<? extends TurnAttribute>> attrs;
    private static HashMap<Class<? extends TurnAttribute>, Short> attrs_reverse;

    static {
        attrs = new HashMap<>();
        attrs_reverse = new HashMap<>();
        add((short) 0x0001, MappedAddress.class);
        add((short) 0x0002, ResponseAddress.class);
        add((short) 0x0003, ChangeRequest.class);
        add((short) 0x0004, SourceAddress.class);
        add((short) 0x0005, ChangedAddress.class);
        add((short) 0x0006, Username.class);
        add((short) 0x0007, Password.class);
        add((short) 0x0008, MessageIntegrity.class);
        add((short) 0x0009, ErrorCode.class);
        add((short) 0x000a, UnknownAttributes.class);
        add((short) 0x000b, ReflectedFrom.class);
        add((short) 0x000c, ChannelNumber.class);
        add((short) 0x000d, Lifetime.class);
        add((short) 0x0012, XorPeerAddress.class);
        //add((short) 0x0013, Data.class);
        add((short) 0x0014, Realm.class);
        //add((short) 0x0015, Nonce.class);
        add((short) 0x0016, XorRelayedAddress.class);
        //add((short) 0x0017, RequestedAddressFamily.class);
        //add((short) 0x0018, EvenPort.class);
        add((short) 0x0019, RequestedTransport.class);
        //add((short) 0x001a, DontFragment.class);
        add((short) 0x0020, XorMappedAddress.class);
        //add((short) 0x0022, ReservationToken.class);
        //add((short) 0x0026, Padding.class);
        add((short) 0x0027, ResponsePort.class);
        //add((short) 0x8000, AdditionalAddressFamily.class);
        //add((short) 0x8001, AddressErrorCode.class);
        //add((short) 0x8004, ICMP.class);
        add((short) 0x8022, Software.class);
        add((short) 0x8023, AlternateServer.class);
        //add((short) 0x8028, Fingerprint.class);
        add((short) 0x802b, ResponseOrigin.class);
        add((short) 0x802c, OtherAddress.class);
    }

    private static void add(short id, Class<? extends TurnAttribute> attr) {
        attrs.put(id, attr);
        attrs_reverse.put(attr, id);
    }


    public static TurnAttribute find(short type) throws ReflectiveOperationException {
        Class<? extends TurnAttribute> attr = attrs.get(type);
        if (attr == null) {
            return null;
        }
        return attr.getConstructor().newInstance();
    }

    public static short find(Class<? extends TurnAttribute> attr) {
        return attrs_reverse.get(attr);
    }

    protected TurnLogBack log;

    public void initialize(TurnLogBack log) {
        this.log = log;
    }

    protected String readNullTerminatedString(InputStream is, int len) throws IOException {
        byte[] arr = is.readNBytes(len);
        int i;
        for (i = 0; i < arr.length && arr[i] != 0; i++) {
        }
        return new String(arr, 0, i, StandardCharsets.US_ASCII);
    }

    protected final byte[] writeString(String str) {
        return str.getBytes(StandardCharsets.US_ASCII);
    }


    protected final int skipUntilBoundary(DataInputStream is, int pos, int bound) throws IOException {
        int count = 0;
        while (pos % bound != 0) {
            is.skipNBytes(1);
            pos++;
            count++;
        }
        return count;
    }

    protected final int padUntilBoundary(DataOutputStream os, int pos, int bound) throws IOException {
        int count = 0;
        while (pos % bound != 0) {
            os.writeByte(0);
            pos++;
            count++;
        }
        return count;
    }

    protected final int getPaddingUntilBoundary(int pos, int bound) {
        int count = 0;
        while (pos % bound != 0) {
            pos++;
            count++;
        }
        return count;
    }

    public abstract void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException;

    public abstract void write(TurnPacket turnPacket, DataOutputStream os) throws IOException;

    public abstract short getLength();

}
