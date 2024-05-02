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

                    if(entry.getValue() < -3)
                        return false;

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
                        entry.setValue(entry.getValue() + 1.0);

                        if(move.getSegmentIndex() < 7)
                            bestNextDirections.put(move, current.rotateTo(board.getNextSegmentDirection(), leftFreeTurns));
                    } else {
                        // update the score of the move based on the next move
                        entry.setValue(entry.getValue() + bestNextScore * 0.75);

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
     *     <li>whether the player has enough passengers and has pushed the enemy ship</li>
     *     <li>whether the enemy can collect the passenger before the player can reach it</li>
     *     <li>whether the move prevents the enemy from picking up a passenger or ending the game</li>
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
        final boolean hasEnoughPassengers = ship.hasEnoughPassengers();
        final boolean canEnemyWinByDistance = isEnemyAhead && move.getSegmentIndex() < 5;
        final boolean shouldMoveTowardsGoal = canEnemyWinByDistance || (enemyShip.hasEnoughPassengers() && passengers >= 2);
        final double segmentDistance = getMoveSegmentDistance(board, ship, move);
        final int coalCost = Math.max(0, coalBefore - coalAfter - (turn < 2 ? 1 : 0));

        boolean preventsGoal = false, preventsPassenger = false;

        if(move.getPushes() > 0) {
            final Vector3 enemyEndPosition = move.getEnemyEndPosition();

            if(board.canPickUpPassenger(enemyShip.getPosition()) && !board.canPickUpPassenger(enemyEndPosition))
                preventsPassenger = true;

            final int previousRequiredSpeed = board.isCounterCurrent(enemyShip.getPosition()) ? 2 : 1;

            if(board.getGoalFields().containsKey(enemyShip.getPosition()) && enemyShip.getSpeed() == previousRequiredSpeed) {
                final int requiredSpeed = board.isCounterCurrent(enemyEndPosition) ? 2 : 1;

                if(!board.getGoalFields().containsKey(enemyEndPosition) || enemyShip.getSpeed() != requiredSpeed)
                    preventsGoal = true;
            }
        }

        boolean canEnemyCollectPassengerBeforePlayer = false;

        if(move.getPassengers() > 0) {
            try {
                Direction.fromVector3(move.getEndPosition().copy().subtract(ship.getPosition()));
            } catch (IllegalArgumentException ignored) {
                // proceed if the direction is invalid, meaning the player needs at least 2 turns to reach the passenger
                canEnemyCollectPassengerBeforePlayer = canEnemyCollectPassenger(board, enemyShip, move.getEnemyEndPosition(), move.getEndPosition());
            }
        }

        final double passengersToInclude = shouldMoveTowardsGoal || canEnemyCollectPassengerBeforePlayer ? 0.25 : move.getPassengers();

        return (move.isGoal() || preventsGoal ? 100 : 0)
                + (preventsPassenger ? 0.75 : 0)
                + passengersToInclude * Math.max(0, 3 - passengers) * 4
                + segmentDistance * (shouldMoveTowardsGoal ? 5 : (hasEnoughPassengers ? 2 : 1)) * (turn > 45 ? 2.5 : 1)
                - coalCost * (hasEnoughPassengers ? 1 : 1.5)
                - getSegmentDirectionCost(board, move.getEndPosition(), move.getEndDirection()) * 0.75
                - Math.max(0, move.getTotalCost() - 2) * Math.max(0, move.getSegmentIndex() - 5) * 0.375
                + board.getMinTurns(enemyShip.getDirection(), move.getEnemyEndPosition()) * (move.getPushes() > 0 ? 0.25 : 0)
                + move.getPushes() * (hasEnoughPassengers ? 0.5 : 0);
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
        final boolean hasEnemyMorePoints = enemyShip.getPassengers() >= ship.getPassengers()
                && board.getSegmentDistance(ship.getPosition(), enemyShip.getPosition()) >= 1.25;
        final double segmentDirectionCost = getSegmentDirectionCost(board, position, direction);
        final int accelerationCoal = getAccelerationCoal(segmentDirectionCost, isEnemyAhead || hasEnemyMorePoints, coal - extraCoal);
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
                if(entry.getValue() < -3.5)
                    return true;

                final Move nextMove = entry.getKey();

                if (endsAtLastSegmentBorder(board, nextMove, remainingCoal))
                    entry.setValue(entry.getValue() + 1.0);
                else {
                    final Map.Entry<Move, Double> bestNextMove = getBestNextMove(board, newTurn, ship, enemyShip, move, remainingCoal, nextMove);

                    if (bestNextMove == null)
                        return true;

                    entry.setValue(entry.getValue() + bestNextMove.getValue() * 0.375);
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
     * Allows more coal usage if the enemy is ahead or the player likely needs to turn 180° in the next round.
     *
     * @param coal the available coal
     * @return the amount of coal to use for acceleration
     */
    public static int getAccelerationCoal(double segmentDirectionCost, boolean isEnemyAhead, int coal) {
        return Math.min(coal, isEnemyAhead || segmentDirectionCost == 3 ? 1 : 0);
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
        final boolean isEnemyAhead = isEnemyAhead(board, playerShip.getPosition(), playerShip.getDirection(), enemyShip, enemyShip.getPosition());
        final boolean hasEnemyMorePoints = enemyShip.getPassengers() >= playerShip.getPassengers()
                && board.getSegmentDistance(playerShip.getPosition(), enemyShip.getPosition()) >= 1.25;
        final Move move = new Move(path.get(0), enemyShip.getPosition(), playerShip.getDirection());

        final int maxIndex = path.size() - 1;
        final Vector3 destination = path.get(maxIndex);
        final Field destinationField = board.getFieldAt(destination);
        final boolean mustReachSpeed = destinationField instanceof Goal || board.canPickUpPassenger(destination);
        final int maxVelocity = getMaxVelocity(board, path);
        final int segmentDirectionCost = getSegmentDirectionCost(board, playerShip.getPosition(), playerShip.getDirection());
        final int accelerationCoal = getAccelerationCoal(segmentDirectionCost, isEnemyAhead || hasEnemyMorePoints, playerShip.getCoal());

        int freeTurns = playerShip.getFreeTurns();
        int coal = Math.min(playerShip.getCoal(), accelerationCoal + 1);
        int pathIndex = 1 /* skip start position */;

        boolean wasCounterCurrent = false;

        while(pathIndex <= maxIndex) {
            final int accelerationCost = Math.max(0, move.getTotalCost() - playerShip.getSpeed());
            final int minReachableSpeed = Math.max(1, gameState.getMinMovementPoints(playerShip) - coal);
            final int initialPathIndex = pathIndex;
            final Vector3 position = move.getEndPosition();
            final Direction currentDirection = move.getEndDirection();
            final Direction direction = Direction.fromVector3(path.get(pathIndex).copy().subtract(position));

            // turn if necessary
            if(direction != currentDirection) {
                final Direction nearest = currentDirection.rotateTo(direction, freeTurns + Math.max(0, coal - accelerationCost));
                final int cost = currentDirection.costTo(nearest);

                coal -= Math.max(0, cost - freeTurns);
                freeTurns = Math.max(0, freeTurns - cost);

                if(nearest != currentDirection)
                    move.turn(nearest);

                if(nearest != direction)
                    break;

                wasCounterCurrent = false;
            }

            final double segmentSpeedWithdrawing = Math.max(0, move.getSegmentIndex() - 4) * 0.5;
            final int maxReachableSpeed = (int) Math.min(Math.ceil(6 - segmentSpeedWithdrawing), gameState.getMaxMovementPoints(playerShip) + coal);

            if(move.getTotalCost() == maxReachableSpeed)
                break;

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

                if(mustReachSpeed && move.getTotalCost() + forwardCost + moveCost > maxVelocity)
                    break;

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

                pathIndex = path.subList(0, pathIndex).lastIndexOf(move.getEndPosition()) + 1;

                if(result.equals(AdvanceInfo.Result.SHIP))
                    wasCounterCurrent = false;
                else if(result.equals(AdvanceInfo.Result.PASSENGER) || result.equals(AdvanceInfo.Result.GOAL))
                    break;
            } else {
                pathIndex = initialPathIndex;
                break;
            }
        }

        if(move.getTotalCost() < Math.max(1, gameState.getMinMovementPoints(playerShip) - coal))
            return Optional.empty();

        // turn to reach the next position if there are available free turns
        if(freeTurns > 0 && pathIndex <= maxIndex) {
            final Direction direction = Direction.fromVector3(path.get(pathIndex).copy().subtract(move.getEndPosition()));

            if(direction != move.getEndDirection())
                move.turn(move.getEndDirection().rotateTo(direction, freeTurns));
        }

        addAcceleration(playerShip.getSpeed(), move);

        return Optional.of(move);
    }

    /**
     * @param board the game board
     * @param path the path to evaluate
     * @return the maximum velocity to reach the given path
     */
    public static int getMaxVelocity(@NonNull Board board, @NonNull List<Vector3> path) {
        int totalPathCost = 0;
        boolean onCurrent = false;

        for (int i = 1; i < path.size(); i++) {
            boolean nextIsCurrent = board.isCounterCurrent(path.get(i));

            totalPathCost += !onCurrent && nextIsCurrent ? 2 : 1;
            onCurrent = nextIsCurrent;
        }

        int changeVelocityFields = 0;
        int maxVelocity = 6;

        for (int speed = 1; speed <= 6; speed++) {
            changeVelocityFields += speed;

            if(changeVelocityFields > totalPathCost) {
                maxVelocity = speed - 1;
                break;
            }
        }

        return maxVelocity;
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
     * @return the cost to reach the direction of the next segment (1 per turn)
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

        return segmentDistance >= (2.75 - requiredTurns * 0.125 - (enemyShip.getSpeed() / 4d));
    }

    /**
     * @param board the game board
     * @param enemyShip the enemy's ship
     * @param enemyPosition the enemy's position
     * @param collectPosition the position to collect the passenger
     * @return whether the enemy can collect the passenger in the next round
     */
    public static boolean canEnemyCollectPassenger(Board board, Ship enemyShip, Vector3 enemyPosition, Vector3 collectPosition) {
        try {
            final Direction enemyDirection = Direction.fromVector3(enemyPosition.copy().subtract(collectPosition));
            final int turnCost = enemyShip.getDirection().costTo(enemyDirection);
            final int coal = Math.min(2, enemyShip.getCoal());

            if (turnCost <= 1 + coal) {
                final int remainingCoal = coal - Math.max(0, turnCost - 1);
                final int minReachableSpeed = enemyShip.getSpeed() - 1 - remainingCoal;
                final int requiredSpeed = board.isCounterCurrent(collectPosition) ? 2 : 1;

                if (minReachableSpeed <= requiredSpeed)
                    return true;
            }
        } catch (IllegalArgumentException ignored) {
            // ignore invalid directions
        }

        return false;
    }

}
