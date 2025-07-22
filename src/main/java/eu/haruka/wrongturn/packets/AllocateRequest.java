package eu.haruka.wrongturn.packets;


import eu.haruka.wrongturn.Allocation;
import eu.haruka.wrongturn.TurnPacket;
import eu.haruka.wrongturn.attributes.*;
import eu.haruka.wrongturn.objects.ProtocolNumber;
import eu.haruka.wrongturn.objects.TurnConfig;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Objects;

public class AllocateRequest extends TurnPacket {
    @Override
    public void handle(InetSocketAddress sender, InetSocketAddress receiver, DataInputStream is) throws IOException {
        if (!checkGenericAttributes(sender)) {
            return;
        }

        if (!hasAttributes(Username.class, Realm.class, Lifetime.class, RequestedTransport.class)) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_BAD_REQUEST, "Required attribute missing");
            return;
        }

        String username = getAttribute(Username.class).getUsername();
        String realm = getAttribute(Realm.class).getRealm();
        Password password_attr = getAttribute(Password.class);
        String password = password_attr != null ? password_attr.getPassword() : null;
        boolean login_ok = false;

        for (TurnConfig.Login l : server.getConfig().static_logins) {
            if (l.username.equals(username) && l.realm.equals(realm) && Objects.equals(l.password, password)) {
                login_ok = true;
                break;
            }
        }
        if (!login_ok) {
            try {
                login_ok = server.externalAuthentication(sender, receiver, username, realm, password);
            } catch (Exception ex) {
                log.exception(ex, "Failed verifying authentication data for " + username + ", " + realm);
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_SERVER_ERROR, "Internal error while verifying authentication");
                return;
            }
        }
        if (!login_ok && !server.getConfig().allow_anonymous) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_UNAUTHORIZED, "Invalid login data");
            return;
        }

        ProtocolNumber protocol = getAttribute(RequestedTransport.class).getProtocol();
        if (protocol == null) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_UNSUPPORTED_TRANSPORT_PROTOCOL, "Specified transport protocol not supported");
            return;
        }

        int lifetime = getAttribute(Lifetime.class).getLifetime();
        if (lifetime == 0) {
            lifetime = server.getConfig().default_lifetime;
        }
        lifetime = Math.min(lifetime, server.getConfig().max_lifetime);

        InetSocketAddress allocation = null;
        Allocation existing = server.getAllocation(sender, receiver, protocol);
        if (existing != null) {
            if (!server.getConfig().hacks.allow_reallocation) {
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_ALLOCATION_MISMATCH, "Already allocated");
                return;
            }
            if (server.getConfig().hacks.delete_previous_allocation_on_reallocation) {
                existing.freeAllocation();
            } else {
                allocation = existing.getRelay();
                existing.setExpiry(System.currentTimeMillis() + lifetime * 1000L);
            }
        }

        if (server.getAllocationCount() > server.getConfig().maximum_allocations) {
            server.sendErrorForRequest(sender, this, ErrorCode.ERROR_ALLOCATION_QUOTA_REACHED, "No free slots, try again later");
            return;
        }

        if (allocation == null) {
            try {
                allocation = server.allocate(sender, receiver, protocol, lifetime, username, realm);
            } catch (SocketException e) {
                server.log().exception(e, "Failed to allocate for " + sender);
                server.sendErrorForRequest(sender, this, ErrorCode.ERROR_ALLOCATION_QUOTA_REACHED, "No free ports, try again later");
                return;
            }
        }

        AllocateResponse resp = new AllocateResponse(transactionId,
                new XorMappedAddress(sender),
                new XorRelayedAddress(allocation),
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
        return new AllocateErrorResponse();
    }
}
