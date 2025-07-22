package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ResponsePort extends TurnAttribute {

    private short port;

    public ResponsePort() {
    }

    public ResponsePort(short port) {
        this.port = port;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        port = is.readShort();
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeShort(port);
    }

    @Override
    public short getLength() {
        return 2;
    }

    public short getPort() {
        return port;
    }
}
