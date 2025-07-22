package eu.haruka.wrongturn;

import java.net.InetSocketAddress;

public interface TurnExternalAuth {
    boolean auth(InetSocketAddress sender, InetSocketAddress receiver, String username, String realm, String password) throws Exception;
}
