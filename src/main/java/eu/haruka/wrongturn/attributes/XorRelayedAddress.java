package eu.haruka.wrongturn.attributes;

import java.net.InetSocketAddress;

public class XorRelayedAddress extends XorMappedAddress {
    public XorRelayedAddress(InetSocketAddress allocation) {
        super(allocation);
    }
}
