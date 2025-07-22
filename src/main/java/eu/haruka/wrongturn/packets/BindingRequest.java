package eu.haruka.wrongturn.packets;


import eu.haruka.wrongturn.TurnPacket;
import eu.haruka.wrongturn.attributes.*;
import eu.haruka.wrongturn.objects.TurnConfig;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class BindingRequest extends TurnPacket {

    @Override
    public void handle(InetSocketAddress sender, InetSocketAddress receiver, DataInputStream is) throws IOException {
        if (!checkGenericAttributes(sender)) {
            return;
        }

        ChangeRequest cr = getAttribute(ChangeRequest.class);
        if (cr != null) {
            if ((cr.isChangeIP() || cr.isChangePort()) && !server.getConfig().hacks.ignore_change_ip_port_flags_in_binding)
                if (server.getConfig().hacks.no_response_to_change_ip_port) {
                    return;
                }
        }

        TurnConfig.Server[] servers = server.getConfig().servers;

        BindingResponse resp = new BindingResponse(transactionId,
                new MappedAddress(sender),
                new XorMappedAddress(sender)
        );
        if (servers.length > 0) {
            resp.addAttribute(new ResponseOrigin(new InetSocketAddress(InetAddress.getByName(servers[0].ip), servers[0].port)));
        }
        if (servers.length > 1) {
            resp.addAttribute(new OtherAddress(new InetSocketAddress(InetAddress.getByName(servers[1].ip), servers[1].port)));
            resp.addAttribute(new ChangedAddress(new InetSocketAddress(InetAddress.getByName(servers[1].ip), servers[1].port)));
        }
        ResponseAddress ra = getAttribute(ResponseAddress.class);
        if (ra != null) {
            server.send(new InetSocketAddress(ra.getIP(), ra.getPort()), resp);
        } else {
            server.send(sender, resp);
        }
    }

    @Override
    public TurnPacket getNewErrorPacket() {
        return new BindingErrorResponse();
    }

}
