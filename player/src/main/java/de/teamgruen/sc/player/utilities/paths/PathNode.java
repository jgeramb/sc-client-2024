/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Data
@RequiredArgsConstructor
public class PathNode implements Comparable<PathNode> {

    private final Vector3 position;
    private PathNode previousNode = null;
    private int graphCost = 0, heuristicCost = 0, turnCost = 0;

    public int getTotalCost() {
        return this.graphCost + this.heuristicCost + this.turnCost;
    }

    public Optional<Direction> getDirection() {
        if (this.previousNode == null)
            return Optional.empty();

        return Optional.of(Direction.fromVector3(this.position.copy().subtract(this.previousNode.getPosition())));
    }

    @Override
    public int compareTo(PathNode other) {
        return Integer.compare(this.getTotalCost(), other.getTotalCost());
    }

}
