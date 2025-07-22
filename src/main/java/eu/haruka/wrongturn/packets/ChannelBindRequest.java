package eu.haruka.wrongturn.packets;


import eu.haruka.wrongturn.Allocation;
import eu.haruka.wrongturn.TurnPacket;
import eu.haruka.wrongturn.attributes.*;
import eu.haruka.wrongturn.objects.ProtocolNumber;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ChannelBindRequest extends TurnPacket {
    @Override
    public void handle(InetSocketAddress sender, InetSocketAddress receiver, DataInputStream is) throws IOException {
        if (!checkGenericAttributes(sender)) {
            return;
        }

        if (!hasAttributes(Username.class, Realm.class, XorPeerAddress.class, ChannelNumber.class)) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_BAD_REQUEST, "Required attribute missing");
            return;
        }

        ProtocolNumber protocol = ProtocolNumber.UDP;
        RequestedTransport rattr = getAttribute(RequestedTransport.class);
        if (rattr != null) {
            protocol = rattr.getProtocol();
        }

        Allocation existing = server.getAllocation(sender, receiver, protocol);
        if (existing == null) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_ALLOCATION_MISMATCH, "Allocation not found");
            return;
        }

        short channel = getAttribute(ChannelNumber.class).getChannelNumber();
        if (channel < 0x4000 || channel > 0x4FFF) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_BAD_REQUEST, "Channel number invalid");
            return;
        }

        if (existing.getChannelCount() > server.getConfig().max_channels) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_INSUFFICIENT_CAPACITY, "Maximum number of channels reached");
            return;
        }

        InetSocketAddress peer = getAttribute(XorPeerAddress.class).getSocketAddress();
        server.log().info(this + " setting channel " + channel + " to peer " + peer);

        Allocation other = server.getAllocationByRelay(peer);
        if (other == null) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_BAD_REQUEST, "Specified peer does not exist");
            return;
        }

        if (existing.getChannelPeer(channel) == null) {
            server.log().info(existing + " channel " + channel + " will relay to " + other + " via " + peer);
            existing.setChannel(channel, other.getClient());
        } else {
            server.log().info("Channel relay already exists");

            int lifetime;
            Lifetime lattr = getAttribute(Lifetime.class);
            if (lattr != null) {
                lifetime = lattr.getLifetime();
            } else {
                lifetime = server.getConfig().default_lifetime;
            }
            lifetime = Math.min(lifetime, server.getConfig().max_lifetime);

            server.log().debug("Updating lifetime of " + existing + " to " + lifetime);
            existing.setExpiry(System.currentTimeMillis() + lifetime * 1000L);
        }

        ChannelBindResponse resp = new ChannelBindResponse(transactionId);
        ResponseAddress ra = getAttribute(ResponseAddress.class);
        if (ra != null) {
            server.send(new InetSocketAddress(ra.getIP(), ra.getPort()), resp);
        } else {
            server.send(sender, resp);
        }
    }

    @Override
    public TurnPacket getNewErrorPacket() {
        return new ChannelBindErrorResponse();
    }
}
