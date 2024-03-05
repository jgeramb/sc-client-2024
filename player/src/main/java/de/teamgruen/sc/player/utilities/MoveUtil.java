/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoveUtil {

    /**
     * Returns the most efficient move for the current game state.
     * @param gameState the current game state
     * @return the most efficient move
     */
    public static Optional<Move> getMostEfficientMove(GameState gameState) {
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
            final boolean canFinish = gameState.getBoard().getFieldAt(move.getEndPosition()) instanceof Finish
                    && playerShip.hasEnoughPassengers();
            final double deltaSegmentColumn = Math.floorMod(move.getSegmentColumn() - playerSegmentColumn, 4);
            final double deltaSegmentPosition = move.getSegmentIndex() - playerSegmentIndex
                    + (deltaSegmentColumn > 2 ? deltaSegmentColumn - 4 : deltaSegmentColumn) / 4d;
            final int coalCost = move.getCoalCost(playerShip);
            final double score = (move.isFinished() ? Integer.MAX_VALUE : 0)
                    + (canFinish || (isEnemyAhead && deltaSegmentPosition >= 0) ? 50 : 0)
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
    public static List<Move> getPossibleMoves(GameState gameState, boolean moreCoal) {
        final Ship ship = gameState.getPlayerShip();
        final int maxCoal = Math.min(ship.getCoal(), moreCoal ? 2 : 1);

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
    private static void addAcceleration(Ship ship, Move move) {
        final int acceleration = move.getAcceleration(ship);

        if(acceleration != 0)
            move.getActions().add(0, ActionFactory.changeVelocity(acceleration));
    }

    /**
     * Returns the minimum movement points for the current game state.
     * @param gameState the current game state
     * @return the minimum movement points
     */
    private static int getMinMovementPoints(GameState gameState) {
        return Math.max(1, gameState.getPlayerShip().getSpeed() - 1);
    }

    /**
     * Returns the maximum movement points for the current game state.
     * @param gameState the current game state
     * @param maxCoal the maximum amount of coal to use
     * @return the maximum movement points
     */
    private static int getMaxMovementPoints(GameState gameState, int maxCoal) {
        final Ship ship = gameState.getPlayerShip();
        final int playerSegmentIndex = gameState.getBoard().getSegmentIndex(ship.getPosition());
        final int enemySegmentIndex = gameState.getBoard().getSegmentIndex(gameState.getEnemyShip().getPosition());

        return Math.min(6, ship.getSpeed() + ((enemySegmentIndex > playerSegmentIndex + 1 && maxCoal > 0) ? 2 : 1));
    }

    /**
     * Returns the next move to reach the given path.
     * @param gameState the current game state
     * @param path the path to reach
     * @return the next move to reach the given path
     */
    public static Optional<Move> moveFromPath(GameState gameState, List<Vector3> path) {
        if(path == null || path.isEmpty())
            return Optional.empty();

        final Ship playerShip = gameState.getPlayerShip();
        final Move move = new Move(path.get(0), playerShip.getDirection());

        // TODO: implement moveFromPath

        return Optional.of(move);
    }

}
