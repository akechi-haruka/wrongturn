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

public class XorMappedAddress extends TurnAttribute {

    private AddressFamily addressFamily;
    private int port;
    private InetAddress ip;

    public XorMappedAddress() {
    }

    public XorMappedAddress(InetSocketAddress addr) {
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
        byte[] xport = xor(is.readNBytes(2), turnPacket.getTurnCookie());
        port = (xport[0] & 0xFF) << 8 | (xport[1] & 0xFF);
        ip = switch (addressFamily) {
            case IPv4 -> Inet4Address.getByAddress(xor(is.readNBytes(4), turnPacket.getTurnCookie()));
            case IPv6 -> Inet6Address.getByAddress(xor(is.readNBytes(16), turnPacket.getStunTransactionId()));
            case null, default -> throw new IOException("Unknown address family: " + addressFamilyId);
        };
    }

    private byte[] xor(byte[] b1, byte[] b2) {
        byte[] bo = new byte[Math.min(b1.length, b2.length)];
        for (int i = 0; i < bo.length; i++) {
            bo[i] = (byte) (b1[i] ^ b2[i]);
        }
        return bo;
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeByte(0);
        os.writeByte(addressFamily.getId());
        byte[] xport = new byte[2];
        xport[0] = (byte) (port >> 8);
        xport[1] = (byte) port;
        os.write(xor(xport, turnPacket.getTurnCookie()));
        byte[] ipbytes = ip.getAddress();
        os.write(xor(ipbytes, turnPacket.getStunTransactionId()));
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

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(ip, port);
    }
}
