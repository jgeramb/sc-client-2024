package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;

public record BoardField(Vector3 position, Field field) {
}