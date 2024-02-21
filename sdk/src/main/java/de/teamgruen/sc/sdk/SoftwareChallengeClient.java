package de.teamgruen.sc.sdk;

import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.XMLTcpClient;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpCloseException;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinPreparedRoomRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinRoomRequest;
import lombok.RequiredArgsConstructor;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@RequiredArgsConstructor
public class SoftwareChallengeClient {

    private final String host;
    private final int port;
    private final GameHandler gameHandler;
    private XMLTcpClient client;

    public void start() throws TcpConnectException {
        if(this.client != null)
            throw new IllegalStateException("Client already started");

        if(this.gameHandler == null)
            throw new IllegalStateException("No GameHandler provided");

        final Queue<XMLProtocolPacket> packetQueue = new LinkedBlockingQueue<>();
        final ClientPacketHandler packetHandler = new ClientPacketHandler(this, this.gameHandler);

        this.client = new XMLTcpClient(this.host, this.port);
        this.client.connect(packetQueue::add, this.gameHandler::onError);

        new Thread(() -> {
            Thread.currentThread().setName("PacketHandler");

            while(this.client != null) {
                final XMLProtocolPacket xmlProtocolPacket = packetQueue.poll();

                if (xmlProtocolPacket == null)
                    continue;

                packetHandler.handlePacket(xmlProtocolPacket);
            }
        }).start();
    }

    public void stop() throws TcpCloseException {
        try {
            if(this.client != null)
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

    public void joinGame() {
        this.client.send(new JoinGameRequest());
    }

    public void joinGame(String gameType) {
        final JoinGameRequest request = new JoinGameRequest();
        request.setGameType(gameType);

        this.client.send(request);
    }

    public void joinRoom(String roomId) {
        final JoinRoomRequest request = new JoinRoomRequest();
        request.setRoomId(roomId);

        this.client.send(request);
    }

    public void joinPreparedRoom(String reservationCode) {
        final JoinPreparedRoomRequest request = new JoinPreparedRoomRequest();
        request.setReservationCode(reservationCode);

        this.client.send(request);
    }

}
