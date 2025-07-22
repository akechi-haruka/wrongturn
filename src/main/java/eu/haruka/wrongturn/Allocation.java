package eu.haruka.wrongturn;

import eu.haruka.wrongturn.objects.ProtocolNumber;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;

/**
 * All TURN operations revolve around allocations, and all TURN messages are associated with either a single or dual allocation.
 * <p>
 * An allocation conceptually consists of the following state data:
 * the relayed transport address or addresses;
 * the 5-tuple: (client's IP address, client's port, server IP address, server port, and transport protocol);
 * the authentication information;the time-to-expiry for each relayed transport address;
 * a list of permissions for each relayed transport address;
 * a list of channel-to-peer bindings for each relayed transport address.
 * <p>
 * The relayed transport address is the transport address allocated by the server for communicating with peers, while the 5-tuple describes the communication path between the client and the server.
 */
public class Allocation {

    private TurnServer turn;

    private final InetSocketAddress client;
    private final InetSocketAddress server;
    private final ProtocolNumber protocol;
    private final String username;
    private final String realm;

    private DatagramSocket relay;
    private InetSocketAddress relay_addr;
    private Thread relayThread;
    private long expiry;
    private HashMap<Short, InetSocketAddress> channels = new HashMap<>();
    private long bytes_total = 0;

    Allocation(TurnServer turn, InetSocketAddress client, InetSocketAddress server, ProtocolNumber protocol, long expiry, String username, String realm) {
        this.turn = turn;
        this.client = client;
        this.server = server;
        this.protocol = protocol;
        this.expiry = expiry;
        this.username = username;
        this.realm = realm;
    }

    /**
     * Returns the client address (sender) for this allocation.
     */
    public InetSocketAddress getClient() {
        return client;
    }

    /**
     * Returns the server address (receiver) for this allocation. This is the socket as defined by the configuration.
     */
    public InetSocketAddress getServer() {
        return server;
    }

    /**
     * Returns the allocated server-sided relay address (if any, otherwise null) for this allocation.
     */
    public InetSocketAddress getRelay() {
        return relay != null ? relay_addr : null;
    }

    /**
     * Returns the transport protocol for this allocation.
     */
    public ProtocolNumber getProtocol() {
        return protocol;
    }

    /**
     * Checks equality to a 5-tuple.
     */
    public boolean equals(SocketAddress client, SocketAddress server, ProtocolNumber protocol) {
        return this.client.equals(client) && (this.server.equals(server) || server.equals(this.relay_addr)) && this.protocol.equals(protocol);
    }

    private DatagramSocket getSocket(InetAddress bind_addr) throws SocketException {
        int min = turn.getConfig().allocation_min_port;
        int max = turn.getConfig().allocation_max_port;
        for (int port = min; port <= max; port++) {
            try {
                return new DatagramSocket(port, bind_addr);
            } catch (BindException ignored) {
            }
        }
        throw new SocketException("No free ports in range " + min + "-" + max);
    }

    /**
     * Creates the allocation on the server side, a relay socket and a listening thread.
     * @throws IOException if there is an error creating the socket.
     * @throws IllegalStateException if createAllocation was already called.
     */
    public void createAllocation() throws IOException {
        turn.log().info("Creating allocation for " + this);
        if (relay != null) {
            throw new IllegalStateException("allocation already exists");
        }
        InetAddress bind_addr = InetAddress.getByName(turn.getConfig().bind_addr);
        relay = getSocket(turn.getConfig().bind_to_all ? null : bind_addr);
        relay_addr = new InetSocketAddress(bind_addr, relay.getLocalPort());
        turn.log().info("Allocation relay: " + relay.getLocalSocketAddress());
        relayThread = new Thread(this::allocationThread, "STUN/TURN " + relay.getLocalSocketAddress());
        relayThread.start();
        turn.onAllocate(this);
    }

    private void allocationThread() {
        byte[] buf = new byte[10240];
        turn.log().info("Relay up for " + this);
        while (true) {
            try {
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                relay.receive(dp);
                try (DataInputStream is = new DataInputStream(new ByteArrayInputStream(dp.getData()))) {
                    short messageType = is.readShort();
                    if (messageType >= 0x4000 && messageType <= 0x4FFF) {
                        short len = is.readShort();
                        byte[] data = is.readNBytes(len);
                        turn.handleRelay(dp.getSocketAddress(), relay_addr, messageType, data);
                        continue;
                    }
                }
            } catch (SocketException ignored) {
                break;
            } catch (Exception ex) {
                turn.log().exception(ex, "Error in relay for " + this);
                break;
            }
        }
        turn.log().info("Relay DOWN for " + this);
    }

    /**
     * Frees the allocation, the relay socket and stops the listening thread. No-op on duplicate calls.
     */
    public void freeAllocation() {
        if (relay != null) {
            turn.log().info("Freeing allocation for " + this);
            try {
                relay.close();
            } catch (Exception ignored) {
            }
            turn.onFree(this);
            expiry = 0;
            relayThread = null;
            relay_addr = null;
            relay = null;
        }
    }

    /**
     * Returns true if this alocation has reached it's expiration time.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiry;
    }

    /**
     * Sets the timestamp in unix milliseconds when this allocation expires.
     */
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    /**
     * Returns the username this allocation was created with.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the realm this allocation was created with.
     */
    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return "Allocation{" +
                "client=" + client +
                ", server=" + server +
                ", protocol=" + protocol +
                ", username=" + username +
                ", expires=" + Math.max(0, (expiry - System.currentTimeMillis()) / 1000) +
                '}';
    }

    /**
     * Returns the number of channels this allocation has created.
     */
    public int getChannelCount() {
        return channels.size();
    }

    /**
     * Sets the target for a given channel ID. Do not use externally.
     */
    public void setChannel(short channel, InetSocketAddress peer) {
        channels.put(channel, peer);
    }

    /**
     * Returns the target peer for a given channel ID.
     * @param channel The channel ID.
     * @return the peer for the given channel or null.
     */
    public InetSocketAddress getChannelPeer(short channel) {
        return channels.get(channel);
    }

    void addRelayedBytes(int length) {
        bytes_total += length;
    }

    /**
     * Returns the number of bytes this allocation has relayed (excluding STUN header).
     */
    public long getRelayedBytes() {
        return bytes_total;
    }
}
