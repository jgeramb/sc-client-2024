/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data;

import de.teamgruen.sc.sdk.game.Vector3;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Direction {

    RIGHT(1, 0, -1),
    DOWN_RIGHT(0, 1, -1),
    DOWN_LEFT(-1, 1, 0),
    LEFT(-1, 0, 1),
    UP_LEFT(0, -1, 1),
    UP_RIGHT(1, -1, 0);

    private final int q, r, s;

    public Vector3 toVector3() {
        return new Vector3(this.q, this.r, this.s);
    }

    public Direction rotate(int delta) {
        return values()[Math.floorMod(this.ordinal() + delta, values().length)];
    }

    public int delta(Direction direction) {
        final int delta = Math.floorMod(direction.ordinal() - this.ordinal(), values().length);

        return (delta > values().length / 2) ? delta - values().length : delta;
    }

    public int costTo(Direction direction) {
        return Math.abs(this.delta(direction));
    }

    public static Direction fromVector3(Vector3 vector) {
        for (Direction direction : values()) {
            if (direction.toVector3().equals(vector))
                return direction;
        }

        throw new IllegalArgumentException("Vector is not a direction: " + vector);
    }

}
