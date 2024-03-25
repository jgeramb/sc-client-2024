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
        final int turn = gameState.getTurn();
        final boolean isEnemyAhead = isEnemyAhead(board, playerShip.getPosition(), enemyShip.getPosition());
        final Vector3 playerPosition = playerShip.getPosition(), enemyPosition = enemyShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final int speed = playerShip.getSpeed();
        final int freeTurns = playerShip.getFreeTurns();
        final int coal = playerShip.getCoal();

        final Map<Move, Double> moves = getPossibleMoves(gameState, turn, playerShip, playerPosition, direction, enemyShip, enemyPosition, speed, freeTurns, coal, isEnemyAhead ? 1 : 0);
        final Map<Move, Map<Move, Double>> nextMoves = new HashMap<>();
        final Move bestMove = moves
                .entrySet()
                .stream()
                .filter(entry -> {
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

                    final int minNextCoalCost = currentNextMoves.keySet()
                            .stream()
                            .mapToInt(nextMove -> nextMove.getCoalCost(direction, speed, 1))
                            .min()
                            .orElse(0);

                    entry.setValue(entry.getValue() - minNextCoalCost);

                    nextMoves.put(move, currentNextMoves);

                    return true;
                })
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        if(bestMove != null) {
            final Direction endDirection = bestMove.getEndDirection();

            // turn to the best direction if the ship was not turned yet
            if (bestMove.getActions().stream().noneMatch(action -> action instanceof Turn)) {
                Direction bestDirection = endDirection;
                double bestScore = Integer.MIN_VALUE;

                for(Map.Entry<Move, Double> nextMoveEntry : nextMoves.get(bestMove).entrySet()) {
                    final Direction nextDirection = nextMoveEntry.getKey().getActions().get(0) instanceof Turn turnAction
                            ? turnAction.getDirection()
                            : endDirection;
                    final double moveScore = nextMoveEntry.getValue() - endDirection.costTo(nextDirection);

                    if(moveScore > bestScore) {
                        bestDirection = nextDirection;
                        bestScore = moveScore;
                    }
                }

                if(bestDirection != endDirection)
                    bestMove.getActions().add(ActionFactory.turn(endDirection.rotateTo(bestDirection, 1)));
            }
        } else if (!moves.isEmpty()) {
            // loose intentionally if the ship will be stuck in the next 2 rounds
            return Optional.of(moves.keySet().iterator().next());
        }

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
     * @param availableCoal the amount of coal available after the move
     * @param move the move to evaluate
     * @return the score of the move
     */
    public static double evaluateMove(@NonNull Board board,
                                      int turn,
                                      @NonNull Ship ship, boolean isEnemyAhead, int availableCoal,
                                      @NonNull Move move) {
        final Vector3 nextPosition = move.getEndPosition().copy().add(move.getEndDirection().toVector3());
        final double segmentDistance = getSegmentDistance(board, ship, move);
        final int coalCost = Math.max(0, ship.getCoal() - availableCoal - (turn < 2 ? 1 : 0));
        final int segmentIndex = board.getSegmentIndex(move.getEndPosition());
        final int segmentDirectionCost = segmentIndex < board.getSegments().size() - 1
                ? board.getSegments().get(segmentIndex + 1).direction().costTo(move.getEndDirection())
                : 0;

        return (move.isGoal() ? 100 : 0)
                + (isEnemyAhead && segmentDistance >= 0 ? 50 : 0)
                + move.getPassengers() * Math.max(0, 15 - coalCost * 5)
                + segmentDistance
                - coalCost * 1.5
                + (board.getNextFieldsPositions().contains(nextPosition) ? 5 : 0)
                - segmentDirectionCost * 0.5;
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
     * @return all possible moves for the current game state
     */
    public static Map<Move, Double> getPossibleMoves(@NonNull GameState gameState, int turn,
                                                     @NonNull Ship ship, @NonNull Vector3 position, @NonNull Direction direction,
                                                     @NonNull Ship enemyShip, @NonNull Vector3 enemyPosition,
                                                     int speed, int freeTurns, int coal,
                                                     int extraCoal) {
        final Board board = gameState.getBoard();
        final int turnCoal = Math.min(ship.getCoal(), 1 + extraCoal);
        final int accelerationCoal = Math.min(coal - turnCoal, getAccelerationCoal(board, turn, position, enemyPosition, coal));
        final Set<Move> moves = board.getMoves(ship, position, direction, enemyShip, enemyPosition,
                speed, freeTurns, 1, turnCoal, accelerationCoal);

        // if no moves are possible, try moves that require more coal
        if(moves.isEmpty() && extraCoal < coal - 1) {
            return getPossibleMoves(gameState, turn, ship, position, direction, enemyShip, enemyPosition,
                    speed, freeTurns, coal, extraCoal + 1);
        }

        moves.forEach(move -> addAcceleration(speed, move));

        return moves.stream().collect(HashMap::new, (map, move) -> map.put(move, evaluateMove(
                board,
                turn,
                ship,
                isEnemyAhead(board, position, enemyPosition),
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
                0
        );

        if(!hasPreviousMove) {
            moves.entrySet().removeIf(entry -> {
                final Move nextMove = entry.getKey();

                if (doesNotEndAtLastSegmentBorder(gameState.getBoard(), nextMove, remainingCoal)) {
                    final Map<Move, Double> nextMoves = getNextPossibleMoves(gameState, turn, ship, enemyShip, move, remainingCoal, nextMove);

                    if (nextMoves.isEmpty())
                        return true;

                    final double nextMinCoalCost = nextMoves.keySet()
                            .stream()
                            .mapToInt(next -> next.getCoalCost(move.getEndDirection(), move.getTotalCost(), 1))
                            .min()
                            .orElse(0);

                    entry.setValue(entry.getValue() - 0.5 * nextMinCoalCost);
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
     * @param enemyPosition the enemy's position
     * @param coal the available coal
     * @return the amount of coal to use for acceleration
     */
    public static int getAccelerationCoal(@NonNull Board board, int turn,
                                          @NonNull Vector3 position, @NonNull Vector3 enemyPosition,
                                          int coal) {
        final boolean isEnemyAhead = isEnemyAhead(board, position, enemyPosition);

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

        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final Move move = new Move(path.get(0), enemyShip.getPosition(), playerShip.getDirection());

        final Vector3 destination = path.get(path.size() - 1);
        final Field destinationField = gameState.getBoard().getFieldAt(destination);
        final boolean mustReachSpeed = destinationField instanceof Goal || gameState.getBoard().canPickUpPassenger(destination);
        final int destinationSpeed = gameState.getBoard().isCounterCurrent(destination) ? 2 : 1;

        final int turnCoal = Math.min(playerShip.getCoal(), 1);
        final int accelerationCoal = Math.min(playerShip.getCoal() - turnCoal, getAccelerationCoal(
                gameState.getBoard(),
                gameState.getTurn(),
                playerShip.getPosition(),
                enemyShip.getPosition(),
                playerShip.getCoal()
        ));
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

        addAcceleration(playerShip.getSpeed(), move);

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
