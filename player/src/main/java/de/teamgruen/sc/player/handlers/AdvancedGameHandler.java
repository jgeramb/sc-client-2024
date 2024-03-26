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
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Vector3 shipPosition = playerShip.getPosition();
        final List<List<Vector3>> paths = new ArrayList<>();
        final List<Runnable> tasks = new ArrayList<>();

        // check if enemy ship is more than 2 segments ahead
        if(MoveUtil.isEnemyAhead(board, shipPosition, enemyShip.getPosition()))
            tasks.add(() -> paths.add(PathFinder.findPath(shipPosition, enemyShip.getPosition())));
        // collect passengers and move towards goal after reaching the 5th segment
        else if(board.getSegmentIndex(playerShip.getPosition()) >= 4) {
            // passengers
            board.getPassengerFields().forEach((position, field) -> {
                final Passenger passenger = (Passenger) field;

                if(passenger.getPassenger() < 1)
                    return;

                final Vector3 collectPosition = position.copy().add(passenger.getDirection().toVector3());

                tasks.add(() -> paths.add(PathFinder.findPath(shipPosition, collectPosition)));
            });

            // collect more passengers if the enemy ship is stuck, otherwise move towards a goal
            if (playerShip.hasEnoughPassengers() && (!enemyShip.isStuck() || tasks.isEmpty())) {
                board.getGoalFields().forEach((position, field) -> tasks.add(() ->
                        paths.add(PathFinder.findPath(shipPosition, position))
                ));
            }
        }

        if(!tasks.isEmpty()) {
            final ExecutorService executorService = Executors.newFixedThreadPool(2);
            final CountDownLatch countDownLatch = new CountDownLatch(tasks.size());

            tasks.forEach(task -> executorService.submit(() -> {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    this.onError(throwable.getMessage());
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

            // remove invalid paths
            paths.removeIf(path -> path == null || path.size() < 2);
        }

        this.setNextMove(
                gameState,
                MoveUtil.moveFromPath(gameState, paths.stream().min(Comparator.comparingInt(List::size)).orElse(null))
                        .orElse(MoveUtil.getMostEfficientMove(gameState).orElse(null))
        );
    }

}
