/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class PathNode {

    private final Vector3 position;
    private int graphCost = 0, heuristicCost = 0;

    public int getTotalCost() {
        return this.graphCost + this.heuristicCost;
    }

    public Direction directionTo(Vector3 otherPosition) {
        return Direction.fromVector3(otherPosition.copy().subtract(this.position));
    }

}
