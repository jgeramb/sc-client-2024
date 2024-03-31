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
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
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
    public static Optional<Move> getMostEfficientMove(@NonNull GameState gameState, int timeout) {
        final long startMillis = System.currentTimeMillis();

        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final int turn = gameState.getTurn();
        final boolean isEnemyAhead = isEnemyAhead(board, playerShip.getPosition(), playerShip.getDirection(), enemyShip.getPosition());
        final Vector3 playerPosition = playerShip.getPosition(), enemyPosition = enemyShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final int speed = playerShip.getSpeed();
        final int freeTurns = playerShip.getFreeTurns();
        final int coal = playerShip.getCoal();

        final Map<Move, Double> moves = getPossibleMoves(gameState, turn, playerShip, playerPosition, direction, enemyShip, enemyPosition,
                speed, freeTurns, coal, isEnemyAhead ? 1 : 0, false);

        if(moves.isEmpty()) {
            final Map<Move, Double> forcedMoves = getPossibleMoves(gameState, turn, playerShip, playerPosition, direction, enemyShip, enemyPosition,
                    speed, freeTurns, coal, Math.max(0, coal - 1), true);

            if (forcedMoves.isEmpty())
                return Optional.empty();

            moves.putAll(forcedMoves);
        }

        final Map<Move, Map<Move, Double>> nextMoves = new HashMap<>();
        final Move bestMove = moves
                .entrySet()
                .stream()
                .filter(entry -> {
                    // skip next move calculation if the time is running out
                    if(System.currentTimeMillis() - startMillis > timeout)
                        return true;

                    final Move move = entry.getKey();
                    final int remainingCoal = coal - move.getCoalCost(direction, speed, freeTurns);

                    final Map<Move, Double> currentNextMoves = getNextPossibleMoves(
                            gameState, turn,
                            playerShip, enemyShip,
                            null, remainingCoal, move
                    );

                    if(currentNextMoves.isEmpty() && doesNotEndAtLastSegmentBorder(board, move, remainingCoal))
                        return false;

                    // subtract the minimum amount of coal required for the next move from the score

                    currentNextMoves
                            .entrySet()
                            .stream()
                            .min(Comparator.comparingDouble(Map.Entry::getValue))
                            .ifPresent(bestNextMove -> entry.setValue(entry.getValue() + bestNextMove.getValue()));

                    nextMoves.put(move, currentNextMoves);

                    return true;
                })
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        if(bestMove != null) {
            if(nextMoves.containsKey(bestMove)) {
                final Direction endDirection = bestMove.getEndDirection();
                int turnCost = 0;
                Direction current = direction;

                for (Action action : bestMove.getActions()) {
                    if (action instanceof Turn turnAction) {
                        turnCost += current.costTo(turnAction.getDirection());
                        current = turnAction.getDirection();
                    }
                }

                // turn to the best direction if the ship was not turned yet
                if (turnCost < freeTurns && doesNotEndAtLastSegmentBorder(board, bestMove, 0)) {
                    Direction bestDirection = endDirection;
                    double bestScore = Integer.MIN_VALUE;

                    for (Map.Entry<Move, Double> nextMoveEntry : nextMoves.get(bestMove).entrySet()) {
                        final Move nextMove = nextMoveEntry.getKey();
                        final int firstActionIndex = nextMove.getAcceleration(bestMove.getTotalCost()) == 0 ? 0 : 1;
                        final Direction nextDirection = nextMove.getActions().get(firstActionIndex) instanceof Turn turnAction
                                ? turnAction.getDirection()
                                : endDirection;

                        if (nextMoveEntry.getValue() > bestScore) {
                            bestDirection = nextDirection;
                            bestScore = nextMoveEntry.getValue();
                        }
                    }

                    if (bestDirection != endDirection)
                        bestMove.getActions().add(ActionFactory.turn(endDirection.rotateTo(bestDirection, freeTurns - turnCost)));
                }
            }

            return Optional.of(bestMove);
        }

        // loose intentionally if the ship will be stuck in the next 2 rounds
        return Optional.of(moves.keySet().iterator().next());
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
     * @param isEnemyAhead whether the enemy is ahead of the player
     * @param availableCoal the amount of coal available after the move
     * @param move the move to evaluate
     * @return the score of the move
     */
    public static double evaluateMove(@NonNull Board board,
                                      int turn,
                                      @NonNull Ship ship, boolean isEnemyAhead, int availableCoal,
                                      @NonNull Move move) {
        final Vector3 position = move.getEndPosition();
        final Direction direction = move.getEndDirection();
        final Vector3 nextPosition = position.copy().add(direction.toVector3());
        final double segmentDistance = getMoveSegmentDistance(board, ship, move);
        final int coalCost = Math.max(0, ship.getCoal() - availableCoal - (turn < 2 ? 1 : 0));

        return (move.isGoal() ? 100 : 0)
                + (isEnemyAhead && segmentDistance >= 0 ? 50 : 0)
                + move.getPassengers() * Math.max(0, 15 - coalCost * 5)
                + segmentDistance
                - coalCost * 1.5
                + (board.getNextFieldsPositions().contains(nextPosition) ? 5 : 0)
                - getSegmentDirectionCost(board, position, direction) * 0.5;
    }

    /**
     * @param gameState the current game state
     * @param turn the current turn
     * @param ship the player's ship
     * @param position the player's position
     * @param enemyShip the enemy's ship
     * @param enemyPosition the enemy's position
     * @param speed the player's speed
     * @param freeTurns the player's free turns
     * @param coal the available coal
     * @param extraCoal the extra coal to use
     * @param forceMultiplePushes whether to force multiple pushes if possible
     * @return all possible moves for the current game state
     */
    public static Map<Move, Double> getPossibleMoves(@NonNull GameState gameState, int turn,
                                                     @NonNull Ship ship, @NonNull Vector3 position, @NonNull Direction direction,
                                                     @NonNull Ship enemyShip, @NonNull Vector3 enemyPosition,
                                                     int speed, int freeTurns, int coal,
                                                     int extraCoal,
                                                     boolean forceMultiplePushes) {
        final Board board = gameState.getBoard();
        final int turnCoal = Math.min(coal, 1 + extraCoal);
        final int accelerationCoal = getAccelerationCoal(board, turn, position, direction, enemyPosition, coal - turnCoal);
        final Set<Move> moves = board.getMoves(ship, position, direction, enemyShip, enemyPosition,
                speed, freeTurns, 1, turnCoal, accelerationCoal, forceMultiplePushes);

        // if no moves are possible, try moves that require more coal
        if(moves.isEmpty() && extraCoal < coal - 1) {
            return getPossibleMoves(gameState, turn, ship, position, direction, enemyShip, enemyPosition,
                    speed, freeTurns, coal, extraCoal + 1, forceMultiplePushes);
        }

        moves.forEach(move -> addAcceleration(speed, move));

        return moves.stream().collect(HashMap::new, (map, move) -> map.put(move, evaluateMove(
                board,
                turn,
                ship,
                isEnemyAhead(board, position, direction, enemyPosition),
                coal - move.getCoalCost(direction, speed, 1),
                move
        )), HashMap::putAll);
    }

    /**
     * @param gameState the current game state
     * @param turn the current turn
     * @param ship the player's ship
     * @param enemyShip the enemy's ship
     * @param previousMove the previous move, may be null
     * @param coal the available coal
     * @param move the current move
     * @return all possible moves for the next turn
     */
    public static Map<Move, Double> getNextPossibleMoves(@NonNull GameState gameState, int turn,
                                                         @NonNull Ship ship, @NonNull Ship enemyShip,
                                                         Move previousMove, int coal,
                                                         @NonNull Move move) {
        final boolean hasPreviousMove = previousMove != null;
        final int coalCost = move.getCoalCost(
                hasPreviousMove ? previousMove.getEndDirection() : ship.getDirection(),
                hasPreviousMove ? previousMove.getTotalCost() : ship.getSpeed(),
                hasPreviousMove ? 1 : ship.getFreeTurns()
        );
        final int remainingCoal = coal - coalCost;

        final Map<Move, Double> moves = getPossibleMoves(
                gameState,
                turn + 1,
                ship,
                move.getEndPosition(),
                move.getEndDirection(),
                enemyShip,
                move.getEnemyEndPosition(),
                move.getTotalCost(),
                1,
                remainingCoal,
                0,
                false
        );

        if(!hasPreviousMove) {
            moves.entrySet().removeIf(entry -> {
                final Move nextMove = entry.getKey();

                if (doesNotEndAtLastSegmentBorder(gameState.getBoard(), nextMove, remainingCoal)) {
                    final Map<Move, Double> nextMoves = getNextPossibleMoves(gameState, turn, ship, enemyShip, move, remainingCoal, nextMove);
                    final Move bestNextMove = nextMoves
                            .entrySet()
                            .stream()
                            .max(Comparator.comparingDouble(Map.Entry::getValue))
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    if (bestNextMove == null)
                        return true;

                    final int extraPoints = (bestNextMove.isGoal() ? 25 : 0) + bestNextMove.getPassengers() * 5;
                    final double nextMinCoalCost = nextMoves.keySet()
                            .stream()
                            .mapToInt(next -> next.getCoalCost(move.getEndDirection(), move.getTotalCost(), 1))
                            .min()
                            .orElse(0);

                    entry.setValue(entry.getValue() - nextMinCoalCost + extraPoints);
                }

                return false;
            });
        }

        return moves;
    }

    /**
     * @param board the game board
     * @param move the move to evaluate
     * @param coal the available coal
     * @return whether the move ends on a goal or in front of a field that will be revealed in the next round
     */
    public static boolean doesNotEndAtLastSegmentBorder(@NonNull Board board, @NonNull Move move, int coal) {
        final boolean willNextSegmentBeGeneratedInRange = Arrays.stream(Direction.values())
                .filter(nextDirection -> move.getEndDirection().costTo(nextDirection) <= 1 + coal)
                .anyMatch(nextDirection -> board.getNextFieldsPositions().contains(move.getEndPosition().copy().add(nextDirection.toVector3())));

        return !(move.isGoal() || willNextSegmentBeGeneratedInRange);
    }

    /**
     * Adds an acceleration action to the given move if necessary.
     * @param speed the ship's speed
     * @param move the move to add the acceleration action to
     */
    public static void addAcceleration(int speed, @NonNull Move move) {
        final int acceleration = move.getAcceleration(speed);

        if(acceleration != 0)
            move.getActions().add(0, ActionFactory.changeVelocity(acceleration));
    }

    /**
     * @param board the current game board
     * @param turn the current turn
     * @param position the player's position
     * @param direction the player's direction
     * @param enemyPosition the enemy's position
     * @param coal the available coal
     * @return the amount of coal to use for acceleration
     */
    public static int getAccelerationCoal(@NonNull Board board, int turn,
                                          @NonNull Vector3 position, @NonNull Direction direction,
                                          @NonNull Vector3 enemyPosition,
                                          int coal) {
        final boolean isEnemyAhead = isEnemyAhead(board, position, direction, enemyPosition);

        return Math.min(coal, (isEnemyAhead || turn < 2) ? 1 : 0);
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

        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Move move = new Move(path.get(0), enemyShip.getPosition(), playerShip.getDirection());

        final Vector3 destination = path.get(path.size() - 1);
        final Field destinationField = board.getFieldAt(destination);
        final boolean mustReachSpeed = destinationField instanceof Goal || board.canPickUpPassenger(destination);
        final int destinationSpeed = board.isCounterCurrent(destination) ? 2 : 1;

        int freeTurns = playerShip.getFreeTurns();
        int turnCoal = Math.min(playerShip.getCoal(), 1);
        int accelerationCoal = getAccelerationCoal(
                board,
                gameState.getTurn(),
                playerShip.getPosition(),
                playerShip.getDirection(),
                enemyShip.getPosition(),
                playerShip.getCoal() - turnCoal
        );
        int pathIndex = 1 /* skip start position */;

        final int minReachableSpeed = Math.max(1, gameState.getMinMovementPoints(playerShip) - accelerationCoal);
        final int maxReachableSpeed = Math.min(6, gameState.getMaxMovementPoints(playerShip) + accelerationCoal);

        while(move.getTotalCost() < maxReachableSpeed && pathIndex <= path.size() - 1) {
            final Vector3 position = move.getEndPosition();
            final Direction currentDirection = move.getEndDirection();
            final Direction direction = Direction.fromVector3(path.get(pathIndex).copy().subtract(position));

            // turn if necessary
            if(direction != currentDirection) {
                final Direction nearest = currentDirection.rotateTo(direction, freeTurns + turnCoal);
                final int cost = currentDirection.costTo(nearest);

                turnCoal -= Math.max(0, cost - freeTurns);
                freeTurns = Math.max(0, freeTurns - cost);

                move.turn(nearest);

                if(nearest != direction)
                    break;
            }

            // move forward
            final int availablePoints = maxReachableSpeed - move.getTotalCost();
            boolean wasCounterCurrent = false;
            Vector3 lastPosition = position, enemyPosition = move.getEnemyEndPosition().copy();
            int distance = 0, forwardCost = 0;

            while(pathIndex < path.size() && forwardCost < availablePoints) {
                final Vector3 nextPosition = path.get(pathIndex);

                // stop if the direction changes
                if(!nextPosition.copy().subtract(lastPosition).equals(direction.toVector3()))
                    break;

                final boolean isCounterCurrent = board.isCounterCurrent(nextPosition);
                int moveCost = !wasCounterCurrent && isCounterCurrent ? 2 : 1;
                Direction bestPushDirection = null;

                if(nextPosition.equals(enemyPosition)) {
                    bestPushDirection = board.getBestPushDirection(direction, enemyShip, enemyPosition, false);

                    if(bestPushDirection == null)
                        break;

                    moveCost++;
                }

                if(forwardCost + moveCost > availablePoints)
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

                if(bestPushDirection != null)
                    enemyPosition.add(bestPushDirection.toVector3());

                distance++;
                forwardCost += moveCost;

                wasCounterCurrent = isCounterCurrent;
                lastPosition = nextPosition;

                pathIndex++;
            }

            if(distance > 0) {
                final AdvanceInfo advanceInfo = board.getAdvanceLimit(
                        playerShip,
                        position,
                        direction,
                        move.getEnemyEndPosition(),
                        minReachableSpeed,
                        move.getTotalCost(),
                        forwardCost
                );

                board.appendForwardMove(
                        advanceInfo.getEndPosition(position, direction),
                        direction,
                        gameState.getEnemyShip(),
                        move,
                        advanceInfo,
                        availablePoints,
                        false
                );

                pathIndex = path.subList(0, pathIndex).lastIndexOf(move.getEndPosition()) + 1;
            } else
                break;
        }

        if(move.getTotalCost() < minReachableSpeed)
            return Optional.empty();

        // turn to reach the next position if there are available free turns
        if(freeTurns > 0 && pathIndex < path.size()) {
            final Vector3 position = path.get(pathIndex);
            final Direction direction = Direction.fromVector3(position.copy().subtract(move.getEndPosition()));

            if(direction != move.getEndDirection())
                move.turn(move.getEndDirection().rotateTo(direction, freeTurns));
        }

        addAcceleration(playerShip.getSpeed(), move);

        return Optional.of(move);
    }

    /**
     * @param board the game board
     * @param ship the player's ship
     * @param move the move to evaluate
     * @return the distance between the ship and the end of the move
     */
    public static double getMoveSegmentDistance(Board board, Ship ship, Move move) {
        final Vector3 position = ship.getPosition();

        return (move.getSegmentIndex() - board.getSegmentIndex(position)) + (move.getSegmentColumn() - board.getSegmentColumn(position)) / 4d;
    }

    /**
     * @param board the game board
     * @param position the ship's position
     * @param direction the ship's direction
     * @return the cost to reach the direction of the next segment
     */
    public static int getSegmentDirectionCost(@NonNull Board board, @NonNull Vector3 position, @NonNull Direction direction) {
        final int segmentIndex = board.getSegmentIndex(position);

        return segmentIndex < board.getSegments().size() - 1
                ? board.getSegments().get(segmentIndex + 1).direction().costTo(direction)
                : 0;
    }

    /**
     * @param board the game board
     * @param playerPosition the player's position
     * @param playerDirection the player's direction
     * @param enemyPosition the enemy's position
     * @return whether the enemy is ahead of the player
     */
    public static boolean isEnemyAhead(@NonNull Board board,
                                       @NonNull Vector3 playerPosition, @NonNull Direction playerDirection,
                                       @NonNull Vector3 enemyPosition) {
        final double segmentDistance = board.getSegmentDistance(playerPosition, enemyPosition);
        final int requiredTurns = getSegmentDirectionCost(board, playerPosition, playerDirection);

        return segmentDistance >= (2 - requiredTurns * 0.125);
    }

}
