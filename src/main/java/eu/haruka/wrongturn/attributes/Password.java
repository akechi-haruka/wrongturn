package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Password extends TurnAttribute {

    private String password;

    public Password() {
    }

    public Password(String password) {
        this.password = password;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        password = readNullTerminatedString(is, len);
        skipUntilBoundary(is, len, 4);
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        byte[] str = writeString(password);
        os.write(str);
        padUntilBoundary(os, str.length, 4);
    }

    @Override
    public short getLength() {
        byte[] str = writeString(password);
        return (short) (str.length + getPaddingUntilBoundary(str.length, 4));
    }

    public String getPassword() {
        return password;
    }
}
