package eu.haruka.wrongturn.attributes;


import eu.haruka.wrongturn.TurnPacket;
import eu.haruka.wrongturn.objects.ProtocolNumber;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RequestedTransport extends TurnAttribute {

    private ProtocolNumber protocol;

    public RequestedTransport() {
    }

    public RequestedTransport(ProtocolNumber protocol) {
        this.protocol = protocol;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        byte protocolbyte = is.readByte();
        protocol = ProtocolNumber.byId(protocolbyte);
        is.skipNBytes(3);
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeByte(protocol.getId());
        os.write(new byte[3]);
    }

    @Override
    public short getLength() {
        return 4;
    }

    public ProtocolNumber getProtocol() {
        return protocol;
    }
}
