package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.FieldArray;
import de.teamgruen.sc.sdk.protocol.data.board.SegmentData;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class Board {

    private final List<Vector3> counterCurrent = new ArrayList<>();
    private final Map<Vector3, Field> fields = new HashMap<>();
    private Direction nextSegmentDirection;
    private List<BoardSegment> segments = new ArrayList<>();

    public BoardSegment getSegmentOfField(Vector3 position) {
        for (BoardSegment segment : this.segments) {
            if(segment.fields().containsKey(position))
                return segment;
        }

        return null;
    }

    public Field getFieldAt(Vector3 position) {
        return this.fields.get(position);
    }

    public Map<Vector3, Field> getAllFields(Predicate<Map.Entry<Vector3, Field>> predicate) {
        return this.fields
                .entrySet()
                .stream()
                .filter(predicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Vector3, Field> getPassengerFields() {
        return this.getAllFields(entry -> {
            if(entry.getValue() instanceof Passenger passenger)
                return passenger.getPassenger() > 0;

            return false;
        });
    }

    public Map<Vector3, Field> getFinishFields() {
        return this.getAllFields(entry -> entry.getValue() instanceof Finish);
    }

    public void updateCounterCurrent() {
        this.counterCurrent.clear();

        for (int j = 0; j < this.segments.size(); j++) {
            final BoardSegment segment = this.segments.get(j);

            // add the two fields before the turn
            final Vector3 segmentCenter = segment.center();
            this.counterCurrent.add(segmentCenter);

            final Vector3 turnPosition = segmentCenter.copy().add(segment.direction().toVector3());
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

    public void updateSegments(List<SegmentData> segmentDataList) {
        this.fields.clear();
        this.segments = segmentDataList.stream().map(segment -> {
            final Map<Vector3, Field> fields = new HashMap<>();
            final Direction direction = segment.getDirection();
            final Vector3 direction1 = (switch (segment.getDirection()) {
                case RIGHT -> Direction.UP_LEFT;
                case DOWN_RIGHT -> Direction.UP_RIGHT;
                case DOWN_LEFT -> Direction.RIGHT;
                case LEFT -> Direction.DOWN_RIGHT;
                case UP_LEFT -> Direction.DOWN_LEFT;
                case UP_RIGHT -> Direction.LEFT;
            }).toVector3();
            final Vector3 direction2 = (switch (segment.getDirection()) {
                case RIGHT -> Direction.DOWN_LEFT;
                case DOWN_RIGHT -> Direction.LEFT;
                case DOWN_LEFT -> Direction.UP_LEFT;
                case LEFT -> Direction.UP_RIGHT;
                case UP_LEFT -> Direction.RIGHT;
                case UP_RIGHT -> Direction.DOWN_RIGHT;
            }).toVector3();
            final Vector3 center = segment.getCenter().toVector3();

            for (FieldArray columns : segment.getColumns()) {
                final Vector3 columnCenter = center.add(direction.toVector3());
                int j = -(columns.getFields().size() - 1) / 2;

                for (Field field : columns.getFields()) {
                    Vector3 fieldPosition = columnCenter.copy();

                    if (j != 0)
                        fieldPosition.add((j < 0 ? direction1 : direction2).multiply(Math.abs(j)));

                    fields.put(fieldPosition, field);
                    j++;
                }
            }

            this.fields.putAll(fields);

            return new BoardSegment(fields, segment.getCenter().toVector3(), direction);
        }).toList();
        this.updateCounterCurrent();
    }

}
