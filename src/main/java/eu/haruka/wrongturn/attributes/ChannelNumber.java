package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChannelNumber extends TurnAttribute {

    private short channelNumber;

    public ChannelNumber() {
    }

    public ChannelNumber(short channelNumber) {
        this.channelNumber = channelNumber;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        channelNumber = is.readShort();
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeShort(channelNumber);
        os.writeShort(0); // reserved
    }

    @Override
    public short getLength() {
        return 2 + 2;
    }

    public short getChannelNumber() {
        return channelNumber;
    }
}
