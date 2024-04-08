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
     * The move is evaluated by its score and the scores of the next two moves.
     *
     * @param gameState the current game state
     * @return the most efficient move
     */
    public static Optional<Move> getMostEfficientMove(@NonNull GameState gameState, int timeout) {
        final long startMillis = System.currentTimeMillis();

        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final int turn = gameState.getTurn();
        final Vector3 playerPosition = playerShip.getPosition(), enemyPosition = enemyShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final int passengers = playerShip.getPassengers();
        final int speed = playerShip.getSpeed();
        final int freeTurns = playerShip.getFreeTurns();
        final int coal = playerShip.getCoal();

        final Map<Move, Double> moves = getPossibleMoves(board, turn, playerShip, playerPosition, direction, enemyShip, enemyPosition,
                passengers, speed, freeTurns, coal, 0, false);

        if(moves.isEmpty()) {
            Map<Move, Double> forcedMoves = getPossibleMoves(board, turn, playerShip, playerPosition, direction, enemyShip, enemyPosition,
                    passengers, speed, freeTurns, coal, Math.max(0, coal - 1), true);

            if(forcedMoves.isEmpty())
                return Optional.empty();

            moves.putAll(forcedMoves);
        }

        final Map<Move, Direction> bestNextDirections = new HashMap<>();
        final Map<Move, Integer> newFreeTurns = new HashMap<>();
        final Move bestMove = moves
                .entrySet()
                .stream()
                .filter(entry -> {
                    // skip next move calculation if the time is running out
                    if(System.currentTimeMillis() - startMillis > timeout)
                        return true;

                    final Move move = entry.getKey();

                    if(move.isGoal())
                        return true;

                    int turnCost = 0;
                    Direction current = direction;

                    for (Action action : move.getActions()) {
                        if (action instanceof Turn turnAction) {
                            turnCost += current.costTo(turnAction.getDirection());
                            current = turnAction.getDirection();
                        }
                    }

                    final int leftFreeTurns = Math.max(0, freeTurns - turnCost);

                    newFreeTurns.put(move, leftFreeTurns);

                    // determine the best next direction

                    Direction bestNextDirection = null;
                    double bestNextScore = Integer.MIN_VALUE;
                    int bestNextCoal = Integer.MAX_VALUE;

                    for (int i = -leftFreeTurns; i <= leftFreeTurns; i++) {
                        final Direction possibleDirection = current.rotate(i);
                        final Move expandedMove = move.copy();

                        if(possibleDirection != current)
                            expandedMove.turn(possibleDirection);

                        final Map.Entry<Move, Double> bestNextMoveEntry = getBestNextMove(
                                board, turn + 1,
                                playerShip, enemyShip,
                                null, coal, expandedMove
                        );

                        if(bestNextMoveEntry == null || bestNextMoveEntry.getValue() < bestNextScore)
                            continue;

                        final Move bestNextMove = bestNextMoveEntry.getKey();
                        final boolean changedVelocity = bestNextMove.getTotalCost() == expandedMove.getTotalCost();
                        final Action firstAction = bestNextMove.getActions().get(changedVelocity ? 0 : 1);

                        // skip if the ship turns back to the first direction
                        if(firstAction instanceof Turn firstTurn && firstTurn.getDirection() == current)
                            continue;

                        final int nextCoalCost = bestNextMove.getCoalCost(possibleDirection, move.getTotalCost(), 1);

                        if(bestNextMoveEntry.getValue() > bestNextScore || nextCoalCost < bestNextCoal) {
                            bestNextDirection = possibleDirection;
                            bestNextScore = bestNextMoveEntry.getValue();
                            bestNextCoal = nextCoalCost;
                        }
                    }

                    if(bestNextDirection == null)
                        return false;

                    if(endsAtLastSegmentBorder(board, move, move.getCoalCost(direction, speed, freeTurns))) {
                        // update the score of the move if it ends at the last segment border
                        entry.setValue(entry.getValue() + 2.5);

                        if(move.getSegmentIndex() < 7)
                            bestNextDirections.put(move, current.rotateTo(board.getNextSegmentDirection(), leftFreeTurns));
                    } else {
                        // update the score of the move based on the next move
                        entry.setValue(entry.getValue() + bestNextScore * 0.25);

                        bestNextDirections.put(move, bestNextDirection);
                    }

                    return true;
                })
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        if(bestMove != null) {
            // turn to the best direction if the ship was not turned yet
            if (!endsAtLastSegmentBorder(board, bestMove, 0) && bestNextDirections.containsKey(bestMove)) {
                final Direction nextDirection = bestNextDirections.get(bestMove);
                final Direction endDirection = bestMove.getEndDirection();

                if (nextDirection != endDirection)
                    bestMove.getActions().add(ActionFactory.turn(endDirection.rotateTo(nextDirection, newFreeTurns.get(bestMove))));
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
     *     <li>whether the enemy is ahead of the player</li>
     *     <li>the amount of passengers collected in relation to the initial count of passengers</li>
     *     <li>the distance between the ship and the end of the move</li>
     *     <li>the amount of coal used</li>
     *     <li>the turns required to reach the direction of the next segment</li>
     *     <li>whether the ship decelerates when approaching the goal fields</li>
     *     <li>how many turns the enemy ship needs to move when it was pushed</li>
     * </ul>
     *
     * @param board the current game board
     * @param ship the player's ship
     * @param enemyShip the enemy's ship
     * @param isEnemyAhead whether the enemy is ahead of the player
     * @param passengers the amount of passengers before the move
     * @param coalBefore the amount of coal before the move
     * @param coalAfter the amount of coal after the move
     * @param move the move to evaluate
     * @return the score of the move
     */
    public static double evaluateMove(@NonNull Board board, int turn,
                                      @NonNull Ship ship, Ship enemyShip, boolean isEnemyAhead,
                                      int passengers, int coalBefore, int coalAfter,
                                      @NonNull Move move) {
        final boolean canEnemyWinByDistance = isEnemyAhead && move.getSegmentIndex() < 5;
        final double segmentDistance = getMoveSegmentDistance(board, ship, move);
        final int coalCost = Math.max(0, coalBefore - coalAfter - (turn < 2 ? 1 : 0));

        return (move.isGoal() ? 100 : 0)
                + move.getPassengers() * Math.max(0, 3 - passengers) * (canEnemyWinByDistance ? 0 : 5) * (move.getSegmentIndex() < 5 ? 0.5 : 1)
                + segmentDistance * (canEnemyWinByDistance ? 5 : 1.25) * (turn > 45 ? 2 : 1)
                - coalCost * 1.75
                - getSegmentDirectionCost(board, move.getEndPosition(), move.getEndDirection()) * 0.5
                - move.getTotalCost() * Math.max(0, move.getSegmentIndex() - 4) * 0.25
                + (move.getPushes() > 0 ? board.getMinTurns(enemyShip.getDirection(), move.getEnemyEndPosition()) : 0);
    }

    /**
     * @param board the current game board
     * @param turn the current turn
     * @param ship the player's ship
     * @param position the player's position
     * @param enemyShip the enemy's ship
     * @param enemyPosition the enemy's position
     * @param passengers the player's passengers
     * @param speed the player's speed
     * @param freeTurns the player's free turns
     * @param coal the available coal
     * @param extraCoal the extra coal to use
     * @param forceMultiplePushes whether to force multiple pushes if possible
     * @return all possible moves for the current game state
     */
    public static Map<Move, Double> getPossibleMoves(@NonNull Board board, int turn,
                                                     @NonNull Ship ship, @NonNull Vector3 position, @NonNull Direction direction,
                                                     @NonNull Ship enemyShip, @NonNull Vector3 enemyPosition,
                                                     int passengers, int speed, int freeTurns, int coal,
                                                     int extraCoal, boolean forceMultiplePushes) {
        final boolean isEnemyAhead = isEnemyAhead(board, position, direction, enemyShip, enemyPosition);
        final int accelerationCoal = getAccelerationCoal(turn, isEnemyAhead, coal - extraCoal);
        final Set<Move> moves = board.getMoves(ship, position, direction, enemyShip, enemyPosition,
                speed, freeTurns, Math.min(coal, 1 + accelerationCoal + extraCoal), forceMultiplePushes);

        // if no moves are possible, try moves that require more coal
        if(moves.isEmpty() && extraCoal < coal - 1) {
            return getPossibleMoves(board, turn, ship, position, direction, enemyShip, enemyPosition,
                    passengers, speed, freeTurns, coal, extraCoal + 1, forceMultiplePushes);
        }

        moves.forEach(move -> addAcceleration(speed, move));

        return moves.stream().collect(HashMap::new, (map, move) -> map.put(move, evaluateMove(
                board,
                turn,
                ship,
                enemyShip,
                isEnemyAhead(board, position, direction, enemyShip, enemyPosition),
                passengers,
                coal,
                coal - move.getCoalCost(direction, speed, freeTurns),
                move
        )), HashMap::putAll);
    }

    /**
     * @param board the current game board
     * @param turn the current turn
     * @param ship the player's ship
     * @param enemyShip the enemy's ship
     * @param previousMove the previous move, may be null
     * @param coal the coal before the move
     * @param move the current move
     * @return the best next move for the given move
     */
    public static Map.Entry<Move, Double> getBestNextMove(@NonNull Board board, int turn,
                                                          @NonNull Ship ship, @NonNull Ship enemyShip,
                                                          Move previousMove, int coal,
                                                          @NonNull Move move) {
        final int newTurn = turn + 1;
        final boolean hasPreviousMove = previousMove != null;
        final int coalCost = move.getCoalCost(
                hasPreviousMove ? previousMove.getEndDirection() : ship.getDirection(),
                hasPreviousMove ? previousMove.getTotalCost() : ship.getSpeed(),
                hasPreviousMove ? 1 : ship.getFreeTurns()
        );
        final int remainingCoal = coal - coalCost;
        final int passengers = ship.getPassengers() + (hasPreviousMove ? previousMove.getPassengers() : 0) + move.getPassengers();

        Map<Move, Double> moves = getPossibleMoves(
                board, newTurn,
                ship, move.getEndPosition(), move.getEndDirection(), enemyShip, move.getEnemyEndPosition(),
                passengers, move.getTotalCost(), 1, remainingCoal, 0,
                false
        );

        if(moves.isEmpty()) {
            moves = getPossibleMoves(
                    board, newTurn,
                    ship, move.getEndPosition(), move.getEndDirection(), enemyShip, move.getEnemyEndPosition(),
                    passengers, move.getTotalCost(), 1, remainingCoal, 0,
                     true
            );

            if(moves.isEmpty())
                return null;
        }

        if(!hasPreviousMove) {
            moves.entrySet().removeIf(entry -> {
                final Move nextMove = entry.getKey();

                if (endsAtLastSegmentBorder(board, nextMove, remainingCoal))
                    entry.setValue(entry.getValue() + 2.5);
                else {
                    final Map.Entry<Move, Double> bestNextMove = getBestNextMove(board, newTurn, ship, enemyShip, move, remainingCoal, nextMove);

                    if (bestNextMove == null)
                        return true;

                    entry.setValue(entry.getValue() + bestNextMove.getValue() * 0.25);
                }

                return false;
            });
        }

        return moves.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .orElse(null);
    }

    /**
     * @param board the game board
     * @param move the move to evaluate
     * @param coal the available coal
     * @return whether the move ends on a goal or in front of a field that will be revealed in the next round.
     */
    public static boolean endsAtLastSegmentBorder(@NonNull Board board, @NonNull Move move, int coal) {
        final boolean willNextSegmentBeGeneratedInRange = Arrays.stream(Direction.values())
                .filter(nextDirection -> move.getEndDirection().costTo(nextDirection) <= 1 + coal)
                .anyMatch(nextDirection -> board.getNextFieldsPositions().contains(move.getEndPosition().copy().add(nextDirection.toVector3())));

        return move.isGoal() || willNextSegmentBeGeneratedInRange;
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
     * @param turn the current turn
     * @param coal the available coal
     * @return the amount of coal to use for acceleration
     */
    public static int getAccelerationCoal(int turn, boolean isEnemyAhead, int coal) {
        return Math.min(coal, (isEnemyAhead || turn < 2) ? 1 : 0);
    }

    /**
     * Returns the next move to reach the given path.
     * @param gameState the current game state
     * @param path the path to reach
     * @return the next move to reach the given path
     */
    public static Optional<Move> moveFromPath(@NonNull GameState gameState, @NonNull List<Vector3> path) {
        if(path.isEmpty())
            return Optional.empty();

        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Move move = new Move(path.get(0), enemyShip.getPosition(), playerShip.getDirection());

        final Vector3 destination = path.get(path.size() - 1);
        final Field destinationField = board.getFieldAt(destination);
        final boolean mustReachSpeed = destinationField instanceof Goal || board.canPickUpPassenger(destination);
        final int destinationSpeed = board.isCounterCurrent(destination) ? 2 : 1;
        int currentSpeed = playerShip.getSpeed();
        int fieldsToAccelerate = currentSpeed < destinationSpeed ? 1 : 0;

        while ((currentSpeed--) > destinationSpeed) {
            fieldsToAccelerate += currentSpeed;
        }

        int freeTurns = playerShip.getFreeTurns();
        int turnCoal = Math.min(playerShip.getCoal(), 1);
        int accelerationCoal = getAccelerationCoal(
                gameState.getTurn(),
                isEnemyAhead(board, playerShip.getPosition(), playerShip.getDirection(), enemyShip, enemyShip.getPosition()),
                playerShip.getCoal() - turnCoal
        );
        int pathIndex = 1 /* skip start position */;

        final int minReachableSpeed = Math.max(1, gameState.getMinMovementPoints(playerShip) - accelerationCoal);
        final int maxReachableSpeed = Math.min(6, gameState.getMaxMovementPoints(playerShip) + accelerationCoal);
        boolean wasCounterCurrent = false;

        while(move.getTotalCost() < maxReachableSpeed && pathIndex < path.size()) {
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

                wasCounterCurrent = false;
            }

            // move forward
            final int availablePoints = maxReachableSpeed - move.getTotalCost();
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
                    final int fieldsToDestination = path.size() - pathIndex;

                    if(fieldsToDestination <= fieldsToAccelerate) {
                        final int maxSpeed = destinationSpeed > playerShip.getSpeed()
                                ? Math.max(destinationSpeed - (fieldsToDestination - 1), playerShip.getSpeed())
                                : Math.min(fieldsToDestination- (destinationSpeed - 1), playerShip.getSpeed());

                        if (move.getTotalCost() + forwardCost + moveCost > maxSpeed)
                            break;
                    }
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

                final AdvanceInfo.Result result = board.appendForwardMove(
                        advanceInfo.getEndPosition(position, direction),
                        direction,
                        enemyShip,
                        move,
                        advanceInfo,
                        availablePoints,
                        false
                );

                if(!(result.equals(AdvanceInfo.Result.NORMAL) || result.equals(AdvanceInfo.Result.COUNTER_CURRENT)))
                    wasCounterCurrent = false;

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
        final int deltaSegmentIndex = move.getSegmentIndex() - board.getSegmentIndex(position);
        final double deltaSegmentColumn = (move.getSegmentColumn() - board.getSegmentColumn(position)) / 4d;
        final double deltaFieldColumn = (move.getEndDirection().toFieldColumn() - ship.getDirection().toFieldColumn()) / 16d;

        return deltaSegmentIndex + deltaSegmentColumn + deltaFieldColumn;
    }

    /**
     * @param board the game board
     * @param position the ship's position
     * @return the direction of the next segment
     */
    public static Direction getNextSegmentDirection(@NonNull Board board, @NonNull Vector3 position) {
        final int segmentIndex = board.getSegmentIndex(position);

        if(segmentIndex < board.getSegments().size() - 1)
            return board.getSegments().get(segmentIndex + 1).direction();

        return null;
    }

    /**
     * @param board the game board
     * @param position the ship's position
     * @param direction the ship's direction
     * @return the cost to reach the direction of the next segment
     */
    public static int getSegmentDirectionCost(@NonNull Board board, @NonNull Vector3 position, @NonNull Direction direction) {
        final Direction nextSegmentDirection = getNextSegmentDirection(board, position);

        return nextSegmentDirection == null ? 0 : direction.costTo(nextSegmentDirection);
    }

    /**
     * @param board the game board
     * @param playerPosition the player's position
     * @param playerDirection the player's direction
     * @param enemyShip the enemy's ship
     * @param enemyPosition the enemy's position
     * @return whether the enemy is ahead of the player
     */
    public static boolean isEnemyAhead(@NonNull Board board,
                                       @NonNull Vector3 playerPosition, @NonNull Direction playerDirection,
                                       @NonNull Ship enemyShip, @NonNull Vector3 enemyPosition) {
        final double segmentDistance = board.getSegmentDistance(playerPosition, enemyPosition);
        final int requiredTurns = getSegmentDirectionCost(board, playerPosition, playerDirection);

        return segmentDistance >= (3 - requiredTurns * 0.125 - (enemyShip.getSpeed() / 4d));
    }

}
