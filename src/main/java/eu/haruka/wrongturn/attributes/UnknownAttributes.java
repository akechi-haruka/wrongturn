package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class UnknownAttributes extends TurnAttribute {

    private ArrayList<Short> unknownAttributes;

    public UnknownAttributes() {
        unknownAttributes = new ArrayList<>();
    }

    public UnknownAttributes(short... attributes) {
        this();
        for (short s : attributes) {
            unknownAttributes.add(s);
        }
    }

    public UnknownAttributes(Iterable<Short> attributes) {
        this();
        for (short s : attributes) {
            unknownAttributes.add(s);
        }
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        unknownAttributes = new ArrayList<>();
        for (int i = 0; i < len; i += 2) {
            unknownAttributes.add(is.readShort());
        }
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        for (short s : unknownAttributes) {
            os.write(s);
        }
        if (unknownAttributes.size() % 2 != 0) {
            os.write(unknownAttributes.getFirst());
        }
    }

    @Override
    public short getLength() {
        return (short) (unknownAttributes.size() * 2 + (unknownAttributes.size() % 2 != 0 ? 2 : 0));
    }

    public Collection<Short> getUnknownAttributes() {
        return unknownAttributes;
    }
}
