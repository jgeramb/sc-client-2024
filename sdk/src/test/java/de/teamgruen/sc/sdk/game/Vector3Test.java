/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Vector3Test {

    @Test
    public void testAdd() {
        final Vector3 vector = new Vector3(1, 2, 3);
        final Vector3 delta = new Vector3(4, 5, 6);

        assertEquals(new Vector3(5, 7, 9), vector.add(delta));
    }

    @Test
    public void testSubtract() {
        final Vector3 vector = new Vector3(1, 2, 3);
        final Vector3 delta = new Vector3(4, 5, 6);

        assertEquals(new Vector3(-3, -3, -3), vector.subtract(delta));
    }

    @Test
    public void testMultiply() {
        final Vector3 vector = new Vector3(1, 2, 3);

        assertEquals(new Vector3(2, 4, 6), vector.multiply(2));
    }

    @Test
    public void testInvert() {
        final Vector3 vector = new Vector3(1, 2, 3);

        assertEquals(new Vector3(-1, -2, -3), vector.invert());
    }

    @Test
    public void testCopy() {
        final Vector3 vector = new Vector3(1, 2, 3);

        assertEquals(vector, vector.copy());
    }

}
