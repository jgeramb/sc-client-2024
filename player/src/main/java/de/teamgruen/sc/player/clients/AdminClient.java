package de.teamgruen.sc.player.clients;

import de.teamgruen.sc.player.handlers.AdvancedGameHandler;
import de.teamgruen.sc.player.handlers.SimpleGameHandler;
import de.teamgruen.sc.sdk.game.handlers.AdminGameHandler;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminClient extends Client {

    private final Object gameEndLock = new Object();
    private final List<PlayerClient> playerClients = new ArrayList<>();
    private final Logger logger;

    public AdminClient(Logger logger, String host, int port) {
        super(host, port);

        this.logger = logger;
    }

    public void connect() throws TcpConnectException {
        super.connect(new AdminGameHandler() {
            @Override
            public void onRoomCreated(String roomId, List<String> reservations) {
                for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
                    try {
                        final PlayerClient playerClient = new PlayerClient(
                                host,
                                port,
                                playerIndex == 0
                                        ? new SimpleGameHandler(logger)
                                        : new AdvancedGameHandler(logger)
                        );
                        playerClient.connect();
                        playerClient.joinPreparedRoom(reservations.get(playerIndex));

                        playerClients.add(playerClient);
                    } catch (TcpConnectException ex) {
                        logger.error("Error: " + ex.getMessage());
                    }
                }
            }

            @Override
            public void onPlayerJoined(int playerCount) {
                if (playerCount == 1)
                    logger.info("Waiting for second player");
            }

            @Override
            public void onGameEnd() {
                synchronized (gameEndLock) {
                    gameEndLock.notify();
                }
            }

            @Override
            public void onError(String message) {
                logger.error("Error: " + message);
            }
        });
    }

    public void runTests(String password, int count) {
        for (int i = 0; i < count; i++) {
            this.client.prepareRoom(password);

            try {
                synchronized (this.gameEndLock) {
                    this.gameEndLock.wait();
                }
            } catch (InterruptedException ignore) {
            }

            this.playerClients.forEach(client -> {
                try {
                    client.disconnect();
                } catch (IOException ex) {
                    logger.error("Error: " + ex.getMessage());
                }
            });
            this.playerClients.clear();
        }
    }

}
