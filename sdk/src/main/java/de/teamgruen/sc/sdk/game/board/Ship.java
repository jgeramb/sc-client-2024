/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Ship {

    private final Team team;
    private int passengers = 0, coal = 6, speed = 1, freeTurns = 1, points = 0;
    private Direction direction = Direction.RIGHT;
    private Vector3 position = null;

    public boolean hasEnoughPassengers() {
        return this.passengers >= 2;
    }

}
