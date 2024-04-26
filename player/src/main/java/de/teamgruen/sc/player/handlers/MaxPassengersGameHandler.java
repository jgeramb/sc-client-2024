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
import de.teamgruen.sc.sdk.logging.AnsiColor;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.NonNull;

import java.util.*;

public class MaxPassengersGameHandler extends BaseGameHandler {

    public MaxPassengersGameHandler(Logger logger) {
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

                    final HashMap<List<Vector3>, Integer> costs = new HashMap<>();

                    getPaths(gameState).forEach(path -> {
                        // skip impossible paths (start excluded from distance calculation)
                        if (path.size() <= minSpeed)
                            return;

                        Direction direction = playerDirection;
                        int turns = 0;

                        for (int i = 1; i < path.size(); i++) {
                            final Direction nextDirection = Direction.fromVector3(path.get(i).copy().subtract(path.get(i - 1)));

                            turns += direction.costTo(nextDirection);
                            direction = nextDirection;
                        }

                        // skip paths that require more turns than possible after reaching the destination
                        if (gameState.getBoard().getMinTurns(direction, path.get(path.size() - 1)) > 1 + playerShip.getCoal())
                            return;

                        costs.put(path, turns);
                    });

                    final List<Vector3> shortestPath = costs.entrySet()
                            .stream()
                            .min(Comparator.comparingInt(entry -> entry.getKey().size() + entry.getValue()))
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    if (shortestPath != null && !shortestPath.isEmpty()) {
                        final Vector3 endPosition = shortestPath.get(shortestPath.size() - 1);
                        final double segmentDistance = gameState.getBoard().getSegmentDistance(playerShip.getPosition(), endPosition);

                        // let weighted player handle short paths
                        if(segmentDistance > 0.75) {
                            final Optional<Move> move = MoveUtil.moveFromPath(gameState, shortestPath);

                            if (move.isPresent())
                                return move.get();
                        }
                    }

                    this.logger.debug(AnsiColor.WHITE + "Falling back to " + AnsiColor.PURPLE + "Simple" + AnsiColor.WHITE + " player" + AnsiColor.RESET);

                    return MoveUtil.getMostEfficientMove(gameState, 500).orElse(null);
                }
        );
    }

    private Set<List<Vector3>> getPaths(GameState gameState) {
        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Direction shipDirection = playerShip.getDirection();
        final Vector3 shipPosition = playerShip.getPosition();
        final Vector3 enemyPosition = enemyShip.getPosition();
        final boolean isEnemyAhead = MoveUtil.isEnemyAhead(board, shipPosition, shipDirection, enemyShip, enemyPosition);
        final Set<List<Vector3>> paths = new HashSet<>();

        if(playerShip.getPassengers() < 3 && !enemyShip.hasEnoughPassengers()) {
            // passengers
            board.getPassengerFields().forEach((position, field) -> {
                final Passenger passenger = (Passenger) field;

                if(passenger.getPassenger() < 1)
                    return;

                final Vector3 collectPosition = position.copy().add(passenger.getDirection().toVector3());

                // skip passengers that are too far behind
                if(board.getSegmentDistance(shipPosition, collectPosition) < -0.5)
                    return;

                if(isEnemyAhead && board.getSegmentIndex(collectPosition) < board.getSegmentIndex(enemyPosition) - 2)
                    return;

                final List<Vector3> path = PathFinder.findPath(shipDirection, shipPosition, collectPosition);

                if(path != null)
                    paths.add(path);
            });
        }

        if(playerShip.hasEnoughPassengers()) {
            // goals
            board.getGoalFields().keySet().forEach(position -> {
                final List<Vector3> path = PathFinder.findPath(shipDirection, shipPosition, position);

                if(path != null)
                    paths.add(path);
            });
        }

        return paths;
    }

}
