/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities;

import de.teamgruen.sc.sdk.game.AdvanceInfo;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.actions.Turn;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Goal;
import lombok.NonNull;

import java.util.*;

public class MoveUtil {

    /**
     * Returns the most efficient move for the current game state.
     * @param gameState the current game state
     * @return the most efficient move
     */
    public static Optional<Move> getMostEfficientMove(@NonNull GameState gameState) {
        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final boolean isEnemyAhead = isEnemyAhead(board, playerShip.getPosition(), enemyShip.getPosition());
        final List<Move> moves = getPossibleMoves(gameState, isEnemyAhead ? 1 : 0);

        Move bestMove = null;
        double highestScore = Integer.MIN_VALUE;
        int lowestCoalCost = 0;

        for(Move move : moves) {
            final int coalDiscount = (isEnemyAhead || gameState.getTurn() < 2) ? Math.max(0, move.getTotalCost() - playerShip.getSpeed()) : 0;
            final int coalCost = move.getCoalCost(playerShip.getDirection(), playerShip.getSpeed(), playerShip.getFreeTurns()) - coalDiscount;
            final int availableCoal = playerShip.getCoal() - Math.max(0, coalCost);
            final Vector3 endPosition = move.getEndPosition();
            final Direction endDirection = move.getEndDirection();
            int nextMoveMinCoalCost;

            if(!move.isGoal()) {
                final Map<Move, Double> nextMoves = gameState.getBoard()
                        .getMoves(
                                playerShip,
                                endPosition,
                                endDirection,
                                enemyShip,
                                move.getEnemyEndPosition(),
                                move.getTotalCost(),
                                1,
                                1,
                                Math.min(availableCoal, 1),
                                0
                        )
                        .stream()
                        .collect(HashMap::new, (map, nextMove) -> map.put(nextMove, evaluateMove(
                                board,
                                playerShip,
                                isEnemyAhead(board, endPosition, move.getEnemyEndPosition()),
                                nextMove,
                                availableCoal - nextMove.getCoalCost(endDirection, move.getTotalCost(), 1),
                                0
                        )), HashMap::putAll);

                // skip moves that lead to a dead end
                if (nextMoves.isEmpty())
                    continue;

                nextMoveMinCoalCost = nextMoves.entrySet()
                        .stream()
                        .min(Comparator.comparingDouble(Map.Entry::getValue))
                        .get()
                        .getKey()
                        .getCoalCost(endDirection, move.getTotalCost(), 1);

                // turn to the best direction if the ship was not turned yet
                if (move.getActions().stream().noneMatch(action -> action instanceof Turn)) {
                    Direction bestDirection = endDirection;
                    double bestScore = Integer.MIN_VALUE;

                    for(Map.Entry<Move, Double> nextMoveEntry : nextMoves.entrySet()) {
                        if(!(nextMoveEntry.getKey().getActions().get(0) instanceof Turn turn))
                            continue;

                        final Direction nextDirection = turn.getDirection();
                        final double moveScore = nextMoveEntry.getValue() - endDirection.costTo(nextDirection);

                        if(moveScore > bestScore) {
                            bestDirection = nextDirection;
                            bestScore = moveScore;
                        }
                    }

                    if(bestDirection != endDirection)
                        move.getActions().add(ActionFactory.turn(endDirection.rotateTo(bestDirection, 1)));
                }
            } else
                nextMoveMinCoalCost = 0;

            final double score = evaluateMove(board, playerShip, isEnemyAhead, move, availableCoal, nextMoveMinCoalCost);

            if(score > highestScore || (score == highestScore && coalCost < lowestCoalCost)) {
                highestScore = score;
                bestMove = move;
                lowestCoalCost = coalCost;
            }
        }

        // loose intentionally if the ship will be stuck in the next round
        if (bestMove == null && !moves.isEmpty())
            bestMove = moves.get(0);

        return Optional.ofNullable(bestMove);
    }

