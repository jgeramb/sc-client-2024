/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Island;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BoardTest {

    private Board board;

    @BeforeAll
    public void prepareBoard() {
        this.board = new ExampleGameState().getBoard();
    }

    @Test
    public void testUpdateSegments() {
        final BoardSegment actualSegment = this.board.getSegments().get(1);

        final Vector3 actualCenter = actualSegment.center();
        assertEquals(0, actualCenter.getQ());
        assertEquals(4, actualCenter.getR());
        assertEquals(-4, actualCenter.getS());

        final Direction actualDirection = actualSegment.direction();
        assertEquals(Direction.DOWN_RIGHT, actualDirection);

        final LinkedHashMap<Vector3, Field> actualFields = actualSegment.fields();
        final Map.Entry<Vector3, Field> actualField = actualFields.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(new Vector3(1, 2, -3)))
                .findFirst()
                .orElse(null);

        assertNotNull(actualField);
        assertInstanceOf(Passenger.class, actualField.getValue());

        final Passenger actualPassenger = (Passenger) actualField.getValue();

        assertEquals(Direction.RIGHT, actualPassenger.getDirection());
    }

    @Test
    public void testUpdateCounterCurrent() {
        final List<Vector3> expected = Arrays.asList(
                new Vector3(-1, 0, 1),
                new Vector3(0, 0, 0),
                new Vector3(0, 1, -1),
                new Vector3(0, 2, -2),
                new Vector3(0, 3, -3),
                new Vector3(0, 4, -4),
                new Vector3(-1, 5, -4),
                new Vector3(-2, 6, -4),
                new Vector3(-3, 7, -4),
                new Vector3(-4, 8, -4),
                new Vector3(-5, 9, -4),
                new Vector3(-6, 10, -4)
        );

        assertArrayEquals(vectorsToArray(expected), vectorsToArray(this.board.getCounterCurrent()));
    }

    @Test
    public void testIsCounterCurrent() {
        assertTrue(this.board.isCounterCurrent(new Vector3(-2, 6, -4)));
    }

    @Test
    public void testGetGoalFields() {
        assertTrue(this.board.getGoalFields().containsKey(new Vector3(-6, 10, -4)));
    }

    @Test
    public void testGetPassengerFields() {
        final Field actualField = this.board.getPassengerFields().get(new Vector3(-1, 7, -6));

        assertNotNull(actualField);
        assertInstanceOf(Passenger.class, actualField);
        assertEquals(Direction.UP_LEFT, ((Passenger) actualField).getDirection());
    }

    @Test
    public void testGetFieldAt() {
        final Field actualField = this.board.getFieldAt(new Vector3(-3, 6, -3));

        assertNotNull(actualField);
        assertInstanceOf(Island.class, actualField);
    }

    @Test
    public void testIsBlocked_Null() {
        assertTrue(this.board.isBlocked(new Vector3(-2, 0, 2)));
    }

    @Test
    public void testIsBlocked_Island() {
        assertTrue(this.board.isBlocked(new Vector3(1, -1, 0)));
    }

    @Test
    public void testIsBlocked_Passenger() {
        assertTrue(this.board.isBlocked(new Vector3(1, 2, -3)));
    }

    @Test
    public void testIsBlocked_Water() {
        assertFalse(this.board.isBlocked(new Vector3(1, 0, -1)));
    }

    @Test
    public void testIsBlocked_Goal() {
        assertFalse(this.board.isBlocked(new Vector3(-4, 10, -6)));
    }

    @Test
    public void testGetSegmentColumn() {
        assertEquals(1, this.board.getSegmentColumn(new Vector3(0, 4, -4)));
    }

    @Test
    public void testGetSegmentColumn_InvalidPosition() {
        assertThrows(IllegalArgumentException.class, () -> this.board.getSegmentColumn(new Vector3(-2, 0, 2)));
    }

    @Test
    public void testGetSegmentIndex() {
        assertEquals(1, this.board.getSegmentIndex(new Vector3(0, 4, -4)));
    }

    @Test
    public void testGetSegmentIndex_InvalidPosition() {
        assertThrows(IllegalArgumentException.class, () -> this.board.getSegmentIndex(new Vector3(-2, 0, 2)));
    }

    @Test
    public void testGetNextSegmentDirection() {
        assertEquals(Direction.DOWN_LEFT, this.board.getNextSegmentDirection());
    }

    public int[][] vectorsToArray(List<Vector3> vectors) {
        int[][] result = new int[vectors.size()][3];

        for (int i = 0; i < vectors.size(); i++) {
            final Vector3 vector = vectors.get(i);

            result[i][0] = vector.getQ();
            result[i][1] = vector.getR();
            result[i][2] = vector.getS();
        }

        return result;
    }

}
