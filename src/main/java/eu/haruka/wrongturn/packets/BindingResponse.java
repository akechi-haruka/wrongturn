package eu.haruka.wrongturn.packets;

import eu.haruka.wrongturn.TurnPacket;
import eu.haruka.wrongturn.attributes.TurnAttribute;

import java.io.DataInputStream;
import java.net.InetSocketAddress;

public class BindingResponse extends TurnPacket {

    public BindingResponse(byte[] transactionId, TurnAttribute... attributes) {
        super(transactionId, attributes);
    }

    @Override
    public void handle(InetSocketAddress sender, InetSocketAddress receiver, DataInputStream is) {
        throw new UnsupportedOperationException("Invalid packet");
    }

    @Override
    public TurnPacket getNewErrorPacket() {
        return null;
    }

}
