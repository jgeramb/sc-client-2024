package de.teamgruen.sc.player.utilities;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoveUtil {

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
                    + move.getPassengers() * 20
                    + move.getPushes() * 3
                    + Math.max(0, move.getDistance() - coalCost) * deltaSegmentPosition
                    - Math.max(0, move.getMinTurns(gameState) - 1) * 3.5;

            if(score > highestScore || score == highestScore && coalCost < lowestCoalCost) {
                highestScore = score;
                bestMove = move;
                lowestCoalCost = coalCost;
            }
        }

        return Optional.ofNullable(bestMove);
    }

    public static List<Move> getPossibleMoves(GameState gameState, boolean moreCoal) {
        final Ship ship = gameState.getPlayerShip();
        final Vector3 position = ship.getPosition();
        final int playerSegmentIndex = gameState.getBoard().getSegmentIndex(position);
        final int enemySegmentIndex = gameState.getBoard().getSegmentIndex(gameState.getEnemyShip().getPosition());
        final boolean mustFollowEnemy = enemySegmentIndex > playerSegmentIndex + 2;
        final int maxCoal = Math.min(ship.getCoal(), moreCoal ? 2 : 1);

        final List<Move> moves = new ArrayList<>(gameState.getMoves(
                position,
                ship.getDirection(),
                ship.getFreeTurns(),
                1,
                Math.max(1, ship.getSpeed() - 1 - ship.getCoal()),
                Math.min(6, ship.getSpeed() + ((mustFollowEnemy && maxCoal > 0) ? 2 : 1)),
                maxCoal
        ));

        if(moves.isEmpty() && !moreCoal && ship.getCoal() > 1)
            return getPossibleMoves(gameState, true);

        moves.forEach(move -> {
            final List<Action> actions = move.getActions();
            final int acceleration = move.getAcceleration(ship);

            if(acceleration != 0)
                actions.add(0, ActionFactory.changeVelocity(acceleration));
        });

        return moves;
    }

}
