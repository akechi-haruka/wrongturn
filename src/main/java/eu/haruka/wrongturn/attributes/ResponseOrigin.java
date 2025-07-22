package eu.haruka.wrongturn.attributes;

import java.net.InetSocketAddress;

public class ResponseOrigin extends MappedAddress {
    public ResponseOrigin(InetSocketAddress receiver) {
        super(receiver);
    }
}
