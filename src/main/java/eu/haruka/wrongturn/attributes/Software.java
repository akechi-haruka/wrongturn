package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Software extends TurnAttribute {

    private String software;

    public Software() {
    }

    public Software(String software) {
        this.software = software;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        software = readNullTerminatedString(is, len);
        skipUntilBoundary(is, len, 4);
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        byte[] str = writeString(software);
        os.write(str);
        int pad = padUntilBoundary(os, str.length, 4);
    }

    @Override
    public short getLength() {
        byte[] str = writeString(software);
        return (short) (str.length + getPaddingUntilBoundary(str.length, 4));
    }

    public String getSoftware() {
        return software;
    }
}
