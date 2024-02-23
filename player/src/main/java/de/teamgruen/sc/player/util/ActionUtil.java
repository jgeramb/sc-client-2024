package de.teamgruen.sc.player.util;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Island;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;

import java.util.*;

public class ActionUtil {

    private static final Random RANDOM = new Random();

    public static ActionCombination getRandomCombination(GameState gameState) {
        final List<ActionCombination> actionCombinations = getPossibleActionCombinations(gameState);

        return actionCombinations.get(RANDOM.nextInt(actionCombinations.size()));
    }

    public static List<ActionCombination> getPossibleActionCombinations(GameState gameState) {
        final List<ActionCombination> actions = new ArrayList<>();
        final Ship ship = gameState.getPlayerShip();
        final int direction = ship.getDirection().ordinal();
        final int possibleTurns = 1 + ship.getCoal();

        for(int i = direction - possibleTurns; i <= direction + possibleTurns; i++) {
            final Direction currentDirection = Direction.values()[i % Direction.values().length];
            final int turnCost = Math.max(0, Math.abs(direction - currentDirection.ordinal()) - 1);
            final int maxSpeed = Math.min(6, ship.getSpeed() + ship.getCoal() - turnCost + 1);
            final int minSpeed = Math.max(1, ship.getSpeed() - ship.getCoal() - turnCost - 1);

            int maxDistance = getMaxDistance(gameState, ship.getPosition().copy(), currentDirection, maxSpeed);

            if(maxDistance < minSpeed)
                continue;

            final int extraCost = turnCost + Math.max(0, Math.abs(maxDistance - ship.getSpeed()) - 1);

            actions.add(new ActionCombination(currentDirection, maxDistance, extraCost));
        }

        return actions;
    }

    private static int getMaxDistance(GameState gameState, Vector3 position, Direction direction, int movesAvailable) {
        final List<Vector3> counterCurrent = gameState.getBoard().getCounterCurrent();
        final Vector3 directionVector = direction.toVector3();

        int distance = 0;
        boolean isCounterCurrent = counterCurrent.contains(position);

        while(movesAvailable > 0) {
            position.add(directionVector);

            final Field field = gameState.getBoard().getFieldAt(position);

            if(field instanceof Island || field instanceof Passenger)
                break;

            if(!isCounterCurrent && (isCounterCurrent = counterCurrent.contains(position)))
                movesAvailable--;

            movesAvailable--;
            distance++;
        }

        return distance;
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
