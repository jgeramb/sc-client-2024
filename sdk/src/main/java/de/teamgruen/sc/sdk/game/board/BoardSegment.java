package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;

import java.util.Map;

public record BoardSegment(Map<Vector3, Field> fields, Vector3 center, Direction direction) {
}
