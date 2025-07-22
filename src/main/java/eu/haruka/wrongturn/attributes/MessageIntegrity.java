package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageIntegrity extends TurnAttribute {

    private byte[] hmac;

    public MessageIntegrity() {
    }

    public MessageIntegrity(byte[] hmac) {
        this.hmac = hmac;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        hmac = is.readNBytes(len);
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.write(hmac);
        padUntilBoundary(os, hmac.length, 64);
    }

    @Override
    public short getLength() {
        return (short) (hmac.length + getPaddingUntilBoundary(hmac.length, 64));
    }

    public byte[] getHMAC() {
        return hmac;
    }
}
