package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.player.utilities.MoveUtil;
import de.teamgruen.sc.player.utilities.paths.PathFinder;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdvancedGameHandler extends BaseGameHandler {

    private final Object readyLock = new Object();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private List<Vector3> nextPath;

    public AdvancedGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onGameStart(GameState gameState) {
        super.onGameStart(gameState);

        PathFinder.setGameState(gameState);
    }

    @Override
    public void onBoardUpdate(GameState gameState) {
        if(!gameState.getPlayerTeam().equals(gameState.getCurrentTeam()))
            return;

        final Board board = gameState.getBoard();
        final Ship playerShip = gameState.getPlayerShip();
        final Vector3 shipPosition = playerShip.getPosition();
        final List<List<Vector3>> paths = new ArrayList<>();
        final List<Runnable> tasks = new ArrayList<>();

        final Vector3 enemyShipPosition = gameState.getEnemyShip().getPosition();
        final int enemyShipVisitedSegmentsCount = board.getSegmentIndex(enemyShipPosition);

        // check if enemy ship is more than 2 segments ahead
        if(enemyShipVisitedSegmentsCount - board.getSegmentIndex(playerShip.getPosition()) > 2)
            tasks.add(() -> paths.add(PathFinder.findPath(shipPosition, enemyShipPosition)));
        else {
            // passengers
            board.getPassengerFields().forEach((position, field) -> {
                final Passenger passenger = (Passenger) field;

                if(passenger.getPassenger() < 1)
                    return;

                final Vector3 collectPosition = position.copy().add(passenger.getDirection().toVector3());

                tasks.add(() -> paths.add(PathFinder.findPath(shipPosition, collectPosition)));
            });

            if (playerShip.hasEnoughPassengers()) {
                // finish
                board.getFinishFields().forEach((position, field) -> tasks.add(() ->
                        paths.add(PathFinder.findPath(shipPosition, position))
                ));
            }
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        final CountDownLatch countDownLatch = new CountDownLatch(tasks.size());

        tasks.forEach(task -> executorService.submit(() -> {
            try {
                task.run();
            } finally {
                countDownLatch.countDown();
            }
        }));

        try {
            countDownLatch.await();
        } catch (InterruptedException ignore) {
        } finally {
            executorService.shutdown();
        }

        this.nextPath = paths.stream()
            .filter(Objects::nonNull)
            .min(Comparator.comparingInt(List::size))
            .orElse(null);
        this.ready.set(true);

        synchronized (this.readyLock) {
            this.readyLock.notify();
        }
    }

    @Override
    public List<Action> getNextActions(GameState gameState) {
        final Ship ship = gameState.getPlayerShip();

        if(!this.ready.get()) {
            synchronized (this.readyLock) {
                try {
                    this.readyLock.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }

        if(this.nextPath == null || this.nextPath.isEmpty()) {
            final Optional<Move> optionalMove = MoveUtil.getMostEfficientMove(gameState);

            if(optionalMove.isEmpty()) {
                this.onError("No actions available");
                return Collections.emptyList();
            }

            final Move move = optionalMove.get();
            final List<Action> actions = move.getActions();
            actions.forEach(action -> action.perform(gameState));
            ship.setCoal(ship.getCoal() - move.getCoalCost(ship));

            return actions;
        }

        final long startTime = System.currentTimeMillis();

        try {
            final Board board = gameState.getBoard();
            final List<Action> actions = new ArrayList<>();

            // update speed
            final int freeAccelerations = 1;
            final Vector3 endPosition = this.nextPath.get(this.nextPath.size() - 1);
            final Field endField = gameState.getBoard().getFieldAt(endPosition);
            int coal = ship.getCoal();
            int deltaSpeed;

            if ((endField instanceof Finish && ship.hasEnoughPassengers()) || endField instanceof Passenger) {
                final int expectedSpeed = board.isCounterCurrent(endPosition) ? 2 : 1;

                deltaSpeed = expectedSpeed - ship.getSpeed();
            } else
                deltaSpeed = this.nextPath.size() - ship.getSpeed();

            deltaSpeed = Math.max(Math.min(deltaSpeed, Math.min(1, coal) + freeAccelerations), -coal - freeAccelerations);
            deltaSpeed = Math.min(6 - ship.getSpeed(), deltaSpeed);

            if (deltaSpeed != 0) {
                coal -= Math.abs(deltaSpeed) - freeAccelerations;
                actions.add(ActionFactory.changeVelocity(deltaSpeed));
            }

            // move ship
            boolean isOnCounterCurrent = false;
            int availableMoves = ship.getSpeed();
            Direction direction = ship.getDirection();
            int freeTurns = ship.getFreeTurns();
            int distance = 0;

            for (int i = 1; i < this.nextPath.size(); i++) {
                distance++;

                final Vector3 nextPosition = ship.getPosition();
                final Vector3 delta = this.nextPath.get(i - 1).copy().subtract(nextPosition);
                final Direction nextDirection = Direction.fromVector3(delta);

                if (nextDirection == null) {
                    this.logger.error("Invalid endDirection");
                    break;
                }

                if (direction != nextDirection) {
                    if (distance > 1)
                        actions.add(ActionFactory.forward(distance - 1));

                    final int directionDelta = direction.delta(nextDirection);
                    final int turns = Math.min(freeTurns + coal, Math.abs(directionDelta));

                    freeTurns -= turns;

                    if(freeTurns < 0)
                        coal += freeTurns;

                    actions.add(ActionFactory.turn(direction.rotate(directionDelta > 0 ? -turns : turns)));
                    distance = 1;
                    isOnCounterCurrent = false;
                }

                final Ship enemyShip = gameState.getEnemyShip();

                if (enemyShip != null && enemyShip.getPosition().equals(nextPosition)) {
                    final Direction pushDirection = gameState.getBestPushDirection(direction);

                    if (availableMoves < 2 || pushDirection == null) {
                        distance--;
                        break;
                    }

                    actions.add(ActionFactory.push(pushDirection));

                    isOnCounterCurrent = false;
                }

                final boolean isCounterCurrent = board.isCounterCurrent(nextPosition);

                if (!isOnCounterCurrent && availableMoves < 2) {
                    distance--;
                    break;
                }

                isOnCounterCurrent = isCounterCurrent;
                availableMoves -= isCounterCurrent ? 2 : 1;

                if (availableMoves == 0)
                    break;
            }

            if (availableMoves > 0 && distance > 0)
                actions.add(ActionFactory.forward(Math.min(distance, availableMoves)));

            // perform moves
            actions.forEach(action -> action.perform(gameState));
            ship.setCoal(coal);

            return actions;
        } finally {
            this.ready.set(false);
            this.logger.debug("Time: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

}
