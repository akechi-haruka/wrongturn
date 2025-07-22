package eu.haruka.wrongturn.packets;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.net.InetSocketAddress;

public class AllocateErrorResponse extends TurnPacket {
    @Override
    public void handle(InetSocketAddress sender, InetSocketAddress receiver, DataInputStream is) {
        throw new UnsupportedOperationException("Invalid packet");
    }

    @Override
    public TurnPacket getNewErrorPacket() {
        return null;
    }
}
