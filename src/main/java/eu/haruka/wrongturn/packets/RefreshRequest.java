package eu.haruka.wrongturn.packets;


import eu.haruka.wrongturn.TurnPacket;
import eu.haruka.wrongturn.attributes.*;
import eu.haruka.wrongturn.objects.ProtocolNumber;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class RefreshRequest extends TurnPacket {
    @Override
    public void handle(InetSocketAddress sender, InetSocketAddress receiver, DataInputStream is) throws IOException {
        if (!checkGenericAttributes(sender)) {
            return;
        }

        if (!hasAttributes(Username.class, Realm.class, Lifetime.class)) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_BAD_REQUEST, "Required attribute missing");
            return;
        }

        ProtocolNumber protocol = ProtocolNumber.UDP;
        RequestedTransport rattr = getAttribute(RequestedTransport.class);
        if (rattr != null) {
            protocol = rattr.getProtocol();
        }

        int lifetime = 0;

        Allocation existing = server.getAllocation(sender, receiver, protocol);
        if (existing != null) {

            if (server.getAllocationCount() > server.getConfig().maximum_allocations) {
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_ALLOCATION_QUOTA_REACHED, "No free slots, try again later");
                return;
            }
            Lifetime lattr = getAttribute(Lifetime.class);
            if (lattr != null) {
                lifetime = lattr.getLifetime();
            } else {
                lifetime = server.getConfig().default_lifetime;
            }

            if (lifetime > 0) {
                lifetime = Math.min(lifetime, server.getConfig().max_lifetime);
                lifetime = Math.max(lifetime, server.getConfig().hacks.min_lifetime);
                server.log().debug("Updating lifetime of " + existing + " to " + lifetime);
                existing.setExpiry(System.currentTimeMillis() + lifetime * 1000L);
            } else if (!server.getConfig().hacks.delete_retains_allocation_active) {
                existing.setExpiry(0);
                existing.freeAllocation();
            }

        } else if (!server.getConfig().hacks.delete_on_invalid_allocation_returns_ok) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_ALLOCATION_MISMATCH, "Allocation not found");
            return;
        }

        RefreshResponse resp = new RefreshResponse(transactionId,
                new Lifetime(lifetime)
        );
        ResponseAddress ra = getAttribute(ResponseAddress.class);
        if (ra != null) {
            server.send(new InetSocketAddress(ra.getIP(), ra.getPort()), resp);
        } else {
            server.send(sender, resp);
        }
    }

    @Override
    public TurnPacket getNewErrorPacket() {
        return new RefreshErrorResponse();
    }
}
