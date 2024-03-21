/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data;

import de.teamgruen.sc.sdk.game.Vector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DirectionTest {

    @Test
    public void testToVector3() {
        assertEquals(new Vector3(1, 0, -1), Direction.RIGHT.toVector3());
    }

    @Test
    public void testRotate_Clockwise() {
        assertEquals(Direction.UP_RIGHT, Direction.UP_LEFT.rotate(1));
    }

    @Test
    public void testRotate_Clockwise_Overflow() {
        assertEquals(Direction.RIGHT, Direction.UP_RIGHT.rotate(1));
    }

    @Test
    public void testRotate_CounterClockwise() {
        assertEquals(Direction.RIGHT, Direction.DOWN_RIGHT.rotate(-1));
    }

    @Test
    public void testRotate_CounterClockwise_Underflow() {
        assertEquals(Direction.UP_RIGHT, Direction.RIGHT.rotate(-1));
    }

    @Test
    public void testRotateTo() {
        assertEquals(Direction.UP_RIGHT, Direction.UP_LEFT.rotateTo(Direction.UP_RIGHT, 1));
    }

    @Test
    public void testRotateTo_LessThanRequired() {
        assertEquals(Direction.UP_RIGHT, Direction.UP_LEFT.rotateTo(Direction.RIGHT, 1));
    }

    @Test
    public void testRotateTo_MoreThanRequired() {
        assertEquals(Direction.RIGHT, Direction.UP_LEFT.rotateTo(Direction.RIGHT, 3));
    }

    @Test
    public void testDelta() {
        assertEquals(1, Direction.RIGHT.delta(Direction.DOWN_RIGHT));
    }

    @Test
    public void testDelta_Equal() {
        assertEquals(0, Direction.RIGHT.delta(Direction.RIGHT));
    }

    @Test
    public void testDelta_Underflow() {
        assertEquals(-1, Direction.RIGHT.delta(Direction.UP_RIGHT));
    }

    @Test
    public void testDelta_Overflow() {
        assertEquals(1, Direction.UP_RIGHT.delta(Direction.RIGHT));
    }

    @Test
    public void testCostTo() {
        assertEquals(1, Direction.RIGHT.costTo(Direction.DOWN_RIGHT));
    }

    @Test
    public void testCostTo_Equal() {
        assertEquals(0, Direction.RIGHT.costTo(Direction.RIGHT));
    }

    @Test
    public void testCostTo_Underflow() {
        assertEquals(1, Direction.RIGHT.costTo(Direction.UP_RIGHT));
    }

    @Test
    public void testCostTo_Overflow() {
        assertEquals(1, Direction.UP_RIGHT.costTo(Direction.RIGHT));
    }

    @Test
    public void testFromVector3() {
        assertEquals(Direction.RIGHT, Direction.fromVector3(new Vector3(1, 0, -1)));
    }

    @Test
    public void testFromVector3_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> Direction.fromVector3(new Vector3(0, 0, 0)));
    }

}
