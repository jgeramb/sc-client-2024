/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities;

import de.teamgruen.sc.sdk.game.AdvanceInfo;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Goal;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MoveUtil {

    /**
     * Returns the most efficient move for the current game state.
     * @param gameState the current game state
     * @return the most efficient move
     */
    public static Optional<Move> getMostEfficientMove(@NonNull GameState gameState) {
        final List<Move> moves = getPossibleMoves(gameState, false);
        final Ship playerShip = gameState.getPlayerShip();
        final Vector3 position = playerShip.getPosition();
        final int playerSegmentIndex = gameState.getBoard().getSegmentIndex(position);
        final int playerSegmentColumn = gameState.getBoard().getSegmentColumn(position);
        final int enemySegmentIndex = gameState.getBoard().getSegmentIndex(gameState.getEnemyShip().getPosition());
        final boolean isEnemyAhead = enemySegmentIndex > playerSegmentIndex + 2;

        Move bestMove = null;
        double highestScore = Integer.MIN_VALUE;
        int lowestCoalCost = 0;

        for(Move move : moves) {
            final boolean canEnd = gameState.getBoard().getFieldAt(move.getEndPosition()) instanceof Goal
                    && playerShip.hasEnoughPassengers();
            final double deltaSegmentColumn = Math.floorMod(move.getSegmentColumn() - playerSegmentColumn, 4);
            final double deltaSegmentPosition = move.getSegmentIndex() - playerSegmentIndex
                    + (deltaSegmentColumn > 2 ? deltaSegmentColumn - 4 : deltaSegmentColumn) / 4d;
            final int coalCost = move.getCoalCost(playerShip);
            final double score = (move.isGoal() ? 100 : 0)
                    + (canEnd || (isEnemyAhead && deltaSegmentPosition >= 0) ? 50 : 0)
                    + move.getPassengers() * 15
                    + move.getPushes() * 3
                    + move.getDistance() * deltaSegmentPosition
                    - coalCost
                    - Math.max(0, move.getMinTurns(gameState) - 1) * 3.5;

            if(score > highestScore || score == highestScore && coalCost < lowestCoalCost) {
                highestScore = score;
                bestMove = move;
                lowestCoalCost = coalCost;
            }
        }

        return Optional.ofNullable(bestMove);
    }

    /**
     * Returns a list of all possible moves for the current game state.
     * @param gameState the current game state
     * @param moreCoal whether to consider moves with more coal
     * @return a list of all possible moves
     */
    public static List<Move> getPossibleMoves(@NonNull GameState gameState, boolean moreCoal) {
        final Ship ship = gameState.getPlayerShip();
        final int maxCoal = Math.min(ship.getCoal(), moreCoal || gameState.getTurn() < 2 ? 2 : 1);

        final List<Move> moves = new ArrayList<>(gameState.getMoves(
                ship.getPosition(),
                ship.getDirection(),
                ship.getFreeTurns(),
                1,
                getMinMovementPoints(gameState),
                getMaxMovementPoints(gameState, maxCoal),
                maxCoal
        ));

        // if no moves are possible, try to move with more coal
        if(moves.isEmpty() && !moreCoal && ship.getCoal() > 1)
            return getPossibleMoves(gameState, true);

        moves.forEach(move -> addAcceleration(ship, move));

        return moves;
    }

    /**
     * Adds an acceleration action to the given move if necessary.
     * @param ship the player's ship
     * @param move the move to add the acceleration action to
     */
    private static void addAcceleration(@NonNull Ship ship, @NonNull Move move) {
        final int acceleration = move.getAcceleration(ship);

        if(acceleration != 0)
            move.getActions().add(0, ActionFactory.changeVelocity(acceleration));
    }

    /**
     * Returns the minimum movement points for the current game state.
     * @param gameState the current game state
     * @return the minimum movement points
     */
    private static int getMinMovementPoints(@NonNull GameState gameState) {
        return Math.max(1, gameState.getPlayerShip().getSpeed() - 1);
    }

    /**
     * Returns the maximum movement points for the current game state.
     * @param gameState the current game state
     * @param maxCoal the maximum amount of coal to use
     * @return the maximum movement points
     */
    private static int getMaxMovementPoints(@NonNull GameState gameState, int maxCoal) {
        final Ship ship = gameState.getPlayerShip();
        final int playerSegmentIndex = gameState.getBoard().getSegmentIndex(ship.getPosition());
        final int enemySegmentIndex = gameState.getBoard().getSegmentIndex(gameState.getEnemyShip().getPosition());
        final boolean useCoal = enemySegmentIndex > playerSegmentIndex + 1 || gameState.getTurn() < 2;

        return Math.min(6, ship.getSpeed() + ((useCoal && maxCoal > 0) ? 2 : 1));
    }

    /**
     * Returns the next move to reach the given path.
     * @param gameState the current game state
     * @param path the path to reach
     * @return the next move to reach the given path
     */
    public static Optional<Move> moveFromPath(@NonNull GameState gameState, List<Vector3> path) {
        if(path == null || path.isEmpty())
            return Optional.empty();

        final Ship playerShip = gameState.getPlayerShip();
        final Move move = new Move(path.get(0), playerShip.getDirection());
        final int maxMovementPoints = getMaxMovementPoints(gameState, playerShip.getCoal());
        final Vector3 destination = path.get(path.size() - 1);
        final Field destinationField = gameState.getBoard().getFieldAt(destination);
        final boolean collectPassenger = Arrays.stream(Direction.values())
                .map(direction -> destination.copy().add(direction.toVector3()))
                .anyMatch(position -> gameState.getBoard().getFieldAt(position) instanceof Passenger);
        final boolean mustReachSpeed = destinationField instanceof Goal || collectPassenger;
        final int destinationSpeed = gameState.getBoard().isCounterCurrent(destination) ? 2 : 1;

        int pathIndex = 1 /* skip start position */;

        AtomicInteger coal = new AtomicInteger(Math.min(playerShip.getCoal(), 1));
        AtomicInteger freeTurns = new AtomicInteger(playerShip.getFreeTurns());

        while(move.getTotalCost() < maxMovementPoints && pathIndex < path.size()) {
            final int availablePoints = maxMovementPoints - move.getTotalCost();
            final Vector3 position = move.getEndPosition();
            final Direction currentDirection = move.getEndDirection();
            final Direction direction = Direction.fromVector3(path.get(pathIndex).copy().subtract(position));

            // turn if necessary
            if(direction != currentDirection) {
                final Direction nearest = getNearestPossibleDirection(currentDirection, direction, freeTurns, coal);

                move.turn(nearest);

                if(nearest != direction)
                    break;
            }

            // move forward
            boolean wasCounterCurrent = false;
            Vector3 lastPosition = position;
            int distance = 0, points = 0;

            while(pathIndex < path.size()) {
                final Vector3 nextPosition = path.get(pathIndex);

                // stop if the direction changes
                if(!nextPosition.copy().subtract(lastPosition).equals(direction.toVector3()))
                    break;

                final boolean isCounterCurrent = gameState.getBoard().isCounterCurrent(nextPosition);
                final int requiredPoints = isCounterCurrent && !wasCounterCurrent ? 2 : 1;

                if(availablePoints - points < requiredPoints)
                    break;

                wasCounterCurrent = isCounterCurrent;

                if(mustReachSpeed) {
                    final boolean accelerate = destinationSpeed > playerShip.getSpeed();
                    final int fieldsToDestination = path.size() - pathIndex;
                    final int maxSpeed;

                    if(accelerate)
                        maxSpeed = Math.max(destinationSpeed - (fieldsToDestination - 1), playerShip.getSpeed());
                    else
                        maxSpeed = Math.min(fieldsToDestination - (destinationSpeed - 1), playerShip.getSpeed());

                    if (move.getTotalCost() + points + requiredPoints > maxSpeed)
                        break;
                }

                distance++;
                points += requiredPoints;

                lastPosition = nextPosition;
                pathIndex++;
            }

            if(distance > 0) {
                final AdvanceInfo advanceInfo = gameState.getAdvanceLimit(
                        position,
                        direction,
                        0,
                        points,
                        move.getTotalCost() > playerShip.getSpeed() ? 1 : 0,
                        coal.get()
                );

                gameState.appendForwardMove(
                        advanceInfo.getEndPosition(position, direction),
                        direction,
                        move,
                        advanceInfo,
                        availablePoints - advanceInfo.getCost()
                );
            } else
                break;
        }

        if(move.getTotalCost() < getMinMovementPoints(gameState))
            return Optional.empty();

        pathIndex = path.indexOf(move.getEndPosition());

        // turn to reach the next position if there are free turns left
        if(freeTurns.get() > 0 && pathIndex < path.size() - 1) {
            final Vector3 position = path.get(pathIndex + 1);
            final Direction direction = Direction.fromVector3(position.copy().subtract(move.getEndPosition()));

            if(direction != move.getEndDirection())
                move.turn(getNearestPossibleDirection(move.getEndDirection(), direction, freeTurns, new AtomicInteger(0)));
        }

        addAcceleration(playerShip, move);

        return Optional.of(move);
    }

    /**
     * @param from the current direction
     * @param to the target direction
     * @param freeTurns the amount of free turns
     * @param coal the available coal
     * @return the nearest possible direction to the target direction
     */
    private static Direction getNearestPossibleDirection(@NonNull Direction from,
                                                         @NonNull Direction to,
                                                         @NonNull AtomicInteger freeTurns,
                                                         @NonNull AtomicInteger coal) {
        int rotations = from.delta(to), turnCost = Math.abs(rotations);

        final int rotationsToDrop = Math.max(0, turnCost - freeTurns.get() - coal.get());
        rotations += rotationsToDrop * ((rotations < 0) ? 1 : -1);

        freeTurns.set(Math.min(0, turnCost - freeTurns.get()));
        coal.set(Math.min(0, turnCost - freeTurns.get() - coal.get()));

        return from.rotate(rotations);
    }

}
