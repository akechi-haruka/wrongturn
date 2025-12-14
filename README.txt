WrongTurn
Minimal STUN/TURN server written in Java that allows nonsensical configurations.
(c) 2026-2026 Haruka
Licensed under the SSPL.

--------------------------------------

Please note that this was implemented for a very specific purpose (that's why the entire "hacks" block exists in the config), and compatability is not guranteed.

Based on:
RFC 3489
RFC 5389
RFC 8656

How to use:

public class TurnModule implements TurnLogBack, TurnExternalAuth, TurnAllocationCallback {

    private TurnConfig config;
    private TurnServer server;

    public void startTurnServer() throws IOException {
        config = ... (load from serialization, json, ...)
        server = new TurnServer(config);
        server.setLogFunctions(this);
        server.setExternalAuthentication(this);
        server.setAllocationCallback(this);
        server.start();
    }

    [...]

}