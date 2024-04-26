/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.clients;

import de.teamgruen.sc.player.handlers.MaxPassengersGameHandler;
import de.teamgruen.sc.player.handlers.WeightedGameHandler;
import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.handlers.AdminGameHandler;
import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.logging.Level;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static de.teamgruen.sc.sdk.logging.AnsiColor.*;

public class AdminClient extends Client {

    private final Object roomCreateLock = new Object();
    private final List<ControlledRoom> controlledRooms = new ArrayList<>();
    private final int[][] playerStats = new int[2][4];
    private final AtomicBoolean replaceRequired = new AtomicBoolean(false);
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
                final ControlledRoom controlledRoom = new ControlledRoom();

                controlledRooms.add(controlledRoom);

                final int gameId = controlledRooms.size();

                for (int i = 0; i < playerStats.length; i++) {
                    final int playerId = i;

                    try {
                        final Logger playerLogger = new Logger(new ByteArrayOutputStream());
                        final GameHandler gameHandler = Objects.equals(playStyle, "weighted") || (playerId == 0 && !Objects.equals(playStyle, "max-passengers"))
                                ? new WeightedGameHandler(playerLogger)
                                : new MaxPassengersGameHandler(playerLogger);
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
                                try {
                                    return gameHandler.getNextActions(gameState);
                                } catch (Exception ex) {
                                    onError(ex.getMessage());

                                    return Collections.emptyList();
                                }
                            }

                            @Override
                            public void onResults(@NonNull LinkedHashMap<ScoreFragment, Integer> scores, @NonNull GameResult result, String reason) {
                                gameHandler.onResults(scores, result, reason);

                                if(reason != null && !reason.isEmpty() && result.equals(GameResult.LOOSE)) {
                                    final String playerName = gameHandler instanceof WeightedGameHandler ? "Weighted" : "MaxPassengers";

                                    print(Level.DEBUG, "Lost game " + RED + gameId + RESET + " - " + RED + playerName + RESET + ": " + WHITE + reason + RESET);
                                }

                                synchronized (playerStats) {
                                    scores.forEach((scoreFragment, score) -> {
                                        if(scoreFragment.getName().equals("Passagiere"))
                                            playerStats[playerId][2] += score;
                                    });

                                    switch (result) {
                                        case WIN:
                                            playerStats[playerId][0]++;
                                            break;
                                        case DRAW:
                                            playerStats[playerId][1]++;
                                            break;
                                    }
                                }

                                controlledRoom.endGame();
                            }

                            @Override
                            public void onError(String message) {
                                final String playerName = gameHandler instanceof WeightedGameHandler ? "Weighted" : "MaxPassengers";

                                print(Level.ERROR, "Game " + RED + gameId + RESET + " - " + RED + playerName + RESET + ": " + WHITE + message + RESET);

                                playerStats[playerId][3]++;

                                controlledRoom.endGame();
                            }
                        });

                        controlledRoom.clients.add(playerClient);

                        playerClient.connect();
                        playerClient.joinPreparedRoom(reservations.get(i));
                    } catch (TcpConnectException ex) {
                        print(Level.ERROR, "Error: " + ex.getMessage());
                    }
                }

                synchronized (roomCreateLock) {
                    roomCreateLock.notify();
                }
            }

            @Override
            public void onPlayerJoined(int playerCount) {
                if (playerCount == 1)
                    print(Level.DEBUG, "Waiting for second player");
            }

            @Override
            public void onError(String message) {
                print(Level.ERROR, message);
            }
        });
    }

    public void runTests(@NonNull String password, int count) {
        // authenticate the client
        this.client.authenticate(password);

        // initialize the player stats
        for (int i = 0; i < playerStats.length; i++)
            playerStats[i] = new int[4];

        final int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 8);
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch latch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                try {
                    // create a room and wait for the response

                    this.client.prepareRoom();

                    synchronized (roomCreateLock) {
                        try {
                            roomCreateLock.wait();
                        } catch (InterruptedException ignore) {
                        }
                    }

                    final int gameId = controlledRooms.size();

                    Thread.currentThread().setName("GameThread-" + gameId);

                    final String spacer = " ".repeat(String.valueOf(count).length() - String.valueOf(gameId).length());

                    synchronized (this.logger) {
                        this.logger.log(Level.INFO, "Running game " + GREEN + spacer + gameId + RESET + " of " + WHITE + count + RESET, true);
                        replaceRequired.set(true);
                    }

                    // wait for the game to end

                    final ControlledRoom controlledRoom = controlledRooms.get(gameId - 1);

                    synchronized (controlledRoom.endLock) {
                        try {
                            controlledRoom.endLock.wait(TimeUnit.SECONDS.toMillis(30));
                        } catch (InterruptedException ignore) {
                        }
                    }

                    // disconnect the clients

                    controlledRoom.clients.forEach(playerClient -> {
                        try {
                            playerClient.disconnect();
                        } catch (IOException ignore) {
                        }
                    });
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException ignore) {
        }

        executor.shutdown();

        // print the final stats

        print(Level.INFO, "");
        this.logger.info(GREEN + "✓ All games completed" + RESET);

        final String line = BLACK + "―".repeat(3);

        for (int playerId = 0; playerId < this.playerStats.length; playerId++) {
            final int[] stats = this.playerStats[playerId];
            final String playerDescription = switch (playStyle == null ? "mixed" : playStyle) {
                case "weighted" -> "Weighted #" + (playerId + 1);
                case "max-passengers" -> "MaxPassengers #" + (playerId + 1);
                default -> playerId == 0 ? "Weighted" : "MaxPassengers";
            };

            this.logger.info("");
            this.logger.info(line + " " + PURPLE + playerDescription + " " + line + RESET);
            this.logger.info(WHITE + "┣" + RESET + " Wins:   " + stats[0]);
            this.logger.info(WHITE + "┣" + RESET + " Draws:  " + stats[1]);
            this.logger.info(WHITE + "┣" + RESET + " Passengers (ø):  " + String.format("%.1f", stats[2] / (double) count));
            this.logger.info(WHITE + "┗" + RESET + " Errors:  " + stats[3]);
        }

        // shutdown the JVM
        System.exit(0);
    }

    private void print(Level level, String message) {
        synchronized (this.logger) {
            if(!this.logger.log(level, message, replaceRequired.get()))
                return;
        }

        if(replaceRequired.get()) {
            System.out.println("\r");

            replaceRequired.set(false);
        }
    }

    @RequiredArgsConstructor
    private static class ControlledRoom {

        private final Object endLock = new Object();
        private final AtomicInteger endedClients = new AtomicInteger();
        private final List<PlayerClient> clients = new ArrayList<>();

        public void endGame() {
            if (this.endedClients.incrementAndGet() == 2) {
                synchronized (this.endLock) {
                    this.endLock.notify();
                }
            }
        }

    }

}
