package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.FieldArray;
import de.teamgruen.sc.sdk.protocol.data.board.SegmentData;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import lombok.Data;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class Board {

    private final Map<Vector3, Field> fields = new HashMap<>();
    private final List<Vector3> counterCurrent = new ArrayList<>();
    private List<BoardSegment> segments = new ArrayList<>();
    private Direction nextSegmentDirection;

    public int getSegmentIndex(Vector3 fieldPosition) {
        for (int i = 0; i < this.segments.size(); i++) {
            if(this.segments.get(i).fields().containsKey(fieldPosition))
                return i;
        }

        throw new IllegalArgumentException("Field is not part of any segment");
    }

    public int getSegmentColumn(Vector3 fieldPosition) {
        for (BoardSegment segment : this.segments) {
            final int index = List.copyOf(segment.fields().keySet()).indexOf(fieldPosition);

            if(index != -1)
                return index / 5 /* rows*/;
        }

        throw new IllegalArgumentException("Field is not part of any segment");
    }

    public boolean isBlocked(Vector3 position) {
        final Field field = this.getFieldAt(position);

        return field == null || field.isObstacle();
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
        return this.getAllFields(entry ->
                entry.getValue() instanceof Passenger passenger
                        && passenger.getPassenger() > 0
        );
    }

    public Map<Vector3, Field> getFinishFields() {
        return this.getAllFields(entry -> entry.getValue() instanceof Finish);
    }

    public boolean isCounterCurrent(Vector3 position) {
        return this.counterCurrent.contains(position);
    }

    public void updateCounterCurrent() {
        this.counterCurrent.clear();

        for (int j = 0; j < this.segments.size(); j++) {
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

    public void updateSegments(List<SegmentData> segmentDataList) {
        this.fields.clear();
        this.segments = segmentDataList.stream().map(segment -> {
            final LinkedHashMap<Vector3, Field> fields = new LinkedHashMap<>();
            final Direction direction = segment.getDirection();
            final Vector3 direction1 = direction.rotate(-2).toVector3();
            final Vector3 direction2 = direction.rotate(2).toVector3();
            final Vector3 center = segment.getCenter().toVector3().subtract(direction.toVector3());

            for (FieldArray columns : segment.getColumns()) {
                int j = -(columns.getFields().size() - 1) / 2;

                for (Field field : columns.getFields()) {
                    Vector3 fieldPosition = center.copy();

                    if (j != 0) {
                        final Vector3 offsetVector = j < 0 ? direction1 : direction2;
                        fieldPosition.add(offsetVector.copy().multiply(Math.abs(j)));
                    }

                    fields.put(fieldPosition, field);
                    j++;
                }

                center.add(direction.toVector3());
            }

            this.fields.putAll(fields);

            return new BoardSegment(fields, segment.getCenter().toVector3(), direction);
        }).toList();
        this.updateCounterCurrent();
    }

}
