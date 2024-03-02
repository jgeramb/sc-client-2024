package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.Team;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class GameState {

    protected final Board board = new Board();
    protected final List<Ship> ships = new ArrayList<>();
    protected final Team playerTeam;
    protected GamePhase gamePhase = GamePhase.LOBBY;
    protected Team currentTeam;
    protected int turn;

    public GameState(Team playerTeam) {
        this.playerTeam = playerTeam;

        for(Team team : Team.values())
            this.ships.add(new Ship(team));
    }

    public Ship getShip(Team team) {
        return this.ships.stream()
                .filter(ship -> ship.getTeam() == team)
                .findFirst()
                .orElse(null);
    }

    public Ship getPlayerShip() {
        return this.getShip(this.playerTeam);
    }

    public Ship getEnemyShip() {
        return this.ships.stream().filter(ship -> ship.getTeam() != this.playerTeam).findFirst().orElse(null);
    }

    /**
     * Updates the ships with the new ship data list.
     *
     * @param shipDataList The new ship data list
     */
    public void updateShips(@NonNull List<ShipData> shipDataList) {
        shipDataList.forEach(ship -> {
            final Ship stateShip = this.getShip(ship.getTeam());

            if(stateShip == null)
                throw new NoSuchElementException("Ship not found for team " + ship.getTeam());

            stateShip.setPosition(ship.getPosition().toVector3());
            stateShip.setDirection(ship.getDirection());
            stateShip.setPassengers(ship.getPassengers());
            stateShip.setCoal(ship.getCoal());
            stateShip.setSpeed(ship.getSpeed());
            stateShip.setFreeTurns(ship.getFreeTurns());
            stateShip.setPoints(ship.getPoints());
        });
    }

    /**
     * Get all possible moves for the current ship.
     *
     * @param position The start position of the ship
     * @param shipDirection The direction of the ship
     * @param freeTurns The amount of free turns
     * @param freeAcceleration The remaining free accelerations
     * @param minMovementPoints The minimum amount of movement points to use
     * @param maxMovementPoints The maximum amount of movement points to use
     * @param maxCoal The maximum amount of coal to use
     * @return All possible moves for the current ship.
     */
    public List<Move> getMoves(@NonNull Vector3 position,
                               @NonNull Direction shipDirection,
                               int freeTurns,
                               int freeAcceleration,
                               int minMovementPoints,
                               int maxMovementPoints,
                               int maxCoal) {
        final List<Move> moves = new ArrayList<>();

        this.getDirectionCosts(shipDirection, position, maxCoal + freeTurns).forEach((turnDirection, turnCost) -> {
            final int currentFreeTurns = Math.max(0, freeTurns - turnCost);
            final int availableCoal = Math.max(0, maxCoal - Math.max(0, turnCost - freeTurns));

            for (int currentMax = minMovementPoints; currentMax <= maxMovementPoints; currentMax++) {
                final Move longestMove = new Move(position, turnDirection);

                if (shipDirection != turnDirection)
                    longestMove.turn(turnDirection);

                final AdvanceInfo advanceInfo = this.getAdvanceLimit(
                        position,
                        turnDirection,
                        currentMax,
                        freeAcceleration,
                        availableCoal
                );
                final Vector3 endPosition = advanceInfo.getEndPosition(position, turnDirection);
                final AdvanceInfo.Result result = advanceInfo.getResult();
                final int cost = this.appendForwardMove(
                        position,
                        turnDirection,
                        longestMove,
                        advanceInfo,
                        currentMax - advanceInfo.getCost()
                );

                if(cost == -1)
                    continue;

                longestMove.segment(
                        this.getBoard().getSegmentIndex(endPosition),
                        this.getBoard().getSegmentColumn(endPosition)
                );

                final int extraCost = cost - longestMove.getDistance();
                final boolean lookForMoves = switch (result) {
                    case COUNTER_CURRENT, BLOCKED, PASSENGER, FINISH -> cost < currentMax;
                    default -> false;
                };

                if (lookForMoves && (cost <= minMovementPoints || extraCost < freeAcceleration)) {
                    getMoves(
                            endPosition,
                            turnDirection,
                            currentFreeTurns,
                            Math.max(0, freeAcceleration - extraCost),
                            minMovementPoints - cost,
                            currentMax - cost,
                            availableCoal - Math.max(0, extraCost - freeAcceleration)
                    ).forEach(move -> {
                        final Move longestMoveCopy = longestMove.copy();
                        longestMoveCopy.append(move);

                        moves.add(longestMoveCopy);
                    });
                }

                if (longestMove.getTotalCost() == currentMax)
                    moves.add(longestMove);
            }
        });

        return moves;
    }

    /**
     * Appends all actions for the advance to the given move.
     *
     * @param endPosition The end position of the advance
     * @param direction The direction of the advance
     * @param move The move to append the actions to
     * @param advanceInfo The information about the advance
     * @param remainingMovementPoints The remaining movement points after the advance
     * @return The total cost of the move
     */
    public int appendForwardMove(@NonNull Vector3 endPosition,
                                 @NonNull Direction direction,
                                 @NonNull Move move,
                                 @NonNull AdvanceInfo advanceInfo,
                                 int remainingMovementPoints) {
        final AdvanceInfo.Result result = advanceInfo.getResult();

        if (result == AdvanceInfo.Result.SHIP) {
            final boolean wasCounterCurrent = this.getBoard()
                    .isCounterCurrent(endPosition);
            final boolean isCounterCurrent = this.getBoard()
                    .isCounterCurrent(endPosition.copy().add(direction.toVector3()));
            final int pushCost = 2 + (!wasCounterCurrent && isCounterCurrent ? 1 : 0);

            if (pushCost <= remainingMovementPoints) {
                final Direction pushDirection = this.getBestPushDirection(direction);

                if (pushDirection != null) {
                    final int forwardCost = advanceInfo.getCost() + pushCost - 1;

                    move.forward(advanceInfo.getDistance() + 1, forwardCost);
                    move.push(pushDirection);

                    return move.getTotalCost();
                }
            }
        }

        if (result == AdvanceInfo.Result.PASSENGER) {
            move.passenger();
        } else if (result == AdvanceInfo.Result.FINISH)
            move.finish();

        if (advanceInfo.getDistance() == 0)
            return -1;

        move.forward(advanceInfo.getDistance(), advanceInfo.getCost());

        return move.getTotalCost();
    }

    /**
     * Get the maximum free forward moves.
     *
     * @param start The start position of the ship
     * @param direction The direction of the ship
     * @param maxMovementPoints The maximum amount of movement points to use
     * @param freeAcceleration The remaining free accelerations
     * @param coal The maximum amount of coal to use
     * @return The information about the maximum free forward moves
     */
    public AdvanceInfo getAdvanceLimit(@NonNull Vector3 start,
                                       @NonNull Direction direction,
                                       int maxMovementPoints,
                                       int freeAcceleration,
                                       int coal) {
        final AdvanceInfo advanceInfo = new AdvanceInfo();

        final Vector3 position = start.copy();
        final Ship playerShip = this.getPlayerShip(), enemyShip = this.getEnemyShip();

        boolean onCounterCurrent = false;

        while(advanceInfo.getCost() < maxMovementPoints) {
            position.add(direction.toVector3());

            final Field field = this.board.getFieldAt(position);

            if(field == null || field.isObstacle()) {
                advanceInfo.setResult(AdvanceInfo.Result.BLOCKED);
                break;
            }

            if(enemyShip != null && enemyShip.getPosition().equals(position)) {
                advanceInfo.setResult(AdvanceInfo.Result.SHIP);
                break;
            }

            final boolean isCounterCurrent = this.board.isCounterCurrent(position);

            if(!onCounterCurrent && isCounterCurrent) {
                if(advanceInfo.getCost() + 2 > maxMovementPoints) {
                    advanceInfo.setResult(AdvanceInfo.Result.COUNTER_CURRENT);
                    break;
                }

                advanceInfo.incrementCost();
                onCounterCurrent = true;
            }

            advanceInfo.incrementDistance();
            advanceInfo.incrementCost();

            final boolean isAtMinimumSpeed = playerShip.getSpeed() == 1;
            final boolean canSlowDown = advanceInfo.getCost() == (isCounterCurrent ? 2 : 1)
                    && advanceInfo.getCost() >= playerShip.getSpeed() - freeAcceleration - coal;
            final boolean canReachMinimumSpeed = isAtMinimumSpeed || canSlowDown;

            if(field instanceof Finish && playerShip.hasEnoughPassengers() && canReachMinimumSpeed) {
                advanceInfo.setResult(AdvanceInfo.Result.FINISH);
                break;
            }

            for (int i = -2; i <= 2; i++) {
                final Direction currentDirection = direction.rotate(i);
                final Vector3 currentPosition = position.copy().add(currentDirection.toVector3());
                final Field currentField = this.board.getFieldAt(currentPosition);

                if(!(currentField instanceof Passenger passenger))
                    continue;

                final Vector3 collectPosition = currentPosition.copy().add(passenger.getDirection().toVector3());
                final boolean canPickUpPassenger = passenger.getPassenger() > 0 && position.equals(collectPosition);

                if (canReachMinimumSpeed && canPickUpPassenger) {
                    advanceInfo.setResult(AdvanceInfo.Result.PASSENGER);
                    break;
                }
            }

            if(advanceInfo.getResult().equals(AdvanceInfo.Result.PASSENGER))
                break;
        }

        return advanceInfo;
    }

    /**
     * Returns the cost for all possible directions.
     *
     * @param direction The current direction of the ship
     * @param position The current position of the ship
     * @param maxTurns The maximum amount of turns to consider
     * @return The required turn count for all possible directions
     */
    public Map<Direction, Integer> getDirectionCosts(@NonNull Direction direction, @NonNull Vector3 position, int maxTurns) {
        final Map<Direction, Integer> costs = new HashMap<>();
        final double maxRotations = Math.ceil((Direction.values().length - 1) / 2d);
        final double turns = Math.min(maxTurns, maxRotations);

        for(int i = (int) Math.floor(-turns); i <= turns; i++) {
            final Direction currentDirection = direction.rotate(i);

            if(costs.containsKey(currentDirection))
                continue;

            final int turnCost = direction.costTo(currentDirection);

            if(turnCost > turns || this.board.isBlocked(position.copy().add(currentDirection.toVector3())))
                continue;

            costs.put(currentDirection, turnCost);
        }

        return costs;
    }

    /**
     * Get the minimum required turn count for a direction.
     *
     * @param direction The target direction
     * @param position The current position of the ship
     * @return The minimum required turn count for the given direction
     */
    public int getMinTurns(@NonNull Direction direction, @NonNull Vector3 position) {
        return this.getDirectionCosts(direction, position, Direction.values().length)
                .values()
                .stream()
                .min(Integer::compareTo)
                .orElse(0);
    }

    /**
     * Get the best push direction for the enemy ship.
     *
     * @param from The direction the player ship is coming from
     * @return The direction with the highest score
     */
    public Direction getBestPushDirection(@NonNull Direction from) {
        final Ship enemyShip = this.getEnemyShip();
        final Vector3 position = enemyShip.getPosition();
        final int segmentIndex = this.board.getSegmentIndex(position);
        final int segmentColumn = this.board.getSegmentColumn(position);

        Direction bestDirection = null;
        double maxScore = Integer.MIN_VALUE;

        for(Direction currentDirection : Direction.values()) {
            if(currentDirection.toVector3().equals(from.toVector3().invert()))
                continue;

            final Vector3 pushPosition = position.copy().add(currentDirection.toVector3());
            final Field pushField = this.board.getFieldAt(pushPosition);

            if(pushField == null || pushField.isObstacle())
                continue;

            final int counterCurrentBonus = this.board.isCounterCurrent(pushPosition) ? 1 : 0;

            if(pushField instanceof Finish
                    && enemyShip.hasEnoughPassengers()
                    && (enemyShip.getSpeed() - enemyShip.getCoal() - 1) <= (1 + counterCurrentBonus))
                continue;

            final int currentSegmentIndex = this.board.getSegmentIndex(pushPosition);
            final int currentSegmentColumn = this.board.getSegmentColumn(pushPosition);
            final double deltaSegmentPosition = currentSegmentIndex - segmentIndex
                    + Math.abs(currentSegmentColumn - segmentColumn)
                    * (currentSegmentColumn >= segmentColumn ? 1 : -1)
                    / 4d;
            final int nearbyObstacles = (int) Arrays.stream(Direction.values())
                    .filter(direction -> this.board.isBlocked(pushPosition.copy().add(direction.toVector3())))
                    .count();
            final double score = nearbyObstacles
                    + this.getMinTurns(enemyShip.getDirection(), pushPosition)
                    + counterCurrentBonus
                    - deltaSegmentPosition;

            if(score > maxScore) {
                maxScore = score;
                bestDirection = currentDirection;
            }
        }

        return bestDirection;
    }

}