    /**
     * Evaluates the given move based on the following criteria:
     * <ul>
     *     <li>whether the move allows the player to end the game</li>
     *     <li>whether the enemy is at least 2 segments ahead of the player</li>
     *     <li>the amount of passengers collected in relation to the coal used</li>
     *     <li>the distance between the ship and the end of the move</li>
     *     <li>the amount of coal used</li>
     *     <li>the minimum amount of coal required for the next move in relation to the remaining coal</li>
     *     <li>whether the move ends in front of a field that will be revealed in the next round</li>
     *     <li>the turns required to reach the direction of the next segment</li>
     * </ul>
     *
     * @param board the game board
     * @param ship the player's ship
     * @param isEnemyAhead whether the enemy is at least 2 segments ahead of the player
     * @param move the move to evaluate
     * @param availableCoal the amount of coal available after the move
     * @param minNextCoalCost the minimum amount of coal required for the next move
     * @return the score of the move
     */
    public static double evaluateMove(Board board, Ship ship, boolean isEnemyAhead, Move move, int availableCoal, int minNextCoalCost) {
        final Vector3 nextPosition = move.getEndPosition().copy().add(move.getEndDirection().toVector3());
        final double segmentDistance = getSegmentDistance(board, ship, move);
        final int coalCost = ship.getCoal() - availableCoal;
        final int segmentIndex = board.getSegmentIndex(move.getEndPosition());
        final int segmentDirectionCost = segmentIndex < board.getSegments().size() - 1
                ? board.getSegments().get(segmentIndex + 1).direction().costTo(move.getEndDirection())
                : 0;

        return (move.isGoal() ? 100 : 0)
                + (isEnemyAhead && segmentDistance >= 0 ? 50 : 0)
                + move.getPassengers() * Math.max(0, 15 - coalCost * 5)
                + segmentDistance
                - (coalCost + minNextCoalCost) * 1.5
                + (board.getNextFieldsPositions().contains(nextPosition) ? 5 : 0)
                - segmentDirectionCost * 0.5;
    }

    /**
     * Returns a list of all possible moves for the current game state.
     * @param gameState the current game state
     * @param extraCoal the additional amount of coal to use for turns
     * @return a list of all possible moves
     */
    public static List<Move> getPossibleMoves(@NonNull GameState gameState, int extraCoal) {
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final int turnCoal = Math.min(playerShip.getCoal(), 1 + extraCoal);

        final List<Move> moves = new ArrayList<>(gameState.getBoard().getMoves(
                playerShip,
                playerShip.getPosition(),
                playerShip.getDirection(),
                enemyShip,
                enemyShip.getPosition(),
                playerShip.getSpeed(),
                playerShip.getFreeTurns(),
                1,
                turnCoal,
                Math.min(playerShip.getCoal() - turnCoal, getAccelerationCoal(gameState))
        ));

        // if no moves are possible, try moves that require more coal
        if(moves.isEmpty() && extraCoal < playerShip.getCoal() - 1)
            return getPossibleMoves(gameState, extraCoal + 1);

        moves.forEach(move -> addAcceleration(playerShip, move));

        return moves;
    }

    /**
     * Adds an acceleration action to the given move if necessary.
     * @param ship the player's ship
     * @param move the move to add the acceleration action to
     */
    public static void addAcceleration(@NonNull Ship ship, @NonNull Move move) {
        final int acceleration = move.getAcceleration(ship.getSpeed());

        if(acceleration != 0)
            move.getActions().add(0, ActionFactory.changeVelocity(acceleration));
    }

