package de.teamgruen.sc.player.clients;

import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;

public class PlayerClient extends Client {

    private final GameHandler gameHandler;

    public PlayerClient(String host, int port, GameHandler gameHandler) {
        super(host, port);

        this.gameHandler = gameHandler;
    }

    public void connect() throws TcpConnectException {
        super.connect(this.gameHandler);
    }

    public void joinAnyGame() {
        this.client.joinAnyGame();
    }

    public void joinGame(String gameType) {
        this.client.joinGame(gameType);
    }

    public void joinPreparedRoom(String reservation) {
        this.client.joinPreparedRoom(reservation);
    }

    public void joinRoom(String roomId) {
        this.client.joinRoom(roomId);
    }

}
