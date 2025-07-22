package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;
import eu.haruka.wrongturn.objects.AddressFamily;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class MappedAddress extends TurnAttribute {

    private AddressFamily addressFamily;
    private int port;
    private InetAddress ip;

    public MappedAddress() {
    }

    public MappedAddress(InetSocketAddress addr) {
        ip = addr.getAddress();
        port = addr.getPort();
        if (addr.getAddress() instanceof Inet4Address) {
            addressFamily = AddressFamily.IPv4;
        } else if (addr.getAddress() instanceof Inet6Address) {
            addressFamily = AddressFamily.IPv6;
        } else {
            throw new IllegalArgumentException("Unknown InetSocketAddress type " + addr.getClass());
        }
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        is.readByte(); // padding
        byte addressFamilyId = is.readByte();
        addressFamily = AddressFamily.byId(addressFamilyId);
        port = is.readUnsignedShort();
        ip = switch (addressFamily) {
            case IPv4 -> Inet4Address.getByAddress(is.readNBytes(4));
            case IPv6 -> Inet6Address.getByAddress(is.readNBytes(16));
            case null, default -> throw new IOException("Unknown address family: " + addressFamilyId);
        };
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeByte(0);
        os.writeByte(addressFamily.getId());
        os.writeShort(port);
        byte[] ipbytes = ip.getAddress();
        os.write(ipbytes);
    }

    @Override
    public short getLength() {
        return (short) (1 + 1 + 2 + ip.getAddress().length);
    }

    public AddressFamily getAddressFamily() {
        return addressFamily;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getIP() {
        return ip;
    }
}
