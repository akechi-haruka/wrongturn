package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Username extends TurnAttribute {

    private String username;

    public Username() {
    }

    public Username(String username) {
        this.username = username;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        username = readNullTerminatedString(is, len);
        skipUntilBoundary(is, len, 4);
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        byte[] str = writeString(username);
        os.write(str);
        int pad = padUntilBoundary(os, str.length, 4);
    }

    @Override
    public short getLength() {
        byte[] str = writeString(username);
        return (short) (str.length + getPaddingUntilBoundary(str.length, 4));
    }

    public String getUsername() {
        return username;
    }
}
