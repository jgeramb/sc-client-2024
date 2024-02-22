package de.teamgruen.sc.player.util;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Island;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;

import java.util.*;

public class ActionUtil {

    private static final Random RANDOM = new Random();

    public static List<Action> getPossibleActions(GameState gameState) {
        final List<Action> actions = new ArrayList<>();
        final Ship ship = gameState.getPlayerShip();

        // Add all possible actions here

        return actions;
    }

    public static Action getRandomAction(GameState gameState) {
        final List<Action> actions = getPossibleActions(gameState);

        return actions.get(RANDOM.nextInt(actions.size()));
    }

    public static Map<Direction, Integer> getPossiblePushDirections(GameState gameState, Ship enemyShip) {
        final Ship ship = gameState.getPlayerShip();
        final Direction direction = ship.getDirection();
        final List<Vector3> counterCurrent = gameState.getBoard().getCounterCurrent();
        final Map<Direction, Integer> possibleDirections = new HashMap<>();

        for(Direction currentDirection : Direction.values()) {
            if(currentDirection.toVector3().equals(direction.toVector3().multiply(-1)))
                continue;

            final Vector3 pushPosition = enemyShip.getPosition().copy().add(currentDirection.toVector3());
            final Field pushField = gameState.getBoard().getFieldAt(pushPosition);

            if(pushField instanceof Island || pushField instanceof Passenger)
                continue;

            if(pushField instanceof Finish && enemyShip.getPassengers() >= 2 && (ship.getSpeed() - ship.getCoal() - 1) < 2)
                continue;

            final int nearbyObstacles = Arrays.stream(Direction.values())
                    .mapToInt(neighbour -> {
                        final Vector3 neighbourPosition = pushPosition.copy().add(neighbour.toVector3());
                        final Field neighbourField = gameState.getBoard().getFieldAt(neighbourPosition);

                        return (neighbourField instanceof Island || neighbourField instanceof Passenger) ? 1 : 0;
                    })
                    .sum();

            possibleDirections.put(currentDirection, nearbyObstacles + (counterCurrent.contains(pushPosition) ? 1 : 0));
        }

        return possibleDirections;
    }

}
