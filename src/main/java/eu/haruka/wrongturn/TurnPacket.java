package eu.haruka.wrongturn;

import eu.haruka.wrongturn.attributes.*;
import eu.haruka.wrongturn.packets.*;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

public abstract class TurnPacket {

    private static HashMap<Short, Class<? extends TurnPacket>> packets;
    private static HashMap<Class<? extends TurnPacket>, Short> packets_reverse;

    static {
        packets = new HashMap<>();
        packets_reverse = new HashMap<>();
        add((short) 0x0001, BindingRequest.class);
        add((short) 0x0101, BindingResponse.class);
        add((short) 0x0111, BindingErrorResponse.class);
        /*add((short)0x0002, SharedSecretRequest.class);
        add((short)0x0102, SharedSecretResponse.class);
        add((short)0x0112, SharedSecretErrorResponse.class);*/
        add((short) 0x0003, AllocateRequest.class);
        add((short) 0x0103, AllocateResponse.class);
        add((short) 0x0113, AllocateErrorResponse.class);
        add((short) 0x0004, RefreshRequest.class);
        add((short) 0x0104, RefreshResponse.class);
        add((short) 0x0114, RefreshErrorResponse.class);
        /*add((short)0x0006, SendRequest.class);
        add((short)0x0106, SendResponse.class);
        add((short)0x0116, SendErrorResponse.class);
        add((short)0x0007, DataRequest.class);
        add((short)0x0107, DataResponse.class);
        add((short)0x0117, DataErrorResponse.class);
        add((short)0x0008, CreatePermissionRequest.class);
        add((short)0x0108, CreatePermissionResponse.class);
        add((short)0x0118, CreatePermissionErrorResponse.class);*/
        add((short) 0x0009, ChannelBindRequest.class);
        add((short) 0x0109, ChannelBindResponse.class);
        add((short) 0x0119, ChannelBindErrorResponse.class);
    }

    private static void add(short id, Class<? extends TurnPacket> p) {
        packets.put(id, p);
        packets_reverse.put(p, id);
    }

    public static TurnPacket find(short messageType) throws ReflectiveOperationException {
        Class<? extends TurnPacket> packet = packets.get(messageType);
        if (packet == null) {
            return null;
        }
        return packet.getConstructor().newInstance();
    }

    protected TurnLogBack log;
    protected TurnServer server;
    protected List<TurnAttribute> attributes;
    protected byte[] transactionId;
    private byte[] rawData;

    public TurnPacket() {
        attributes = new ArrayList<>();
    }

    public TurnPacket(byte[] transactionId, TurnAttribute... attributes) {
        this();
        this.transactionId = transactionId;
        Collections.addAll(this.attributes, attributes);
        this.attributes.add(new Software("GMG-MODULE-WRONGTURN"));
    }

    public void initialize(TurnLogBack log, TurnServer turnServer, byte[] packet) {
        this.log = log;
        this.server = turnServer;
        this.rawData = packet;
    }

    public void addAttribute(TurnAttribute at) {
        this.attributes.add(at);
    }

    public void process(DataInputStream is) throws IOException, ReflectiveOperationException {
        short len = is.readShort();
        transactionId = is.readNBytes(16);

        for (int remaining = len; remaining > 0; ) {
            short type = is.readShort();
            short len_attr = is.readShort();
            TurnAttribute attr = TurnAttribute.find(type);
            if (attr != null) {
                attr.initialize(log);
                log.debug("Read attribute: " + attr + "(len=" + len_attr + ")");
                attr.read(this, is, len_attr);
                attributes.add(attr);
                remaining -= (2 + 2 + attr.getLength());
            } else {
                log.warning("Unknown attribute: " + type + " (" + len_attr + " bytes)");
                attributes.add(new UnknownAttribute(type));
                is.skipBytes(len_attr);
                remaining -= (2 + 2 + len_attr);
            }
        }

    }

