package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChangeRequest extends TurnAttribute {

    private static final short OFFSET_CHANGE_PORT = 1;
    private static final short OFFSET_CHANGE_IP = 2;

    private int flag;

    public ChangeRequest() {
    }

    public ChangeRequest(int flag) {
        this.flag = flag;
    }

    public ChangeRequest(boolean changeIP, boolean changePort) {
        flag = 0;
        if (changeIP) {
            flag |= (1 << OFFSET_CHANGE_IP);
        }
        if (changePort) {
            flag |= (1 << OFFSET_CHANGE_PORT);
        }
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        flag = is.readInt();
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeInt(flag);
    }

    @Override
    public short getLength() {
        return 4;
    }

    public boolean isChangeIP() {
        return ((flag >> OFFSET_CHANGE_IP) & 1) != 0;
    }

    public boolean isChangePort() {
        return ((flag >> OFFSET_CHANGE_PORT) & 1) != 0;
    }
}
