package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.FieldArray;
import de.teamgruen.sc.sdk.protocol.data.board.SegmentData;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Finish;
import lombok.Data;

import java.util.*;

@Data
public class Board {

    private final List<Vector3> counterCurrent = new ArrayList<>();
    private Direction nextSegmentDirection;
    private List<BoardSegment> segments = new ArrayList<>();

    public BoardField getFieldAt(Vector3 position) {
        for (BoardSegment segment : this.segments) {
            BoardField field = segment.getFieldAt(position);

            if (field != null)
                return field;
        }

        return null;
    }

    public Set<BoardField> getFinishFields() {
        Set<BoardField> finishFields = new HashSet<>();

        for (BoardSegment segment : this.segments) {
            Arrays.stream(segment.fields())
                    .filter(boardField -> boardField.field() instanceof Finish)
                    .forEach(finishFields::add);
        }

        return finishFields;
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
        this.segments = segmentDataList.stream().map(segment -> {
            final BoardField[] fields = new BoardField[20];
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

            int i = 0;

            for (FieldArray columns : segment.getColumns()) {
                final Vector3 columnCenter = center.add(direction.toVector3());
                int j = -(columns.getFields().size() - 1) / 2;

                for (Field field : columns.getFields()) {
                    Vector3 fieldPosition = columnCenter.copy();

                    if (j != 0)
                        fieldPosition.add((j < 0 ? direction1 : direction2).multiply(Math.abs(j)));

                    fields[i++] = new BoardField(fieldPosition, field);
                    j++;
                }
            }

            return new BoardSegment(fields, segment.getCenter().toVector3(), direction);
        }).toList();
        this.updateCounterCurrent();
    }

}
