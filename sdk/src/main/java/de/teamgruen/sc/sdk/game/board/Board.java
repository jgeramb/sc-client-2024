/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.AdvanceInfo;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.SegmentData;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Goal;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.Data;
import lombok.NonNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class Board {

    private final Map<Vector3, Field> fields = new HashMap<>();
    private final Map<Vector3, Integer> segmentIndices = new HashMap<>();
    private final Map<Vector3, Integer> segmentColumns = new HashMap<>();
    private final List<Vector3> counterCurrent = new ArrayList<>();
    private final List<Vector3> nextFieldsPositions = new ArrayList<>();
    private final List<BoardSegment> segments = new ArrayList<>();
    private Direction nextSegmentDirection;

    /**
     * @param fieldPosition the position to check
     * @return the index of the segment the field is part of
     * @throws IllegalArgumentException if the field is not part of any segment
     */
    public int getSegmentIndex(@NonNull Vector3 fieldPosition) {
        this.segmentIndices.computeIfAbsent(fieldPosition, (position) -> {
            for (int i = 0; i < this.segments.size(); i++) {
                if(this.segments.get(i).fields().containsKey(fieldPosition))
                    return i;
            }

            return null;
        });

        return Optional.ofNullable(this.segmentIndices.get(fieldPosition))
                .orElseThrow(() -> new IllegalArgumentException("Field is not part of any segment"));
    }

    /**
     * @param fieldPosition the position to check
     * @return the column of the segment the field is part of
     * @throws IllegalArgumentException if the field is not part of any segment
     */
    public int getSegmentColumn(@NonNull Vector3 fieldPosition) {
        this.segmentColumns.computeIfAbsent(fieldPosition, (position) -> {
            for (BoardSegment segment : this.segments) {
                final int index = List.copyOf(segment.fields().keySet()).indexOf(fieldPosition);

                if(index != -1)
                    return index / 5;
            }

            return null;
        });

        return Optional.ofNullable(this.segmentColumns.get(fieldPosition))
                .orElseThrow(() -> new IllegalArgumentException("Field is not part of any segment"));
    }

    /**
     * @param position the position to calculate the segment position for
     * @return the segment position (each column is 1/4 of a segment)
     */
    public double getSegmentPosition(@NonNull Vector3 position) {
        final int segmentIndex = this.getSegmentIndex(position);
        final int segmentColumn = this.getSegmentColumn(position);

        return segmentIndex + segmentColumn / 4d;
    }

    /**
     * @param position the first position
     * @param otherPosition the second position
     * @return the segment distance between the two positions
     */
    public double getSegmentDistance(@NonNull Vector3 position, @NonNull Vector3 otherPosition) {
        return this.getSegmentPosition(otherPosition) - this.getSegmentPosition(position);
    }

    /**
     * @param position the position to check
     * @return whether the position is not passable
     */
    public boolean isBlocked(@NonNull Vector3 position) {
        final Field field = this.getFieldAt(position);

        return field == null || field.isObstacle();
    }

    /**
     * @param position the position to check
     * @return the field at the given position or null if the field does not exist
     */
    public Field getFieldAt(@NonNull Vector3 position) {
        return this.fields.get(position);
    }

    /**
     * Returns all fields that match the given predicate
     * @param predicate the predicate to match
     * @return the matching fields
     */
    public Map<Vector3, Field> getAllFields(@NonNull Predicate<Map.Entry<Vector3, Field>> predicate) {
        return this.fields
                .entrySet()
                .stream()
                .filter(predicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @return all passenger fields with passengers
     */
    public Map<Vector3, Field> getPassengerFields() {
        return this.getAllFields(entry ->
                entry.getValue() instanceof Passenger passenger
                        && passenger.getPassenger() > 0
        );
    }

    /**
     * @return all goal fields
     */
    public Map<Vector3, Field> getGoalFields() {
        return this.getAllFields(entry -> entry.getValue() instanceof Goal);
    }

    /**
     * @param position the position to check
     * @return whether the position is part of the counter current
     */
    public boolean isCounterCurrent(@NonNull Vector3 position) {
        return this.counterCurrent.contains(position);
    }

    /**
     * Updates the counterCurrent list for the current segments
     */
    public void updateCounterCurrent(int startSegment) {
        for (int j = startSegment; j < this.segments.size(); j++) {
            final BoardSegment segment = this.segments.get(j);
            final Vector3 turnPosition = segment.center().copy();

            // add the two fields before the turn
            this.counterCurrent.add(turnPosition.copy().subtract(segment.direction().toVector3()));
            this.counterCurrent.add(turnPosition);

            // add the next two fields after the turn
            final Direction nextDirection;

            if(j < this.segments.size() - 1)
                nextDirection = this.segments.get(j + 1).direction();
            else
                nextDirection = this.nextSegmentDirection;

            for (int i = 1; i <= 2; i++)
                this.counterCurrent.add(turnPosition.copy().add(nextDirection.toVector3().multiply(i)));
        }
    }

    /**
     * Updates the nextFieldsPositions list for the next segment
     */
    public void updateNextFieldPositions() {
        this.nextFieldsPositions.clear();

        if(this.segments.size() == 8)
            return;

        final Vector3 nextCenter = this.segments.get(this.segments.size() - 1)
                .center()
                .copy()
                .add(this.nextSegmentDirection.toVector3().multiply(4));

        this.nextFieldsPositions.addAll(this.getFieldPositions(nextCenter, this.nextSegmentDirection));
    }

    /**
     * Updates the segments and the counterCurrent list
     * @param segmentDataList the new segments
     */
    public void updateSegments(List<SegmentData> segmentDataList) {
        final int nextSegmentIndex = this.segments.size();

        segmentDataList.forEach(segment -> {
            final Vector3 center = segment.getCenter().toVector3();
            final Optional<BoardSegment> optionalBoardSegment = this.segments.stream()
                    .filter(currentSegment -> currentSegment.center().equals(center))
                    .findFirst();

            if(optionalBoardSegment.isPresent()) {
                int i = 0;

                // update the passenger counts of the existing fields
                for (Map.Entry<Vector3, Field> fieldEntry : optionalBoardSegment.get().fields().entrySet()) {
                    if(fieldEntry.getValue() instanceof Passenger passenger) {
                        final Field newField = segment.getColumns().get(i / 5).getFields().get(i % 5);

                        if(!(newField instanceof Passenger newPassenger))
                            continue;

                        passenger.setPassenger(newPassenger.getPassenger());
                    }

                    i++;
                }
            } else {
                final LinkedHashMap<Vector3, Field> fields = new LinkedHashMap<>();
                final Direction direction = segment.getDirection();
                final List<Vector3> positions = this.getFieldPositions(center, direction);

                for (int i = 0; i < positions.size(); i++)
                    fields.put(positions.get(i), segment.getColumns().get(i / 5).getFields().get(i % 5));

                this.fields.putAll(fields);
                this.segments.add(new BoardSegment(fields, center, direction));
            }
        });

        this.updateNextFieldPositions();
        this.updateCounterCurrent(nextSegmentIndex);
    }

    /**
     * @param center the center of the segment
     * @param direction the direction of the segment
     * @return the positions of the fields in the segment
     */
    public List<Vector3> getFieldPositions(Vector3 center, Direction direction) {
        final List<Vector3> positions = new ArrayList<>();
        final Vector3 direction1 = direction.rotate(-2).toVector3();
        final Vector3 direction2 = direction.rotate(2).toVector3();

        // 4 columns
        for (int i = -1; i < 3; i++) {
            final Vector3 columnCenter = center.copy().add(direction.toVector3().multiply(i));

            // 5 rows
            for (int j = -2; j <= 2; j++) {
                Vector3 fieldPosition = columnCenter.copy();

                if (j != 0)
                    fieldPosition.add((j < 0 ? direction1 : direction2).copy().multiply(Math.abs(j)));

                positions.add(fieldPosition);
            }
        }

        return positions;
    }

    /**
     * @param position the position of the ship
     * @return whether the ship can pick up a passenger
     */
    public boolean canPickUpPassenger(@NonNull Vector3 position) {
        for (Direction direction : Direction.values()) {
            final Vector3 currentPosition = position.copy().add(direction.toVector3());
            final Field currentField = this.getFieldAt(currentPosition);

            if(!(currentField instanceof Passenger passenger))
                continue;

            final Vector3 collectPosition = currentPosition.copy().add(passenger.getDirection().toVector3());

            if(passenger.getPassenger() > 0 && position.equals(collectPosition))
                return true;
        }

        return false;
    }

    /**
     * @param ship the current ship
     * @param position the start position of the ship
     * @param shipDirection the direction of the ship
     * @param enemyShip the enemy ship
     * @param enemyPosition the position of the enemy ship
     * @param speed the current speed of the ship
     * @param freeTurns the amount of free turns
     * @param coal the maximum amount of coal to use
     * @param forceMultiplePushes whether to force multiple pushes if possible
     * @return all possible moves for the current ship
     */
    public Set<Move> getMoves(@NonNull Ship ship,
                              @NonNull Vector3 position,
                              @NonNull Direction shipDirection,
                              @NonNull Ship enemyShip,
                              Vector3 enemyPosition,
                              int speed,
                              int freeTurns,
                              int coal,
                              boolean forceMultiplePushes) {
        return this.getMoves(
                ship,
                position,
                shipDirection,
                enemyShip,
                enemyPosition,
                null,
                speed,
                freeTurns,
                0,
                coal,
                forceMultiplePushes
        );
    }

    /**
     * @param ship the current ship
     * @param position the start position of the ship
     * @param shipDirection the direction of the ship
     * @param enemyShip the enemy ship
     * @param enemyPosition the position of the enemy ship
     * @param excludedDirection the direction to exclude, may be null
     * @param speed the initial speed the ship
     * @param freeTurns the amount of free turns
     * @param usedPoints the available movement points
     * @param coal the maximum amount of coal to use
     * @param forceMultiplePushes whether to force multiple pushes if possible
     * @return all possible moves for the current ship
     */
    private Set<Move> getMoves(@NonNull Ship ship,
                               @NonNull Vector3 position,
                               @NonNull Direction shipDirection,
                               @NonNull Ship enemyShip,
                               Vector3 enemyPosition,
                               Direction excludedDirection,
                               int speed,
                               int freeTurns,
                               int usedPoints,
                               int coal,
                               boolean forceMultiplePushes) {
        final Set<Move> moves = new HashSet<>();

        this.getDirectionCosts(shipDirection, position, freeTurns + coal).forEach((turnDirection, turnCost) -> {
            if(Objects.equals(turnDirection, excludedDirection))
                return;

            final int remainingCoal = coal - Math.max(0, turnCost - freeTurns);
            final int minMovementPoints = Math.max(1, speed - remainingCoal - 1);
            final int maxMovementPoints = Math.min(6, speed + remainingCoal + 1);

            for (int currentPoints = 1; currentPoints <= maxMovementPoints - usedPoints; currentPoints++) {
                final Move move = new Move(position, enemyPosition, turnDirection);

                if (shipDirection != turnDirection)
                    move.turn(turnDirection);

                final AdvanceInfo advanceInfo = this.getAdvanceLimit(ship, position, turnDirection, enemyPosition, minMovementPoints, usedPoints, currentPoints);
                final AdvanceInfo.Result result = this.appendForwardMove(
                        advanceInfo.getEndPosition(position, turnDirection),
                        turnDirection,
                        enemyShip,
                        move,
                        advanceInfo,
                        currentPoints,
                        forceMultiplePushes
                );

                final Vector3 endPosition = move.getEndPosition();
                final int cost = move.getTotalCost();

                if (move.getDistance() == 0)
                    continue;

                if (cost >= minMovementPoints - usedPoints) {
                    move.segment(this.getSegmentIndex(endPosition), this.getSegmentColumn(endPosition));

                    moves.add(move);
                }

                final int totalPoints = usedPoints + cost;

                if (cost <= currentPoints && totalPoints < maxMovementPoints && !move.isGoal() && move.getPassengers() == 0) {
                    getMoves(
                            ship,
                            endPosition,
                            turnDirection,
                            enemyShip,
                            move.getEnemyEndPosition(),
                            result.equals(AdvanceInfo.Result.SHIP) ? null : turnDirection,
                            speed,
                            Math.max(0, freeTurns - turnCost),
                            totalPoints,
                            remainingCoal - Math.max(0, totalPoints - speed),
                            forceMultiplePushes
                    ).forEach(currentMove -> moves.add(move.copy().append(currentMove)));
                }
            }
        });

        return moves;
    }

    /**
     * Appends all actions for the advance to the given move.
     *
     * @param endPosition the end position of the advance
     * @param direction the direction of the advance
     * @param enemyShip the enemy ship
     * @param move the move to append the actions to
     * @param advanceInfo the information about the advance
     * @param availableMovementPoints the available movement points
     * @param forceMultiplePushes whether to force multiple pushes if possible
     * @return the actual result of the advance
     */
    public AdvanceInfo.Result appendForwardMove(@NonNull Vector3 endPosition,
                                                @NonNull Direction direction,
                                                @NonNull Ship enemyShip,
                                                @NonNull Move move,
                                                @NonNull AdvanceInfo advanceInfo,
                                                int availableMovementPoints,
                                                boolean forceMultiplePushes) {
        AdvanceInfo.Result result = advanceInfo.getResult();

        if (result == AdvanceInfo.Result.SHIP) {
            final boolean wasCounterCurrent = this.isCounterCurrent(endPosition);
            final boolean isCounterCurrent = this.isCounterCurrent(endPosition.copy().add(direction.toVector3()));
            final boolean payCounterCurrentCost = (!wasCounterCurrent || advanceInfo.getDistance() == 0) && isCounterCurrent;
            final int moveCost = payCounterCurrentCost ? 2 : 1;

            if(moveCost + 1 /* push cost */ + advanceInfo.getCost() <= availableMovementPoints) {
                final Direction pushDirection;

                if(forceMultiplePushes && !this.isBlocked(endPosition.copy().add(direction.toVector3().multiply(2))))
                    pushDirection = direction;
                else
                    pushDirection = this.getBestPushDirection(direction, enemyShip, move.getEnemyEndPosition(), forceMultiplePushes);

                if (pushDirection != null) {
                    move.forward(advanceInfo.getDistance() + 1, advanceInfo.getCost() + moveCost);
                    move.push(pushDirection);

                    return result;
                }
            }

            result = payCounterCurrentCost ? AdvanceInfo.Result.COUNTER_CURRENT : AdvanceInfo.Result.NORMAL;
        } else if (result == AdvanceInfo.Result.PASSENGER)
            move.passenger();
        else if (result == AdvanceInfo.Result.GOAL)
            move.goal();

        if(advanceInfo.getDistance() > 0)
            move.forward(advanceInfo.getDistance(), advanceInfo.getCost());

        return result;
    }

    /**
     * Get the maximum free forward moves.
     *
     * @param playerShip the player ship
     * @param start the start position of the ship
     * @param direction the direction of the ship
     * @param enemyPosition the position of the enemy ship
     * @param minReachableSpeed the minimum reachable speed
     * @param usedMovementPoints the used movement points so far
     * @param movementPoints the maximum amount of movement points to use
     * @return the information about the maximum free forward moves
     */
    public AdvanceInfo getAdvanceLimit(@NonNull Ship playerShip,
                                       @NonNull Vector3 start,
                                       @NonNull Direction direction,
                                       Vector3 enemyPosition,
                                       int minReachableSpeed,
                                       int usedMovementPoints,
                                       int movementPoints) {
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        final Vector3 directionVector = direction.toVector3(), position = start.copy();

        boolean onCounterCurrent = false;

        while(advanceInfo.getCost() < movementPoints) {
            position.add(directionVector);

            final Field field = this.getFieldAt(position);

            if(field == null || field.isObstacle()) {
                advanceInfo.setResult(AdvanceInfo.Result.BLOCKED);
                break;
            }

            if(Objects.equals(position, enemyPosition)) {
                advanceInfo.setResult(AdvanceInfo.Result.SHIP);
                break;
            }

            final boolean isCounterCurrent = this.isCounterCurrent(position);

            if(!onCounterCurrent && isCounterCurrent) {
                if(advanceInfo.getCost() + 2 > movementPoints) {
                    advanceInfo.setResult(AdvanceInfo.Result.COUNTER_CURRENT);
                    break;
                }

                advanceInfo.incrementCost();
                onCounterCurrent = true;
            }

            advanceInfo.incrementDistance();
            advanceInfo.incrementCost();

            final int totalMovementPoints = usedMovementPoints + advanceInfo.getCost();

            if(totalMovementPoints == (isCounterCurrent ? 2 : 1) && minReachableSpeed <= totalMovementPoints) {
                if (field instanceof Goal && playerShip.hasEnoughPassengers()) {
                    advanceInfo.setResult(AdvanceInfo.Result.GOAL);
                    break;
                } else if (this.canPickUpPassenger(position)) {
                    advanceInfo.setResult(AdvanceInfo.Result.PASSENGER);
                    break;
                }
            }
        }

        return advanceInfo;
    }

    /**
     * Returns the cost for all possible directions.
     *
     * @param direction the current direction of the ship
     * @param position the current position of the ship
     * @param maxTurns the maximum amount of turns to consider
     * @return the required turn count for all possible directions
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

            if(turnCost > turns || this.isBlocked(position.copy().add(currentDirection.toVector3())))
                continue;

            costs.put(currentDirection, turnCost);
        }

        return costs;
    }

    /**
     * Get the minimum required turn count for a direction.
     *
     * @param direction the target direction
     * @param position the current position of the ship
     * @return the minimum required turn count for the given direction
     */
    public int getMinTurns(@NonNull Direction direction, @NonNull Vector3 position) {
        return this.getDirectionCosts(direction, position, 3)
                .values()
                .stream()
                .min(Integer::compare)
                .orElse(0);
    }

    /**
     * @param position the ship's position
     * @return the direction of the next segment or null if the ship is on the last segment
     */
    public Direction getNextSegmentDirection(@NonNull Vector3 position) {
        final int segmentIndex = this.getSegmentIndex(position);

        if(segmentIndex < this.segments.size() - 1)
            return this.segments.get(segmentIndex + 1).direction();

        return segmentIndex == 7 ? null : this.nextSegmentDirection;
    }

    /**
     * @param position the ship's position
     * @param direction the ship's direction
     * @return the cost to reach the direction of the next segment (1 per turn)
     */
    public int getSegmentDirectionCost(@NonNull Vector3 position, @NonNull Direction direction) {
        final Direction nextSegmentDirection = this.getNextSegmentDirection(position);

        return nextSegmentDirection == null ? 0 : direction.costTo(nextSegmentDirection);
    }

    /**
     * Get the best push direction for the enemy ship.
     *
     * @param from the direction the ship is coming from
     * @param enemyShip the enemy ship
     * @param enemyPosition the position of the enemy ship
     * @param allowGoalAndPassengerPickUp whether to allow goal and passenger pick up
     * @return the direction with the highest score, or null if no direction is available
     */
    public Direction getBestPushDirection(@NonNull Direction from,
                                          @NonNull Ship enemyShip,
                                          @NonNull Vector3 enemyPosition,
                                          boolean allowGoalAndPassengerPickUp) {
        if(enemyShip.isStuck())
            return null;

        Direction bestDirection = null;
        double maxScore = Integer.MIN_VALUE;

        for(Direction currentDirection : Direction.values()) {
            if(currentDirection.toVector3().equals(from.toVector3().invert()))
                continue;

            final Vector3 pushPosition = enemyPosition.copy().add(currentDirection.toVector3());
            final Field pushField = this.getFieldAt(pushPosition);

            if(pushField == null || pushField.isObstacle())
                continue;

            final int counterCurrentBonus = this.isCounterCurrent(pushPosition) ? 1 : 0;
            final boolean hasEnoughPassengers = enemyShip.hasEnoughPassengers();

            if(!allowGoalAndPassengerPickUp && enemyShip.getSpeed() <= 2 + counterCurrentBonus + enemyShip.getCoal()) {
                if(pushField instanceof Goal && hasEnoughPassengers)
                    continue;

                if(this.canPickUpPassenger(pushPosition))
                    continue;
            }

            final double positionBonus = this.getSegmentDistance(pushPosition, enemyPosition) * (hasEnoughPassengers ? 4 : 2);
            final double score = this.getMinTurns(enemyShip.getDirection(), pushPosition) + counterCurrentBonus + positionBonus;

            if(score > maxScore) {
                maxScore = score;
                bestDirection = currentDirection;
            }
        }

        return bestDirection;
    }

}
