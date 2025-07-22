package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Lifetime extends TurnAttribute {

    private int lifetime;

    public Lifetime() {
    }

    public Lifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        lifetime = is.readInt();
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeInt(lifetime);
    }

    @Override
    public short getLength() {
        return 4;
    }

    public int getLifetime() {
        return lifetime;
    }
}
