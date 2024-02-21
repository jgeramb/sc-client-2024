package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;

public record BoardSegment(BoardField[] fields, Vector3 center, Direction direction) {

    public BoardField getFieldAt(Vector3 position) {
        for (BoardField field : this.fields) {
            if (field.position().equals(position))
                return field;
        }

        return null;
    }

}
