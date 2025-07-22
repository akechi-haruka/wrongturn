package eu.haruka.wrongturn.packets;

import eu.haruka.wrongturn.TurnServer;
import eu.haruka.wrongturn.objects.ProtocolNumber;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;

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

    public Allocation(TurnServer turn, InetSocketAddress client, InetSocketAddress server, ProtocolNumber protocol, long expiry, String username, String realm) {
        this.turn = turn;
        this.client = client;
        this.server = server;
        this.protocol = protocol;
        this.expiry = expiry;
        this.username = username;
        this.realm = realm;
    }

    public InetSocketAddress getClient() {
        return client;
    }

    public InetSocketAddress getServer() {
        return server;
    }

    public InetSocketAddress getRelay() {
        return relay != null ? relay_addr : null;
    }

    public ProtocolNumber getProtocol() {
        return protocol;
    }

    public boolean equals(SocketAddress client, SocketAddress server, ProtocolNumber protocol) {
        return this.client.equals(client) && (this.server.equals(server) || server.equals(this.relay_addr)) && this.protocol.equals(protocol);
    }

    private DatagramSocket getSocket(InetAddress bind_addr) throws SocketException {
        for (int port = turn.getConfig().allocation_min_port; port <= turn.getConfig().allocation_max_port; port++) {
            try {
                return new DatagramSocket(port, bind_addr);
            } catch (BindException ignored) {
                turn.log().warning(ignored.toString());
            }
        }
        throw new SocketException("No free ports");
    }

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

    public boolean isExpired() {
        return System.currentTimeMillis() > expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public String getUsername() {
        return username;
    }

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

    public int getChannelCount() {
        return channels.size();
    }

    public void setChannel(short channel, InetSocketAddress peer) {
        channels.put(channel, peer);
    }

    public InetSocketAddress getChannelPeer(short channel) {
        return channels.get(channel);
    }

    public void addRelayedBytes(int length) {
        bytes_total += length;
    }

    public long getRelayedBytes() {
        return bytes_total;
    }
}
