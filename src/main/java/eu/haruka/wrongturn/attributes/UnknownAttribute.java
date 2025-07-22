package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class UnknownAttribute extends TurnAttribute {

    private short id;

    public UnknownAttribute(short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public short getLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        throw new UnsupportedEncodingException();
    }
}
