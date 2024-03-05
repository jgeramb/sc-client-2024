/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.clients;

import de.teamgruen.sc.sdk.SoftwareChallengeClient;
import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class Client {

    protected final String host;
    protected final int port;
    protected SoftwareChallengeClient client;

    protected void connect(@NonNull GameHandler gameHandler) throws TcpConnectException {
        this.client = new SoftwareChallengeClient(host, port, gameHandler);
        this.client.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.disconnect();
            } catch (IOException ignore) {
            }
        }));
    }

    public void disconnect() throws IOException {
        this.client.stop();
    }

}
