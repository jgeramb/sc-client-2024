package de.teamgruen.sc.sdk;

import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.XMLTcpClient;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinPreparedRoomRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinRoomRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

public class SoftwareChallengeClient {

    private final GameHandler gameHandler;
    @Setter
    @Getter
    private XMLTcpClient client;

    public SoftwareChallengeClient(String host, int port, GameHandler gameHandler) {
        this.gameHandler = gameHandler;
        this.client = new XMLTcpClient(host, port);
    }

    /**
     * @throws IllegalStateException if the client is already started
     * @throws IllegalArgumentException if no GameHandler is provided
     */
    public void start() throws TcpConnectException {
        if(this.client.isConnected())
            throw new IllegalStateException("Client already started");

        if(this.gameHandler == null)
            throw new IllegalArgumentException("No GameHandler provided");

        final ClientPacketHandler packetHandler = new ClientPacketHandler(this, this.gameHandler);

        this.client.connect(
                packet -> new Thread(() -> packetHandler.handlePacket(packet), "PacketHandler").start(),
                this.gameHandler::onError
        );
    }

    /**
     * @throws IllegalStateException if the client is not started
     */
    public void stop() throws IOException {
        if(!this.client.isConnected())
            throw new IllegalStateException("Client not started");

        this.client.disconnect();
    }

    public void sendPacket(XMLProtocolPacket packet) {
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
