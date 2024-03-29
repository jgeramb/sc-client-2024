/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.Vector3;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class PathNode {

    private final Vector3 position;
    private int graphCost = 0, heuristicCost = 0, turnCost = 0;

    public int getTotalCost() {
        return this.graphCost + this.heuristicCost + turnCost;
    }

}
