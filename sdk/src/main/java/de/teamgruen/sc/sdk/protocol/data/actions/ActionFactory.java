/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.NonNull;

public class ActionFactory {

    public static ChangeVelocity changeVelocity(int deltaVelocity) {
        ChangeVelocity move = new ChangeVelocity();
        move.setDeltaVelocity((byte) deltaVelocity);

        return move;
    }

    public static Forward forward(int distance) {
        Forward move = new Forward();
        move.setDistance((byte) distance);

        return move;
    }

    public static Push push(@NonNull Direction direction) {
        Push move = new Push();
        move.setDirection(direction);

        return move;
    }

    public static Turn turn(@NonNull Direction direction) {
        Turn move = new Turn();
        move.setDirection(direction);

        return move;
    }

}
