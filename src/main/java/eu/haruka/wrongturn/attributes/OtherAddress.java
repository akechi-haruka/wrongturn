package eu.haruka.wrongturn.attributes;

import java.net.InetSocketAddress;

public class OtherAddress extends MappedAddress {
    public OtherAddress(InetSocketAddress receiver) {
        super(receiver);
    }
}
