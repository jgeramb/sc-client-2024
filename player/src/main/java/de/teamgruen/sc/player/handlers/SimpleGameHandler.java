package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.player.util.ActionUtil;
import de.teamgruen.sc.player.util.PathFinder;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;

import java.util.*;

public class SimpleGameHandler extends BaseGameHandler {

    private List<Vector3> nextPath;

    public SimpleGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onBoardUpdate(GameState gameState) {
        final Board board = gameState.getBoard();

        PathFinder.setGameState(gameState);

        final Ship playerShip = gameState.getPlayerShip();
        final Vector3 shipPosition = playerShip.getPosition();
        final List<List<Vector3>> paths = new ArrayList<>();

        final int enemyShipVisitedSegmentsCount = gameState.getEnemyShips()
                .stream()
                .mapToInt(enemyShip -> enemyShip.getVisitedSegments().size())
                .max()
                .orElse(0);

        // check if enemy ship is 2+ segments ahead
        if(enemyShipVisitedSegmentsCount >= playerShip.getVisitedSegments().size() + 2) {
            // enemy ships
            gameState.getEnemyShips().forEach(enemyShip -> {
                final Map<Direction, Integer> possibleDirections = ActionUtil.getPossiblePushDirections(gameState, enemyShip);
                final int maxScore = possibleDirections.values().stream().max(Integer::compareTo).orElse(0);

                if (maxScore >= 2)
                    paths.add(PathFinder.findPath(shipPosition, enemyShip.getPosition()));
            });
        } else {
            // passengers
            board.getPassengerFields().forEach((position, field) -> {
                final Passenger passenger = (Passenger) field;

                if(passenger.getPassenger() < 1)
                    return;

                final Vector3 collectPosition = position.copy().add(passenger.getDirection().toVector3());

                paths.add(PathFinder.findPath(shipPosition, collectPosition));
            });

            if (playerShip.getPassengers() > 0) {
                if (playerShip.getPassengers() >= 2) {
                    // finish
                    paths.addAll(board.getFinishFields().keySet().stream().map(field ->
                            PathFinder.findPath(shipPosition, field)
                    ).toList());
                }
            }
        }

        this.nextPath = paths.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(List::size))
                .orElse(null);
    }

    @Override
    public List<Action> getNextActions(GameState gameState) {
        final long startTime = System.currentTimeMillis();

        try {
            final Ship ship = gameState.getPlayerShip();

            if (this.nextPath == null || this.nextPath.isEmpty()) {
                return ActionUtil.getPossibleActionCombinations(gameState)
                        .stream()
                        .max(Comparator.comparingInt(combination ->
                                combination.getTotalDistance(ship.getDirection()) - combination.extraCost())
                        )
                        .map(combination -> combination.toActions(ship))
                        .orElseThrow();
            }

            final List<Action> actions = new ArrayList<>();
            final List<Vector3> counterCurrent = gameState.getBoard().getCounterCurrent();

            // update speed
            final int freeAccelerations = 1;
            final Vector3 endPosition = this.nextPath.get(this.nextPath.size() - 1);
            final Field endField = gameState.getBoard().getFieldAt(endPosition);
            int coal = ship.getCoal();
            int deltaSpeed;

            if ((endField instanceof Finish && ship.getPassengers() >= 2) || endField instanceof Passenger) {
                final int expectedSpeed = counterCurrent.contains(endPosition) ? 2 : 1;

                deltaSpeed = expectedSpeed - ship.getSpeed();
            } else
                deltaSpeed = this.nextPath.size() - ship.getSpeed();

            deltaSpeed = Math.max(Math.min(deltaSpeed, freeAccelerations), -coal - freeAccelerations);
            deltaSpeed = Math.min(6 - ship.getSpeed(), deltaSpeed);

            if (deltaSpeed != 0) {
                coal -= Math.abs(deltaSpeed) - freeAccelerations;
                actions.add(ActionFactory.changeVelocity(deltaSpeed));
            }

            // move ship
            boolean isOnCounterCurrent = counterCurrent.contains(ship.getPosition());
            int availableMoves = ship.getSpeed() - (isOnCounterCurrent ? 1 : 0);
            Direction direction = ship.getDirection();
            int freeTurns = ship.isPushed() ? 2 : 1;
            int distance = 0;

            for (int i = 1; i < this.nextPath.size(); i++) {
                distance++;

                final Vector3 nextPosition = ship.getPosition();
                final Vector3 delta = this.nextPath.get(i - 1).copy().subtract(nextPosition);
                final Direction nextDirection = Direction.fromVector3(delta);

                if (nextDirection == null) {
                    this.logger.error("Invalid direction");
                    break;
                }

                if (direction != nextDirection) {
                    final boolean turnRight = direction.ordinal() < nextDirection.ordinal();
                    int executeTurns = 0;

                    for (int j = 0; j <= Math.abs(nextDirection.ordinal() - direction.ordinal()); j++) {
                        if (freeTurns == 0) {
                            if (coal == 0)
                                break;

                            coal--;
                        } else
                            freeTurns--;

                        if (turnRight)
                            executeTurns++;
                        else
                            executeTurns--;
                    }

                    if (distance > 1)
                        actions.add(ActionFactory.forward(distance - 1));

                    actions.add(ActionFactory.turn(Direction.values()[direction.ordinal() - 1 + executeTurns]));
                    distance = 1;
                }

                final Ship enemyShip = gameState.getShips()
                        .stream()
                        .filter(currentShip -> currentShip.getPosition().equals(nextPosition))
                        .findFirst()
                        .orElse(null);

                if (enemyShip != null) {
                    final Map<Direction, Integer> possibleDirections = ActionUtil.getPossiblePushDirections(gameState, enemyShip);

                    if (availableMoves > 1 && !possibleDirections.isEmpty()) {
                        final Direction pushDirection = possibleDirections
                                .entrySet()
                                .stream()
                                .max(Comparator.comparingInt(Map.Entry::getValue))
                                .map(Map.Entry::getKey)
                                .orElse(null);

                        actions.add(ActionFactory.push(pushDirection));
                    } else {
                        distance--;
                        break;
                    }
                }

                final boolean isCounterCurrent = counterCurrent.contains(nextPosition);

                if (!isCounterCurrent)
                    isOnCounterCurrent = false;
                else if (!isOnCounterCurrent && availableMoves < 2) {
                    distance--;
                    break;
                }

                availableMoves -= isCounterCurrent ? 2 : 1;

                if (availableMoves == 0)
                    break;
            }

            if (availableMoves > 0 && distance > 0)
                actions.add(ActionFactory.forward(Math.min(distance, availableMoves)));

            // perform moves
            actions.forEach(action -> action.perform(gameState, ship));
            ship.setCoal(coal);

            return actions;
        } finally {
            this.logger.debug("Time: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

}
