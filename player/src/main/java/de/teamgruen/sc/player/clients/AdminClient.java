/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.clients;

import de.teamgruen.sc.player.handlers.AdvancedGameHandler;
import de.teamgruen.sc.player.handlers.SimpleGameHandler;
import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.handlers.AdminGameHandler;
import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static de.teamgruen.sc.sdk.logging.AnsiColor.*;

public class AdminClient extends Client {

    private final Object gameEndLock = new Object();
    private final AtomicReference<String> currentRoomId = new AtomicReference<>();
    private final List<PlayerClient> playerClients = new ArrayList<>();
    private final Logger logger;
    private final Map<String, int[]> playStyleStats = new LinkedHashMap<>();

    public AdminClient(@NonNull Logger logger, @NonNull String host, int port) {
        super(host, port);

        this.logger = logger;
    }

    public void connect() throws TcpConnectException {
        super.connect(new AdminGameHandler() {
            @Override
            public void onRoomCreated(String roomId, List<String> reservations) {
                currentRoomId.set(roomId);

                for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
                    try {
                        final Logger playerLogger = new Logger(new ByteArrayOutputStream());
                        final GameHandler gameHandler = playerIndex == 0
                                ? new SimpleGameHandler(playerLogger)
                                : new AdvancedGameHandler(playerLogger);
                        final PlayerClient playerClient = new PlayerClient(host, port, new GameHandler() {
                            @Override
                            public void onRoomJoin(String roomId) {
                                gameHandler.onRoomJoin(roomId);
                            }

                            @Override
                            public void onGameStart(@NonNull GameState gameState) {
                                gameHandler.onGameStart(gameState);
                            }

                            @Override
                            public void onBoardUpdate(@NonNull GameState gameState) {
                                gameHandler.onBoardUpdate(gameState);
                            }

                            @Override
                            public List<Action> getNextActions(@NonNull GameState gameState) {
                                return gameHandler.getNextActions(gameState);
                            }

                            @Override
                            public void onResults(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result) {
                                gameHandler.onResults(scores, result);

                                final String playStyle = gameHandler instanceof SimpleGameHandler ? "Simple" : "Advanced";

                                switch (result) {
                                    case WIN:
                                        playStyleStats.get(playStyle)[0]++;
                                        break;
                                    case LOOSE:
                                        playStyleStats.get(playStyle)[1]++;
                                        break;
                                    case DRAW:
                                        playStyleStats.get(playStyle)[2]++;
                                        break;
                                }

                                if(roomId.equals(currentRoomId.get())) {
                                    synchronized (gameEndLock) {
                                        gameEndLock.notify();
                                    }
                                }
                            }

                            @Override
                            public void onError(String message) {
                                final String playerName = gameHandler instanceof SimpleGameHandler ? "Simple" : "Advanced";

                                logger.error("Error by " + GREEN + playerName + RESET + ": " + WHITE + message + RESET);

                                playStyleStats.get(playerName)[3]++;
                            }
                        });
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
            public void onError(String message) {
                logger.error("Error: " + message);
            }
        });
    }

    public void runTests(@NonNull String password, int count) {
        this.playStyleStats.put("Simple", new int[4]);
        this.playStyleStats.put("Advanced", new int[4]);

        for (int i = 1; i <= count; i++) {
            final String currentCount = String.valueOf(i);
            final String spacer = " ".repeat(String.valueOf(count).length() - currentCount.length());

            this.logger.info("Game " + GREEN + spacer + currentCount + RESET + " of " + PURPLE + count + RESET);
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
            this.currentRoomId.set(null);
        }

        final String line = BLACK + "―".repeat(3);

        for (Map.Entry<String, int[]> entry : this.playStyleStats.entrySet()) {
            this.logger.info("");
            this.logger.info(line + " " + PURPLE + entry.getKey() + " " + line + RESET);
            this.logger.info(WHITE + "┣" + RESET + " Wins:   " + entry.getValue()[0]);
            this.logger.info(WHITE + "┣" + RESET + " Losses: " + entry.getValue()[1]);
            this.logger.info(WHITE + "┣" + RESET + " Draws:  " + entry.getValue()[2]);
            this.logger.info(WHITE + "┗" + RESET + " Errors:  " + entry.getValue()[3]);
        }

        // shutdown the JVM
        System.exit(0);
    }

}
