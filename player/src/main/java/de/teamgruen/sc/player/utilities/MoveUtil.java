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
import de.teamgruen.sc.sdk.logging.AnsiColor;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Goal;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MoveUtil {

    /**
     * Returns the most efficient move for the current game state.
     * @param gameState the current game state
     * @return the most efficient move
     */
    public static Optional<Move> getMostEfficientMove(@NonNull GameState gameState) {
        final int turn = gameState.getTurn();
        final Ship playerShip = gameState.getPlayerShip();
        final Vector3 position = playerShip.getPosition();
        final int playerSegmentIndex = gameState.getBoard().getSegmentIndex(position);
        final int playerSegmentColumn = gameState.getBoard().getSegmentColumn(position);
        final boolean isEnemyAhead = isEnemyAhead(gameState);
        final int coal = playerShip.getCoal();

        final List<Move> moves = getPossibleMoves(gameState, isEnemyAhead);

        Move bestMove = null;
        double highestScore = Integer.MIN_VALUE;
        int lowestCoalCost = 0;

        for(Move move : moves) {
            final int coalCost = Math.max(0, move.getCoalCost(playerShip) - (isEnemyAhead || turn < 2 ? 1 : 0));
            final OptionalInt nextMinTurns = getMinTurns(
                    gameState,
                    position,
                    move.getEndDirection(),
                    1,
                    move.getTotalCost(),
                    coal - coalCost
            );

            // skip moves that lead to a dead end
            if(nextMinTurns.isEmpty())
                continue;

            final boolean isLastColumn = move.getSegmentColumn() == 3;
            final boolean isLastSegment = move.getSegmentIndex() == gameState.getBoard().getSegments().size() - 1;
            final boolean willSegmentSpawn = isLastColumn && isLastSegment && move.getSegmentIndex() != 7;
            final int minTurns = nextMinTurns.getAsInt() - (willSegmentSpawn ? 1 : 0);

            final int enemyMinTurns = gameState.getMinTurns(gameState.getEnemyShip().getDirection(), move.getEnemyEndPosition());
            final boolean canEnd = gameState.getBoard().getFieldAt(move.getEndPosition()) instanceof Goal
                    && playerShip.hasEnoughPassengers();
            final double segmentDistance = (move.getSegmentIndex() - playerSegmentIndex) + (move.getSegmentColumn() - playerSegmentColumn) / 4d;
            final double score = (move.isGoal() ? 100 : 0)
                    + (canEnd || (isEnemyAhead && segmentDistance >= 0) ? 50 : 0)
                    + move.getPassengers() * (15 - coalCost * 5)
                    + move.getPushes() * Math.max(0, enemyMinTurns - 2) * 5
                    + segmentDistance
                    - coalCost
                    - minTurns;

            System.out.println(AnsiColor.GREEN.toString() + score + AnsiColor.RESET + ": " + AnsiColor.WHITE + move.getActions() + AnsiColor.RESET);

            if(score > highestScore || (score == highestScore && coalCost < lowestCoalCost)) {
                highestScore = score;
                bestMove = move;
                lowestCoalCost = coalCost;
            }
        }

        return Optional.ofNullable(bestMove);
    }

    /**
     * @param gameState the current game state
     * @param position the current position
     * @param direction the current direction
     * @param freeTurns the amount of free turns
     * @param speed the current speed
     * @param coal the available coal
     * @return the minimum amount of turns the ship needs to use all movement points
     */
    private static OptionalInt getMinTurns(@NonNull GameState gameState,
                                           @NonNull Vector3 position,
                                           @NonNull Direction direction,
                                           int freeTurns,
                                           int speed,
                                           int coal) {
        return Arrays.stream(Direction.values())
                .mapToInt(nextDirection -> {
                    final int cost = direction.costTo(nextDirection);
                    final int coalLeft = coal - Math.max(0, cost - freeTurns);

                    if(coalLeft < 0)
                        return -1;

                    final Vector3 nextPosition = position.copy().add(nextDirection.toVector3());
                    final int minTurns = gameState.getMinTurns(nextDirection, nextPosition);

                    if(speed > 1) {
                        final OptionalInt nextMinTurns = getMinTurns(gameState,
                                nextPosition,
                                nextDirection,
                                Math.max(0, freeTurns - (cost > 0 ? 1 : 0)),
                                speed - 1,
                                coalLeft
                        );

                        if(nextMinTurns.isEmpty())
                            return -1;
                    }

                    return minTurns;
                })
                .filter(minTurns -> minTurns != -1)
                .min();
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
                gameState.getEnemyShip().getPosition(),
                ship.getDirection(),
                ship.getFreeTurns(),
                1,
                getMinMovementPoints(gameState),
                Math.min(6, getMaxMovementPoints(gameState) + getAccelerationCoal(gameState)),
                maxCoal
        ));

        // if no moves are possible, try moves that require more coal
        if(moves.isEmpty() && maxCoal < 2 && ship.getCoal() > 1)
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
     * @return the maximum movement points
     */
    private static int getMaxMovementPoints(@NonNull GameState gameState) {
        return Math.min(6, gameState.getPlayerShip().getSpeed() + 1);
    }

    /**
     * @param gameState the current game state
     * @return the amount of coal to use for acceleration
     */
    private static int getAccelerationCoal(@NonNull GameState gameState) {
        final boolean useCoal = isEnemyAhead(gameState) || gameState.getTurn() < 2;

        return useCoal && gameState.getPlayerShip().getCoal() > 0 ? 1 : 0;
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
        final Move move = new Move(path.get(0), gameState.getEnemyShip().getPosition(), playerShip.getDirection());
        final int maxMovementPoints = Math.min(6, getMaxMovementPoints(gameState) + getAccelerationCoal(gameState));
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

        while(move.getTotalCost() < maxMovementPoints && pathIndex < path.size() - 1) {
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

            while(pathIndex < path.size() - 1) {
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

    /**
     * @param gameState the current game state
     * @return whether the enemy is at least 3 segments ahead of the player
     */
    public static boolean isEnemyAhead(GameState gameState) {
        final int playerSegmentIndex = gameState.getBoard().getSegmentIndex(gameState.getPlayerShip().getPosition());
        final int enemySegmentIndex = gameState.getBoard().getSegmentIndex(gameState.getEnemyShip().getPosition());

        return enemySegmentIndex > playerSegmentIndex + 2;
    }

}
