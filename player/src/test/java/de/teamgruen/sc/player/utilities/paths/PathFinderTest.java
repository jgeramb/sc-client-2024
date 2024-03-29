/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class PathFinderTest {

    private static final GameState gameState = new ExampleGameState();

    @BeforeAll
    public static void setUp() {
        PathFinder.setGameState(gameState);
    }

    @Test
    public void testGetOrCreateNode_Cached() {
        final Vector3 position = new Vector3(0, 0, 0);

        final PathNode expectedNode = new PathNode(position);
        expectedNode.setHeuristicCost(2);

        PathFinder.CACHE.put(position, expectedNode);

        final PathNode actualNode = PathFinder.getOrCreateNode(position);

        assertNotNull(actualNode);
        assertEquals(2, actualNode.getHeuristicCost());
    }

    @Test
    public void testGetOrCreateNode() {
        final Vector3 position = new Vector3(0, 0, 0);

        final PathNode actualNode = PathFinder.getOrCreateNode(position);

        assertNotNull(actualNode);
    }

    @Test
    public void testGetNeighbours() {
        gameState.getPlayerShip().setDirection(Direction.RIGHT);

        final List<PathNode> expectedNodes = List.of(
                new PathNode(new Vector3(1, 0, -1)),
                new PathNode(new Vector3(0, 1, -1)),
                new PathNode(new Vector3(-1, 1, 0)),
                new PathNode(new Vector3(-1, 0, 1)),
                new PathNode(new Vector3(0, -1, 1))
        );

        PathFinder.CACHE.putAll(expectedNodes.stream().collect(Collectors.toMap(PathNode::getPosition, node -> node)));

        final List<PathNode> actualNodes = PathFinder.getNeighbours(new PathNode(new Vector3(0, 0, 0)));

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
        final Vector3 end = new Vector3(2, 0, -2);
        final Map<Vector3, Vector3> cameFrom = new HashMap<>();
        cameFrom.put(new Vector3(0, 0, 0), null);
        cameFrom.put(new Vector3(1, 0, -1), new Vector3(0, 0, 0));
        cameFrom.put(new Vector3(2, 0, -2), new Vector3(1, 0, -1));

        final List<Vector3> expectedPath = List.of(new Vector3(0, 0, 0), new Vector3(1, 0, -1), end);

        assertEquals(expectedPath, PathFinder.reconstructPath(cameFrom, end));
    }

    @Test
    public void testFindPath_NoneAvailable() {
        final Ship ship = gameState.getPlayerShip();
        ship.setDirection(Direction.DOWN_LEFT);

        final Vector3 start = new Vector3(-3, 5, -2);
        final Vector3 end = new Vector3(-4, 6, -2);

        assertNull(PathFinder.findPath(ship, start, end));
    }

    @Test
    public void testFindPath_Straight() {
        final Ship ship = gameState.getPlayerShip();
        ship.setDirection(Direction.RIGHT);

        final Vector3 start = new Vector3(0, 0, 0);
        final Vector3 end = new Vector3(2, 0, -2);

        final List<Vector3> expectedPath = List.of(start, new Vector3(1, 0, -1), end);

        assertEquals(expectedPath, PathFinder.findPath(ship, start, end));
    }

    @Test
    public void testFindPath_Turns() {
        final Ship ship = gameState.getPlayerShip();
        ship.setDirection(Direction.UP_RIGHT);
        ship.setCoal(0);

        final Vector3 start = new Vector3(-3, 5, -2);
        final Vector3 end = new Vector3(0, 5, -5);

        final List<Vector3> expectedPath = List.of(
                start,
                new Vector3(-2, 4, -2),
                new Vector3(-1, 4, -3),
                new Vector3(0, 4, -4),
                end
        );

        assertEquals(expectedPath, PathFinder.findPath(ship, start, end));
    }

}
