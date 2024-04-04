/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;
import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static de.teamgruen.sc.sdk.logging.AnsiColor.*;

public abstract class BaseGameHandler implements GameHandler {

    protected final Logger logger;
    protected List<Action> nextActions;
    private long lastActionTime;

    protected BaseGameHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameStart(@NonNull GameState gameState) {
        this.logger.info("Game started (Team " + PURPLE + gameState.getPlayerTeam() + RESET + ")");
    }

    @Override
    public void onRoomJoin(String roomId) {
        this.logger.info("Joined room " + PURPLE + roomId + RESET);
    }

    public void setNextMove(GameState gameState, Supplier<Move> moveSupplier) {
        if(!gameState.getPlayerTeam().equals(gameState.getCurrentTeam()))
            return;

        this.lastActionTime = System.currentTimeMillis();

        final Move move = moveSupplier.get();

        if(move == null)
            this.nextActions = null;
        else {
            final Ship playerShip = gameState.getPlayerShip();
            final List<Action> actions = move.getActions();
            actions.forEach(action -> action.perform(gameState));
            playerShip.setCoal(playerShip.getCoal() - move.getCoalCost(playerShip.getDirection(), playerShip.getSpeed(), playerShip.getFreeTurns()));

            this.nextActions = actions;
        }

        final int turn = gameState.getTurn();

        this.logger.debug(
                "Turn " + WHITE + "#" + GREEN + " ".repeat(turn < 10 ? 1 : 0) + turn + RESET + " calculated in " +
                        PURPLE + String.format("%,d", System.currentTimeMillis() - this.lastActionTime) + WHITE + "ms" +
                        RESET
        );
        this.logger.debug("Next actions: " + WHITE + this.nextActions + RESET);
    }

    @Override
    public List<Action> getNextActions(@NonNull GameState gameState) throws IllegalStateException {
        try {
            if(this.nextActions == null || this.nextActions.isEmpty())
                throw new IllegalStateException("No actions available");
            
            return this.nextActions;
        } finally {
            final int turn = gameState.getTurn();

            this.logger.debug(
                    "Turn " + WHITE + "#" + GREEN + " ".repeat(turn < 10 ? 1 : 0) + turn + RESET + " took " +
                    PURPLE + String.format("%,d", System.currentTimeMillis() - this.lastActionTime) + WHITE + "ms" +
                    RESET
            );
        }
    }

    @Override
    public void onResults(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result) {
        final int maxNameLength = scores.keySet()
                .stream()
                .mapToInt(scoreFragment -> scoreFragment.getName().length())
                .max()
                .orElse(0);

        final AtomicInteger maxValueLength = new AtomicInteger(0);

        scores.forEach((scoreFragment, score) -> {
            if(scoreFragment.isRelevantForRanking())
                maxValueLength.set(Math.max(maxValueLength.get(), String.valueOf(score).length()));
        });

        final String horizontalLine = BLACK + "―".repeat(12 + maxValueLength.get()) + RESET;

        this.logger.info(horizontalLine);

        final String resultSymbol = result.equals(GameResult.WIN)
                ? GREEN + "✓"
                : result.equals(GameResult.LOOSE)
                        ? RED + "✕"
                        : WHITE + "/";
        final String resultSpacer = " ".repeat(maxNameLength - 8) + " ".repeat(maxValueLength.get() - 1);
        this.logger.info("Gewonnen: " + resultSpacer + resultSymbol + RESET);

        scores.forEach((scoreFragment, score) -> {
            if(!scoreFragment.isRelevantForRanking())
                return;

            final String name = scoreFragment.getName();
            final String spacer = " ".repeat(maxNameLength - name.length())
                    + " ".repeat(maxValueLength.get() - String.valueOf(score).length());

            this.logger.info(name + ": " + spacer + PURPLE + score + RESET);
        });

        this.logger.info(horizontalLine);
    }

    @Override
    public void onError(String message) {
        this.logger.error(message);
    }

}
