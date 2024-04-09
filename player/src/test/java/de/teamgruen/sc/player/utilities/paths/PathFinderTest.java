/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PathFinderTest {

    private static final GameState gameState = new ExampleGameState();

    @BeforeAll
    public static void setUp() {
        PathFinder.setGameState(gameState);
    }

    @Test
    public void testGetNeighbours() {
        gameState.getPlayerShip().setDirection(Direction.RIGHT);

        final List<Vector3> expectedNodes = List.of(
                new Vector3(1, 0, -1),
                new Vector3(0, 1, -1),
                new Vector3(-1, 1, 0),
                new Vector3(-1, 0, 1),
                new Vector3(0, -1, 1)
        );

        final List<Vector3> actualNodes = PathFinder.getNeighbours(new Vector3(0, 0, 0));

        assertTrue(expectedNodes.containsAll(actualNodes));
    }

    @Test
    public void testGetEstimatedPathCost() {
        final Vector3 start = new Vector3(0, 0, 0);
        final Vector3 end = new Vector3(2, 0, -2);

        assertEquals(2, PathFinder.getEstimatedPathCost(start, end));
    }

    @Test
    public void testReconstructPath() {
        final PathNode end = new PathNode(new Vector3(2, 0, -2));
        final PathNode node1 = new PathNode(new Vector3(1, 0, -1));
        end.setPreviousNode(node1);
        final PathNode node2 = new PathNode(new Vector3(0, 0, 0));
        node1.setPreviousNode(node2);

        final List<Vector3> expectedPath = List.of(new Vector3(0, 0, 0), new Vector3(1, 0, -1), new Vector3(2, 0, -2));

        assertEquals(expectedPath, PathFinder.reconstructPath(end));
    }

    @Test
    public void testFindPath_NoneAvailable() {
        final Vector3 start = new Vector3(-3, 5, -2);
        final Vector3 end = new Vector3(-4, 6, -2);

        assertNull(PathFinder.findPath(Direction.DOWN_LEFT, start, end));
    }

    @Test
    public void testFindPath_Straight() {
        final Vector3 start = new Vector3(0, 0, 0);
        final Vector3 end = new Vector3(2, 0, -2);

        final List<Vector3> expectedPath = List.of(start, new Vector3(1, 0, -1), end);

        assertEquals(expectedPath, PathFinder.findPath(Direction.RIGHT, start, end));
    }

    @Test
    public void testFindPath_Turns() {
        final Vector3 start = new Vector3(-3, 5, -2);
        final Vector3 end = new Vector3(0, 5, -5);

        final List<Vector3> expectedPath = List.of(
                start,
                new Vector3(-2, 4, -2),
                new Vector3(-1, 4, -3),
                new Vector3(0, 4, -4),
                end
        );

        assertEquals(expectedPath, PathFinder.findPath(Direction.UP_RIGHT, start, end));
    }

}
