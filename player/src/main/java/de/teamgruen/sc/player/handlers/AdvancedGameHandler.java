/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.player.utilities.MoveUtil;
import de.teamgruen.sc.player.utilities.paths.PathFinder;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdvancedGameHandler extends BaseGameHandler {

    public AdvancedGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onGameStart(GameState gameState) {
        super.onGameStart(gameState);

        PathFinder.setGameState(gameState);
    }

    @Override
    public void onBoardUpdate(GameState gameState) {
        if(!gameState.getPlayerTeam().equals(gameState.getCurrentTeam()))
            return;

        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip();
        final Vector3 shipPosition = playerShip.getPosition();
        final List<List<Vector3>> paths = new ArrayList<>();
        final List<Runnable> tasks = new ArrayList<>();

        final Vector3 enemyShipPosition = gameState.getEnemyShip().getPosition();
        final int enemyShipVisitedSegmentsCount = board.getSegmentIndex(enemyShipPosition);

        // check if enemy ship is more than 2 segments ahead
        if(enemyShipVisitedSegmentsCount - board.getSegmentIndex(playerShip.getPosition()) > 2)
            tasks.add(() -> paths.add(PathFinder.findPath(shipPosition, enemyShipPosition)));
        else {
            // passengers
            board.getPassengerFields().forEach((position, field) -> {
                final Passenger passenger = (Passenger) field;

                if(passenger.getPassenger() < 1)
                    return;

                final Vector3 collectPosition = position.copy().add(passenger.getDirection().toVector3());

                tasks.add(() -> paths.add(PathFinder.findPath(shipPosition, collectPosition)));
            });

            if (playerShip.hasEnoughPassengers()) {
                // finish
                board.getFinishFields().forEach((position, field) -> tasks.add(() ->
                        paths.add(PathFinder.findPath(shipPosition, position))
                ));
            }
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        final CountDownLatch countDownLatch = new CountDownLatch(tasks.size());

        tasks.forEach(task -> executorService.submit(() -> {
            try {
                task.run();
            } finally {
                countDownLatch.countDown();
            }
        }));

        try {
            countDownLatch.await();
        } catch (InterruptedException ignore) {
        } finally {
            executorService.shutdown();
        }

        Optional<Move> nextMove = MoveUtil.moveFromPath(
                gameState,
                paths.stream()
                        .filter(Objects::nonNull)
                        .min(Comparator.comparingInt(List::size))
                        .orElse(null)
        );

        if(nextMove.isEmpty())
            nextMove = MoveUtil.getMostEfficientMove(gameState);

        this.setNextMove(gameState, nextMove.orElse(null));
    }

}
