/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.Vector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathNodeTest {

    @Test
    public void testGetTotalCost() {
        final PathNode pathNode = new PathNode(new Vector3(0, 0, 0), false);
        pathNode.setGraphCost(2);
        pathNode.setHeuristicCost(3);

        assertEquals(5, pathNode.getTotalCost());
    }

}
