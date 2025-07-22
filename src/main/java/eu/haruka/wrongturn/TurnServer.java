package eu.haruka.wrongturn;

import eu.haruka.wrongturn.attributes.ErrorCode;
import eu.haruka.wrongturn.attributes.TurnAttribute;
import eu.haruka.wrongturn.objects.NullLog;
import eu.haruka.wrongturn.objects.ProtocolNumber;
import eu.haruka.wrongturn.objects.TurnConfig;
import eu.haruka.wrongturn.packets.Allocation;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurnServer extends Thread {

    private final DatagramSocket ssocket;
    private boolean running;
    private TurnLogBack log;
    private TurnConfig config;
    private ArrayList<Allocation> allocations;
    private TurnExternalAuth auth;
    private TurnAllocationCallback allocationCallback;

    public TurnServer(TurnConfig config, DatagramSocket ssocket) {
        super("TURN/STUN server");
        this.ssocket = ssocket;
        this.log = new NullLog();
        this.config = config;
        this.allocations = new ArrayList<>();
    }

    public void setLogFunctions(TurnLogBack log) {
        if (log == null) {
            throw new NullPointerException("log");
        }
        this.log = log;
    }

    public void setExternalAuthentication(TurnExternalAuth auth) {
        if (auth == null) {
            throw new NullPointerException("auth");
        }
        this.auth = auth;
    }

    public void setAllocationCallback(TurnAllocationCallback callback) {
        if (callback == null) {
            throw new NullPointerException("callback");
        }
        this.allocationCallback = callback;
    }

    @Override
    public void run() {
        running = true;
        log.info("Server started");
        byte[] udp_recv_buf = new byte[100 * 1024];
        while (running) {
            try {
                cleanupAllocations();
                DatagramPacket dp = new DatagramPacket(udp_recv_buf, udp_recv_buf.length);
                ssocket.receive(dp);
                if (dp.getLength() > 0) {
                    byte[] packet = new byte[dp.getLength()];
                    System.arraycopy(udp_recv_buf, 0, packet, 0, packet.length);

                    if (packet.length < 20) {
                        log.warning("Malformed packet received (length: " + packet.length + ") from " + dp.getSocketAddress());
                        continue;
                    }

                    try (DataInputStream is = new DataInputStream(new ByteArrayInputStream(packet))) {
                        short messageType = is.readShort();
                        if (messageType >= 0x4000 && messageType <= 0x4FFF) {
                            short len = is.readShort();
                            byte[] data = is.readNBytes(len);
                            handleRelay(dp.getSocketAddress(), ssocket.getLocalSocketAddress(), messageType, data);
                            continue;
                        }
                        TurnPacket tp = TurnPacket.find(messageType);
                        if (tp == null) {
                            log.warning("Unknown message type from " + dp.getSocketAddress() + ": " + messageType);
                            continue;
                        }
                        log.debug("Packet received from " + dp.getSocketAddress() + ": " + tp);
                        try {
                            tp.initialize(log, this, packet);
                            try {
                                tp.process(is);
                            } catch (IOException e) {
                                log.exception(e, "Packet parsing failed");
                                sendErrorForRequest(dp.getSocketAddress(), tp, ErrorCode.ERROR_BAD_REQUEST, "Packet parsing failed");
                            }
                            tp.handle((InetSocketAddress) dp.getSocketAddress(), (InetSocketAddress) ssocket.getLocalSocketAddress(), is);
                        } catch (Exception ex) {
                            log.exception(ex, "Malformed packet received from " + dp.getSocketAddress());
                            sendErrorForRequest(dp.getSocketAddress(), tp, ErrorCode.ERROR_SERVER_ERROR, "Internal server error");
                        }
                    }
                }
            } catch (Exception ex) {
                log.warning("Listen failed: " + ex.getMessage());
            }
        }
        log.info("Server stopped");
    }

    public void handleRelay(SocketAddress sender, SocketAddress receiver, short channel, byte[] data) throws IOException {

        Allocation a = getAllocation(sender, receiver, ProtocolNumber.UDP);
        if (a == null) {
            log.warning("Discarding misdirected packet on channel " + channel + " from " + sender);
            return;
        }

        a.addRelayedBytes(data.length);

        InetSocketAddress target = a.getChannelPeer(channel);
        if (target == null) {
            log.warning("Discarding misdirected packet on unbound channel " + channel + " from " + sender + " to " + a);
            return;
        }

        if (config.log_very_verbose_relay_traffic) {
            log.debug(sender + " via " + receiver + " to " + target + "(" + data.length + " bytes)");
        }

        byte[] pk = new byte[4 + data.length];
        pk[0] = (byte) (channel >> 8);
        pk[1] = (byte) channel;
        pk[2] = (byte) (data.length >> 8);
        pk[3] = (byte) data.length;
        System.arraycopy(data, 0, pk, 4, data.length);
        DatagramPacket dp = new DatagramPacket(pk, pk.length, target);
        ssocket.send(dp);
    }

    public void sendErrorForRequest(SocketAddress target, TurnPacket requestPacket, int errorCode, String message, TurnAttribute... extra) throws IOException {
        log.warning("STUN communication error with " + target + ": " + errorCode + " / " + message);
        TurnPacket p = requestPacket.getNewErrorPacket();
        p.transactionId = requestPacket.transactionId;
        p.attributes.add(new ErrorCode(errorCode, message));
        Collections.addAll(p.attributes, extra);
        send(target, p);
    }

    public void send(SocketAddress target, TurnPacket packet) throws IOException {
        log.debug("Sending " + packet + " to " + target);
        packet.initialize(log, this, null);
        byte[] pk = packet.write();
        send(target, pk);
    }

    public void send(SocketAddress target, byte[] pk) throws IOException {
        log.debug("Sending " + pk.length + " bytes to " + target);
        DatagramPacket dp = new DatagramPacket(pk, 0, pk.length, target);
        ssocket.send(dp);
    }

    public void stopServer() {
        running = false;
        try {
            ssocket.close();
        } catch (Exception ignored) {
        }
    }

    public TurnConfig getConfig() {
        return config;
    }

    public byte[] getKeyFor(String username) {
        return null; // TODO
    }

    public TurnLogBack log() {
        return log;
    }

    public synchronized Allocation getAllocation(SocketAddress sender, SocketAddress receiver, ProtocolNumber protocol) {
        for (Allocation a : allocations) {
            if (a.equals(sender, receiver, protocol) && !a.isExpired()) {
                return a;
            }
        }
        return null;
    }

    public synchronized int getAllocationCount() {
        return allocations.size();
    }

    public synchronized InetSocketAddress allocate(InetSocketAddress sender, InetSocketAddress receiver, ProtocolNumber protocol, int lifetime, String username, String realm) throws IOException {
        Allocation a = new Allocation(this, sender, receiver, protocol, System.currentTimeMillis() + lifetime * 1000L, username, realm);
        a.createAllocation();
        allocations.add(a);
        return a.getRelay();
    }

    private synchronized void cleanupAllocations() {
        List<Allocation> expired = allocations.stream().filter(Allocation::isExpired).toList();
        for (Allocation a : expired) {
            log.info(a + " has expired");
            a.freeAllocation();
            allocations.remove(a);
        }
    }

    public synchronized Allocation getAllocationByRelay(SocketAddress peer) {
        return allocations.stream().filter(a -> a.getRelay().equals(peer)).findFirst().orElse(null);
    }

    public boolean externalAuthentication(InetSocketAddress sender, InetSocketAddress receiver, String username, String realm, String password) throws Exception {
        if (auth != null) {
            return auth.auth(sender, receiver, username, realm, password);
        }
        return false;
    }

    public TurnAllocationCallback getAllocationCallback() {
        return allocationCallback;
    }

    public void onAllocate(Allocation a) {
        if (allocationCallback != null) {
            try {
                allocationCallback.onAllocate(a);
            } catch (Throwable tr) {
                log.exception(tr, "Failed to call onAllocate for " + a);
            }
        }
    }

    public void onFree(Allocation a) {
        if (allocationCallback != null) {
            try {
                allocationCallback.onFree(a);
            } catch (Throwable tr) {
                log.exception(tr, "Failed to call onFree for " + a);
            }
        }
    }
}