    /**
     * @param gameState the current game state
     * @return the amount of coal to use for acceleration
     */
    public static int getAccelerationCoal(@NonNull GameState gameState) {
        final Ship playerShip = gameState.getPlayerShip();
        final boolean isEnemyAhead = isEnemyAhead(gameState.getBoard(), playerShip.getPosition(), gameState.getEnemyShip().getPosition());

        return Math.min(playerShip.getCoal(), (isEnemyAhead || gameState.getTurn() < 2) ? 1 : 0);
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

        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Move move = new Move(path.get(0), enemyShip.getPosition(), playerShip.getDirection());

        final Vector3 destination = path.get(path.size() - 1);
        final Field destinationField = gameState.getBoard().getFieldAt(destination);
        final boolean mustReachSpeed = destinationField instanceof Goal || gameState.getBoard().canPickUpPassenger(destination);
        final int destinationSpeed = gameState.getBoard().isCounterCurrent(destination) ? 2 : 1;

        final int turnCoal = Math.min(playerShip.getCoal(), 1);
        final int accelerationCoal = Math.min(playerShip.getCoal() - turnCoal, getAccelerationCoal(gameState));
        final int maxMovementPoints = Math.min(6, gameState.getMaxMovementPoints(playerShip) + accelerationCoal);

        int pathIndex = 1 /* skip start position */;
        int coal = turnCoal + accelerationCoal;
        int freeTurns = playerShip.getFreeTurns();
        int freeAcceleration = 1;

        while(move.getTotalCost() < maxMovementPoints && pathIndex < path.size() - 1) {
            final int availablePoints = maxMovementPoints - move.getTotalCost();
            final Vector3 position = move.getEndPosition();
            final Direction currentDirection = move.getEndDirection();
            final Direction direction = Direction.fromVector3(path.get(pathIndex).copy().subtract(position));

            // turn if necessary
            if(direction != currentDirection) {
                final Direction nearest = currentDirection.rotateTo(direction, freeTurns + coal);
                final int cost = currentDirection.costTo(nearest);

                freeTurns = Math.max(0, freeTurns - cost);
                coal -= Math.max(0, cost - freeTurns);

                move.turn(nearest);

                if(nearest != direction)
                    break;
            }

            // move forward
            boolean wasCounterCurrent = false;
            Vector3 lastPosition = position, enemyPosition = move.getEnemyEndPosition().copy();
            int distance = 0, forwardCost = 0;

            while(pathIndex < path.size() && forwardCost < availablePoints) {
                final Vector3 nextPosition = path.get(pathIndex);

                // stop if the direction changes
                if(!nextPosition.copy().subtract(lastPosition).equals(direction.toVector3()))
                    break;

                final boolean isCounterCurrent = gameState.getBoard().isCounterCurrent(nextPosition);
                final int pushCost = nextPosition.equals(enemyPosition) ? 1 : 0;
                final int moveCost = isCounterCurrent && !wasCounterCurrent ? 2 : 1 + pushCost;

                if(forwardCost + moveCost < availablePoints)
                    break;

                if(mustReachSpeed) {
                    final boolean accelerate = destinationSpeed > playerShip.getSpeed();
                    final int fieldsToDestination = path.size() - pathIndex;
                    final int maxSpeed;

                    if(accelerate)
                        maxSpeed = Math.max(destinationSpeed - (fieldsToDestination - 1), playerShip.getSpeed());
                    else
                        maxSpeed = Math.min(fieldsToDestination - (destinationSpeed - 1), playerShip.getSpeed());

                    if (move.getTotalCost() + forwardCost + moveCost > maxSpeed)
                        break;
                }

                if(pushCost > 0)
                    enemyPosition.add(gameState.getBoard().getBestPushDirection(direction, enemyShip, enemyPosition).toVector3());

                distance++;
                forwardCost += moveCost;

                wasCounterCurrent = isCounterCurrent;
                lastPosition = nextPosition;

                pathIndex++;
            }

            if(distance > 0) {
                final AdvanceInfo advanceInfo = gameState.getBoard().getAdvanceLimit(
                        playerShip,
                        position,
                        direction,
                        move.getEnemyEndPosition(),
                        Math.max(1, playerShip.getSpeed() - freeAcceleration - coal),
                        move.getTotalCost(),
                        forwardCost
                );

                gameState.getBoard().appendForwardMove(
                        advanceInfo.getEndPosition(position, direction),
                        direction,
                        gameState.getEnemyShip(),
                        move,
                        advanceInfo,
                        availablePoints
                );

                if(move.getTotalCost() > 0)
                    freeAcceleration = Math.max(0, freeAcceleration - Math.abs(move.getAcceleration(playerShip.getSpeed())));
            } else
                break;
        }

        if(move.getTotalCost() < Math.max(1, gameState.getMinMovementPoints(playerShip) - playerShip.getCoal()))
            return Optional.empty();

        pathIndex = path.indexOf(move.getEndPosition());

        // turn to reach the next position if there are available free turns
        if(freeTurns > 0 && pathIndex < path.size() - 1) {
            final Vector3 position = path.get(pathIndex + 1);
            final Direction direction = Direction.fromVector3(position.copy().subtract(move.getEndPosition()));

            if(direction != move.getEndDirection())
                move.turn(move.getEndDirection().rotateTo(direction, freeTurns));
        }

        addAcceleration(playerShip, move);

        return Optional.of(move);
    }

    /**
     * @param board the game board
     * @param ship the player's ship
     * @param move the move to evaluate
     * @return the distance between the ship and the end of the move
     */
    public static double getSegmentDistance(Board board, Ship ship, Move move) {
        final Vector3 position = ship.getPosition();

        return (move.getSegmentIndex() - board.getSegmentIndex(position)) + (move.getSegmentColumn() - board.getSegmentColumn(position)) / 4d;
    }

    /**
     * @param board the game board
     * @param playerPosition the player's position
     * @param enemyPosition the enemy's position
     * @return whether the enemy is at least 2 segments ahead of the player
     */
    public static boolean isEnemyAhead(Board board, Vector3 playerPosition, Vector3 enemyPosition) {
        return board.getSegmentIndex(enemyPosition) > board.getSegmentIndex(playerPosition) + 1;
    }

}
