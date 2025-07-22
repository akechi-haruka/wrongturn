package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Realm extends TurnAttribute {

    private String realm;

    public Realm() {
    }

    public Realm(String realm) {
        this.realm = realm;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        realm = readNullTerminatedString(is, len);
        skipUntilBoundary(is, len, 4);
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        byte[] str = writeString(realm);
        os.write(str);
        int pad = padUntilBoundary(os, str.length, 4);
    }

    @Override
    public short getLength() {
        byte[] str = writeString(realm);
        return (short) (str.length + getPaddingUntilBoundary(str.length, 4));
    }

    public String getRealm() {
        return realm;
    }
}
