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
import de.teamgruen.sc.sdk.logging.AnsiColor;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.actions.Turn;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Goal;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
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
        final List<Vector3> nextSegmentPositions = board.getNextFieldsPositions();
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final boolean isEnemyAhead = isEnemyAhead(board, playerShip.getPosition(), enemyShip.getPosition());

        final List<Move> moves = getPossibleMoves(gameState, isEnemyAhead);

        Move bestMove = null;
        double highestScore = Integer.MIN_VALUE;
        int lowestCoalCost = 0;

        for(Move move : moves) {
            final int coalCost = Math.max(0, move.getCoalCost(playerShip) - (isEnemyAhead || gameState.getTurn() < 2 ? 1 : 0));
            final int availableCoal = playerShip.getCoal() - coalCost;
            final Vector3 endPosition = move.getEndPosition();
            final Direction endDirection = move.getEndDirection();
            final List<Move> nextMoves = gameState.getBoard()
                    .getMoves(
                            playerShip,
                            endPosition,
                            endDirection,
                            enemyShip,
                            move.getEnemyEndPosition(),
                            null,
                            1,
                            1,
                            gameState.getMinMovementPoints(playerShip),
                            Math.min(6, move.getTotalCost() + availableCoal + 1),
                            availableCoal
                    )
                    .stream()
                    .peek(nextMove -> {
                        final Vector3 nextPosition = nextMove.getEndPosition().copy().add(nextMove.getEndDirection().toVector3());

                        if(nextSegmentPositions.contains(nextPosition))
                            nextMove.forward(0, -nextMove.getTotalCost() + playerShip.getSpeed());
                    })
                    .sorted(Comparator.comparingInt(currentMove -> currentMove.getCoalCost(playerShip)))
                    .toList();

            // skip moves that lead to a dead end
            if(nextMoves.isEmpty())
                continue;

            final double score = evaluateMove(board, playerShip, isEnemyAhead, move, availableCoal, nextMoves.get(0).getCoalCost(playerShip));

            System.out.println(AnsiColor.GREEN.toString() + score + AnsiColor.RESET + ": " + AnsiColor.WHITE + move + AnsiColor.RESET);

            if(score > highestScore || (score == highestScore && coalCost < lowestCoalCost)) {
                // turn to the cheapest direction if the ship was not turned yet
                if(move.getActions().stream().noneMatch(action -> action instanceof Turn)) {
                    nextMoves.stream()
                            .filter(nextMove -> nextMove.getActions().get(0) instanceof Turn nextTurn && nextTurn.getDirection() != endDirection)
                            .max(Comparator.comparingDouble(nextMove -> evaluateMove(
                                    board,
                                    playerShip,
                                    isEnemyAhead(board, endPosition, move.getEnemyEndPosition()),
                                    nextMove,
                                    availableCoal - nextMove.getCoalCost(playerShip),
                                    0
                            )))
                            .ifPresent(cheapestEntry -> {
                                final Direction nextDirection = ((Turn) cheapestEntry.getActions().get(0)).getDirection();

                                move.getActions().add(ActionFactory.turn(endDirection.rotateTo(nextDirection, 1)));
                            });
                }

                highestScore = score;
                bestMove = move;
                lowestCoalCost = coalCost;
            }
        }

        return Optional.ofNullable(bestMove);
    }

    /**
     * Evaluates the given move based on the following criteria:
     * <ul>
     *     <li>whether the move allows the player to end the game</li>
     *     <li>whether the enemy is at least 3 segments ahead of the player</li>
     *     <li>the amount of passengers collected in relation to the coal used</li>
     *     <li>the distance between the ship and the end of the move</li>
     *     <li>the amount of coal used</li>
     *     <li>the minimum amount of coal required for the next move in relation to the remaining coal</li>
     * </ul>
     *
     * @param board the game board
     * @param ship the player's ship
     * @param isEnemyAhead whether the enemy is at least 3 segments ahead of the player
     * @param move the move to evaluate
     * @param availableCoal the amount of coal available after the move
     * @param minNextCoalCost the minimum amount of coal required for the next move
     * @return the score of the move
     */
    public static double evaluateMove(Board board, Ship ship, boolean isEnemyAhead, Move move, int availableCoal, int minNextCoalCost) {
        final double segmentDistance = getSegmentDistance(board, ship, move);
        final int coalCost = ship.getCoal() - availableCoal;

        return (move.isGoal() ? 100 : 0)
                + (isEnemyAhead && segmentDistance >= 0 ? 50 : 0)
                + move.getPassengers() * (15 - coalCost * 5)
                + segmentDistance
                - coalCost
                - minNextCoalCost * (6 - availableCoal);
    }

    /**
     * Returns a list of all possible moves for the current game state.
     * @param gameState the current game state
     * @param moreCoal whether to consider moves with more coal
     * @return a list of all possible moves
     */
    public static List<Move> getPossibleMoves(@NonNull GameState gameState, boolean moreCoal) {
        final Ship playerShip = gameState.getPlayerShip(), enemyShip = gameState.getEnemyShip();
        final int maxCoal = Math.min(playerShip.getCoal(), moreCoal || gameState.getTurn() < 2 ? 2 : 1);

        final List<Move> moves = new ArrayList<>(gameState.getBoard().getMoves(
                playerShip,
                playerShip.getPosition(),
                playerShip.getDirection(),
                enemyShip,
                enemyShip.getPosition(),
                null,
                playerShip.getFreeTurns(),
                1,
                gameState.getMinMovementPoints(playerShip),
                Math.min(6, gameState.getMaxMovementPoints(playerShip) + getAccelerationCoal(gameState)),
                maxCoal
        ));

        // if no moves are possible, try moves that require more coal
        if(moves.isEmpty() && maxCoal < 2 && playerShip.getCoal() > 1)
            return getPossibleMoves(gameState, true);

        moves.forEach(move -> addAcceleration(playerShip, move));

        return moves;
    }

    /**
     * Adds an acceleration action to the given move if necessary.
     * @param ship the player's ship
     * @param move the move to add the acceleration action to
     */
    public static void addAcceleration(@NonNull Ship ship, @NonNull Move move) {
        final int acceleration = move.getAcceleration(ship);

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
        final boolean useCoal = isEnemyAhead || gameState.getTurn() < 2;

        return useCoal && playerShip.getCoal() > 0 ? 1 : 0;
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
        final int maxMovementPoints = Math.min(6, gameState.getMaxMovementPoints(playerShip) + getAccelerationCoal(gameState));
        final Vector3 destination = path.get(path.size() - 1);
        final Field destinationField = gameState.getBoard().getFieldAt(destination);
        final boolean collectPassenger = Arrays.stream(Direction.values())
                .map(direction -> destination.copy().add(direction.toVector3()))
                .anyMatch(position -> gameState.getBoard().getFieldAt(position) instanceof Passenger);
        final boolean mustReachSpeed = destinationField instanceof Goal || collectPassenger;
        final int destinationSpeed = gameState.getBoard().isCounterCurrent(destination) ? 2 : 1;

        int pathIndex = 1 /* skip start position */;

        int coal = Math.min(playerShip.getCoal(), 1);
        int freeTurns = playerShip.getFreeTurns();

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
                final AdvanceInfo advanceInfo = gameState.getBoard().getAdvanceLimit(
                        playerShip,
                        position,
                        direction,
                        move.getEnemyEndPosition(),
                        0,
                        points,
                        move.getTotalCost() > playerShip.getSpeed() ? 1 : 0,
                        coal
                );

                gameState.getBoard().appendForwardMove(
                        advanceInfo.getEndPosition(position, direction),
                        direction,
                        gameState.getEnemyShip(),
                        move,
                        advanceInfo,
                        availablePoints - advanceInfo.getCost()
                );
            } else
                break;
        }

        if(move.getTotalCost() < gameState.getMinMovementPoints(playerShip))
            return Optional.empty();

        pathIndex = path.indexOf(move.getEndPosition());

        // turn to reach the next position if there are free turns left
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
     * @return whether the enemy is at least 3 segments ahead of the player
     */
    public static boolean isEnemyAhead(Board board, Vector3 playerPosition, Vector3 enemyPosition) {
        return board.getSegmentIndex(enemyPosition) > board.getSegmentIndex(playerPosition) + 2;
    }

}
