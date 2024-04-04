/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data;

import de.teamgruen.sc.sdk.game.Vector3;
import lombok.AllArgsConstructor;
import lombok.NonNull;

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

    /**
     * @param rotations the amount of rotations
     * @return the rotated direction
     */
    public Direction rotate(int rotations) {
        return values()[Math.floorMod(this.ordinal() + rotations, values().length)];
    }

    /**
     * @param destination the target direction
     * @param maxRotations the maximum possible rotations
     * @return the nearest possible direction to the target direction
     */
    public Direction rotateTo(@NonNull Direction destination, int maxRotations) {
        return this.rotate(Math.max(-maxRotations, Math.min(maxRotations, this.delta(destination))));
    }

    /**
     * @param direction the target direction
     * @return the smallest delta between the current direction and the target direction
     */
    public int delta(@NonNull Direction direction) {
        final int delta = Math.floorMod(direction.ordinal() - this.ordinal(), values().length);

        return (delta > values().length / 2) ? delta - values().length : delta;
    }

    /**
     * @param direction the target direction
     * @return the cost to rotate to the target direction
     */
    public int costTo(@NonNull Direction direction) {
        return Math.abs(this.delta(direction));
    }

    public int toFieldColumn() {
        return switch (this) {
            case LEFT -> 0;
            case UP_LEFT, DOWN_LEFT -> 1;
            case UP_RIGHT, DOWN_RIGHT -> 2;
            case RIGHT -> 3;
        };
    }

    public static Direction fromVector3(@NonNull Vector3 vector) {
        for (Direction direction : values()) {
            if (direction.toVector3().equals(vector))
                return direction;
        }

        throw new IllegalArgumentException("Vector is not a direction: " + vector);
    }

}
