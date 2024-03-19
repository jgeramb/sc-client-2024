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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static de.teamgruen.sc.sdk.logging.AnsiColor.*;

public class AdminClient extends Client {

    private final Object gameEndLock = new Object();
    private final AtomicReference<String> currentRoomId = new AtomicReference<>();
    private final List<PlayerClient> playerClients = new ArrayList<>();
    private final int[][] playerStats = new int[2][4];
    private final Logger logger;
    private final String playStyle;

    public AdminClient(@NonNull Logger logger, @NonNull String host, int port, String playStyle) {
        super(host, port);

        this.logger = logger;
        this.playStyle = playStyle;
    }

    public void connect() throws TcpConnectException {
        super.connect(new AdminGameHandler() {
            @Override
            public void onRoomCreated(String roomId, List<String> reservations) {
                currentRoomId.set(roomId);

                for (int i = 0; i < playerStats.length; i++) {
                    final int playerId = i;

                    try {
                        final Logger playerLogger = new Logger(new ByteArrayOutputStream());
                        final GameHandler gameHandler = Objects.equals(playStyle, "simple") || (playerId == 0 && !Objects.equals(playStyle, "advanced"))
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

                                switch (result) {
                                    case WIN:
                                        playerStats[playerId][0]++;
                                        break;
                                    case LOOSE:
                                        playerStats[playerId][1]++;
                                        break;
                                    case DRAW:
                                        playerStats[playerId][2]++;
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

                                playerStats[playerId][3]++;
                            }
                        });
                        playerClient.connect();
                        playerClient.joinPreparedRoom(reservations.get(i));

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
        // initialize the player stats
        for (int i = 0; i < playerStats.length; i++)
            playerStats[i] = new int[4];

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

        for (int playerId = 0; playerId < this.playerStats.length; playerId++) {
            final int[] stats = this.playerStats[playerId];
            final String playerDescription = switch (playStyle) {
                case "simple" -> "Simple #" + (playerId + 1);
                case "advanced" -> "Advanced #" + (playerId + 1);
                default -> playerId == 0 ? "Simple" : "Advanced";
            };

            this.logger.info("");
            this.logger.info(line + " " + PURPLE + playerDescription + " " + line + RESET);
            this.logger.info(WHITE + "┣" + RESET + " Wins:   " + stats[0]);
            this.logger.info(WHITE + "┣" + RESET + " Losses: " + stats[1]);
            this.logger.info(WHITE + "┣" + RESET + " Draws:  " + stats[2]);
            this.logger.info(WHITE + "┗" + RESET + " Errors:  " + stats[3]);
        }

        // shutdown the JVM
        System.exit(0);
    }

}
