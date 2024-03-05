/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.clients;

import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import lombok.NonNull;

public class PlayerClient extends Client {

    private final GameHandler gameHandler;

    public PlayerClient(@NonNull String host, int port, @NonNull GameHandler gameHandler) {
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

    public void joinPreparedRoom(@NonNull String reservation) {
        this.client.joinPreparedRoom(reservation);
    }

    public void joinRoom(@NonNull String roomId) {
        this.client.joinRoom(roomId);
    }

}
