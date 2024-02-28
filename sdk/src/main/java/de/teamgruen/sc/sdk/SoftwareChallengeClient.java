package de.teamgruen.sc.sdk;

import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.XMLTcpClient;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinPreparedRoomRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@RequiredArgsConstructor
public class SoftwareChallengeClient {

    private final String host;
    private final int port;
    private final GameHandler gameHandler;
    @Setter
    private XMLTcpClient client;

    public void start() throws TcpConnectException {
        if(this.client != null)
            throw new IllegalStateException("Client already started");

        if(this.gameHandler == null)
            throw new IllegalArgumentException("No GameHandler provided");

        final Queue<XMLProtocolPacket> packetQueue = new LinkedBlockingQueue<>();
        final ClientPacketHandler packetHandler = new ClientPacketHandler(this, this.gameHandler);

        this.client = new XMLTcpClient(this.host, this.port);
        this.client.connect(packetQueue::add, this.gameHandler::onError);

        new Thread(() -> {
            while(this.client != null) {
                final XMLProtocolPacket xmlProtocolPacket = packetQueue.poll();

                if (xmlProtocolPacket == null)
                    continue;

                new Thread(() -> packetHandler.handlePacket(xmlProtocolPacket), "PacketHandler").start();
            }
        }).start();
    }

    public void stop() {
        if(this.client == null)
            throw new IllegalStateException("Client not started");

        try {
            this.client.disconnect();
        } finally {
            this.client = null;
        }
    }

    public void sendPacket(XMLProtocolPacket packet) {
        if(this.client == null)
            throw new IllegalStateException("Client not started");

        this.client.send(packet);
    }

    public void joinAnyGame() {
        this.client.send(new JoinGameRequest(null));
    }

    public void joinGame(String gameType) {
        this.client.send(new JoinGameRequest(gameType));
    }

    public void joinRoom(String roomId) {
        this.client.send(new JoinRoomRequest(roomId));
    }

    public void joinPreparedRoom(String reservationCode) {
        this.client.send(new JoinPreparedRoomRequest(reservationCode));
    }

}
