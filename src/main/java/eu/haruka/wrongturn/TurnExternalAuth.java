package eu.haruka.wrongturn;

import java.net.InetSocketAddress;

/**
 * Interface called to externally authenticate an user.
 */
public interface TurnExternalAuth {

    /**
     * Called when a user should be authenticated (and no static login data was found).
     * @param sender The (remote) sending address.
     * @param receiver The (local) receiving address.
     * @param username The given username.
     * @param realm The given realm, may be blank or null.
     * @param password The given password, may be blank or null.
     * @return true if the user is allowed, false if not.
     * @throws Exception if any exception occurs.
     */
    boolean auth(InetSocketAddress sender, InetSocketAddress receiver, String username, String realm, String password) throws Exception;
}
