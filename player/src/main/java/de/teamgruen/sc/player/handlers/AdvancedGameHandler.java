/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.player.utilities.MoveUtil;
import de.teamgruen.sc.player.utilities.paths.PathFinder;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdvancedGameHandler extends BaseGameHandler {

    public AdvancedGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onGameStart(@NonNull GameState gameState) {
        super.onGameStart(gameState);

        PathFinder.setGameState(gameState);
    }

    @Override
    public void onBoardUpdate(@NonNull GameState gameState) {
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

        paths.removeIf(path -> {
            if(Objects.isNull(path) || path.size() < 2)
                return true;

            final Vector3 endPosition = path.get(path.size() - 1);

            if(gameState.getBoard().getFieldAt(endPosition) instanceof Finish)
                return false;

            // remove path if it's not possible to go further after reaching the end position
            final Vector3 beforeLastPosition = path.get(path.size() - 2);
            final Direction direction = Direction.fromVector3(endPosition.copy().subtract(beforeLastPosition));

            return gameState.getMinTurns(direction, endPosition) > Math.min(2, playerShip.getCoal() + 1);
        });

        this.setNextMove(
                gameState,
                MoveUtil.moveFromPath(gameState, paths.stream().min(Comparator.comparingInt(List::size)).orElse(null))
                        .orElse(MoveUtil.getMostEfficientMove(gameState).orElse(null))
        );
    }

}
