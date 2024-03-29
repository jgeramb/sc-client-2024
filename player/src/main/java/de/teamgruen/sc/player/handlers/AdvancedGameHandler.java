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
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdvancedGameHandler extends BaseGameHandler {

    private static final int CPU_THREADS = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));

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
        this.setNextMove(
                gameState,
                () -> {
                    final Ship playerShip = gameState.getPlayerShip();
                    final int minSpeed = Math.max(1, playerShip.getSpeed() - 1 - Math.min(playerShip.getCoal(), 2));
                    final Direction playerDirection = playerShip.getDirection();
                    final HashMap<LinkedList<Vector3>, Integer> costs = new HashMap<>();

                    getPaths(gameState).forEach(path -> {
                        Direction direction = playerDirection;
                        int turns = 0;

                        for (int i = 1; i < path.size(); i++) {
                            final Direction nextDirection = Direction.fromVector3(path.get(i).copy().subtract(path.get(i - 1)));

                            turns += direction.costTo(nextDirection);
                            direction = nextDirection;
                        }

                        // skip impossible paths (start excluded from distance calculation)
                        if(path.size() <= minSpeed)
                            return;

                        // skip paths that require two or more turns after reaching the destination
                        if(gameState.getBoard().getMinTurns(direction, path.get(path.size() - 1)) >= 2)
                            return;

                        costs.put(path, turns);
                    });

                    final LinkedList<Vector3> shortestPath = costs.entrySet()
                            .stream()
                            .min(Comparator.comparingInt(Map.Entry::getValue))
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    final Optional<Move> move = MoveUtil.moveFromPath(gameState, shortestPath);

                    return move.orElseGet(() -> MoveUtil.getMostEfficientMove(gameState, 250).orElse(null));
                }
        );
    }

    private List<LinkedList<Vector3>> getPaths(GameState gameState) {
        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Vector3 shipPosition = playerShip.getPosition();
        final List<LinkedList<Vector3>> paths = new ArrayList<>();
        final List<Runnable> tasks = new ArrayList<>();

        // check if the enemy ship is at least 2 segments ahead
        if(MoveUtil.isEnemyAhead(board, shipPosition, playerShip.getDirection(), enemyShip.getPosition()))
            tasks.add(() -> paths.add(PathFinder.findPath(playerShip, shipPosition, enemyShip.getPosition())));
        // collect passengers and move towards goal after reaching the 4th segment
        else if(board.getSegmentIndex(playerShip.getPosition()) >= 3) {
            final boolean hasEnoughPassengers = playerShip.hasEnoughPassengers();
            final boolean canEnemyMove = !enemyShip.isStuck();

            // passengers
            board.getPassengerFields().forEach((position, field) -> {
                final Passenger passenger = (Passenger) field;

                if(passenger.getPassenger() < 1)
                    return;

                final Vector3 collectPosition = position.copy().add(passenger.getDirection().toVector3());
                final double segmentDistance = board.getSegmentDistance(shipPosition, collectPosition);

                if(segmentDistance < -1.5 && (hasEnoughPassengers || canEnemyMove))
                    return;

                // skip passengers that are too far away
                if(segmentDistance > 2)
                    return;

                tasks.add(() -> paths.add(PathFinder.findPath(playerShip, shipPosition, collectPosition)));
            });
        }

        if(!tasks.isEmpty()) {
            final ExecutorService executorService = Executors.newFixedThreadPool(CPU_THREADS);
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

        return paths;
    }

}