    public byte[] write() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeShort(packets_reverse.get(getClass()));
        dos.writeShort(0); // size placeholder
        dos.write(transactionId);
        short plen = 0;
        for (TurnAttribute a : attributes) {
            short attr = TurnAttribute.find(a.getClass());
            dos.writeShort(attr);
            dos.writeShort(a.getLength());
            log.debug("Write attribute: " + a + "(len=" + a.getLength() + ")");
            a.write(this, dos);
            plen += (short) (a.getLength() + 2 + 2);
        }

        dos.flush();
        bos.flush();

        byte[] data = bos.toByteArray();
        data[2] = (byte) (plen >> 8);
        data[3] = (byte) plen;

        return data;
    }

    @SafeVarargs
    protected final boolean hasAttributes(Class<? extends TurnAttribute>... required) {
        for (TurnAttribute a : attributes) {
            if (!ArrayUtils.contains(required, a.getClass())) {
                return false;
            }
        }
        return true;
    }

    protected final <T extends TurnAttribute> T getAttribute(Class<T> attr) {
        for (TurnAttribute a : attributes) {
            if (a.getClass() == attr) {
                //noinspection unchecked
                return (T) a;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected final <T extends TurnAttribute> List<T> getAttributes(Class<T> attr) {
        ArrayList<T> list = new ArrayList<>();
        for (TurnAttribute a : attributes) {
            if (a.getClass() == attr) {
                list.add((T) a);
            }
        }
        return list;
    }

    protected final byte[] getRawDataWithoutIntegrity() {
        MessageIntegrity mi = getAttribute(MessageIntegrity.class);
        if (mi == null) {
            throw new IllegalStateException("Packet has no message integrity attribute");
        }
        int hmac_length = mi.getHMAC().length;
        byte[] raw = new byte[rawData.length - hmac_length];
        System.arraycopy(rawData, 0, raw, 0, raw.length);
        return raw;
    }

    public byte[] getStunTransactionId() {
        return transactionId;
    }

    public byte[] getTurnTransactionId() {
        byte[] arr = new byte[12];
        System.arraycopy(transactionId, 4, arr, 0, arr.length);
        return arr;
    }

    public byte[] getTurnCookie() {
        byte[] arr = new byte[4];
        System.arraycopy(transactionId, 0, arr, 0, arr.length);
        return arr;
    }

    public Collection<TurnAttribute> getAttributes() {
        return attributes;
    }

    protected final boolean checkGenericAttributes(SocketAddress sender) throws IOException {
        if (server.getConfig().require_message_integrity && !hasAttributes(MessageIntegrity.class)) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_UNAUTHORIZED, "MESSAGE-INTEGRITY is required");
            return false;
        }

        MessageIntegrity mi = getAttribute(MessageIntegrity.class);
        if (mi != null) {
            Username username = getAttribute(Username.class);
            if (username == null) {
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_MISSING_USERNAME, "MESSAGE-INTEGRITY present, but USERNAME missing");
                return false;
            }
            byte[] key = server.getKeyFor(username.getUsername());
            if (key == null) {
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_STALE_CREDENTIALS, "No secret found for given USERNAME");
                return false;
            }
            byte[] expected = mi.getHMAC();
            byte[] calculated = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, key).hmac(getRawDataWithoutIntegrity());
            if (!ArrayUtils.isEquals(expected, calculated)) {
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_INTEGRITY_CHECK_FAILURE, "Expected " + HexFormat.of().formatHex(expected) + ", got " + HexFormat.of().formatHex(calculated));
                return false;
            }
        }

        if (hasAttributes(UnknownAttribute.class)) {
            ArrayList<Short> attrs = new ArrayList<>();
            for (UnknownAttribute attr : getAttributes(UnknownAttribute.class)) {
                if (attr.getId() >= 0 && attr.getId() <= 0x7FFF) {
                    attrs.add(attr.getId());
                }
            }
            if (!attrs.isEmpty()) {
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_UNKNOWN_ATTRIBUTE, "Unknown required attributes present", new UnknownAttributes(attrs));
                return false;
            }
        }

        return true;
    }

    public abstract void handle(InetSocketAddress sender, InetSocketAddress receiver, DataInputStream is) throws IOException;

    public abstract TurnPacket getNewErrorPacket();
}
