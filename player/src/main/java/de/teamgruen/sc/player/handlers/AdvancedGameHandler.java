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

                        // skip paths that require more than two turns after reaching the destination
                        if (gameState.getBoard().getMinTurns(direction, path.get(path.size() - 1)) > 2)
                            return;

                        costs.put(path, turns);
                    });

                    final List<Vector3> shortestPath = costs.entrySet()
                            .stream()
                            .min(Comparator.comparingInt(Map.Entry::getValue))
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    if (shortestPath != null && shortestPath.size() > 2) {
                        final Optional<Move> move = MoveUtil.moveFromPath(gameState, shortestPath);

                        if (move.isPresent())
                            return move.get();
                    }

                    this.logger.debug(AnsiColor.WHITE + "Falling back to " + AnsiColor.PURPLE + "Simple" + AnsiColor.WHITE + " player" + AnsiColor.RESET);

                    // a negative timeout will disable move forecasting
                    final int timeout = shortestPath != null && shortestPath.size() == 2 ? -1 : 500;

                    return MoveUtil.getMostEfficientMove(gameState, timeout).orElse(null);
                }
        );
    }

    private Set<List<Vector3>> getPaths(GameState gameState) {
        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Vector3 shipPosition = playerShip.getPosition();
        final boolean isEnemyAhead = MoveUtil.isEnemyAhead(board, shipPosition, playerShip.getDirection(), enemyShip, enemyShip.getPosition());
        final Set<List<Vector3>> paths = new HashSet<>();

        if(playerShip.getPassengers() < 3) {
            final boolean canEnemyMove = !enemyShip.isStuck();

            // passengers
            board.getPassengerFields().forEach((position, field) -> {
                final Passenger passenger = (Passenger) field;

                if(passenger.getPassenger() < 1)
                    return;

                final Vector3 collectPosition = position.copy().add(passenger.getDirection().toVector3());
                final double segmentDistance = board.getSegmentDistance(shipPosition, collectPosition);

                if(segmentDistance < (playerShip.hasEnoughPassengers() ? 0 : -0.75) && canEnemyMove)
                    return;

                if(isEnemyAhead && board.getSegmentIndex(collectPosition) < 5)
                    return;

                final List<Vector3> path = PathFinder.findPath(playerShip, shipPosition, collectPosition);

                if(path != null)
                    paths.add(path);
            });
        }

        return paths;
    }

}
