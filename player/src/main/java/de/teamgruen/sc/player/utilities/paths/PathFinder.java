/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;

@RequiredArgsConstructor
public class PathFinder {

    private static final Map<Vector3, PathNode> CACHE = new HashMap<>();
    @Setter
    private static GameState gameState;

    /**
     * Find the shortest path from start to end using the A* algorithm.
     * @param start the start position
     * @param end the end position
     * @return the shortest path from start to end (start and end included)
     *         or null if no path was found
     */
    public static LinkedList<Vector3> findPath(@NonNull Vector3 start, @NonNull Vector3 end) {
        PathNode currentNode = getOrCreateNode(start);

        final PriorityQueue<PathNode> frontier = new PriorityQueue<>(Comparator.comparingInt(PathNode::getTotalCost));
        frontier.add(currentNode);

        final Map<Vector3, Vector3> cameFrom = new HashMap<>();
        cameFrom.put(start, null);

        final Map<Vector3, Integer> costSoFar = new HashMap<>();
        costSoFar.put(start, 0);

        while(!frontier.isEmpty()) {
            currentNode = frontier.poll();

            final Vector3 currentPosition = currentNode.getPosition();
            final Vector3 lastNode = cameFrom.get(currentPosition);
            final Direction currentDirection = lastNode != null
                    ? currentNode.directionTo(lastNode)
                    : gameState.getPlayerShip().getDirection();

            // early exit if the end position was reached
            if (currentPosition.equals(end))
                return reconstructPath(cameFrom, currentPosition);

            final boolean wasCounterCurrent = gameState.getBoard().isCounterCurrent(currentPosition);
            final int currentCost = costSoFar.get(currentPosition);

            for (PathNode neighbour : getNeighbours(currentNode)) {
                final Vector3 neighbourPosition = neighbour.getPosition();
                final int gCost = currentCost + 1;

                if(!costSoFar.containsKey(neighbourPosition) || gCost < costSoFar.get(neighbourPosition)) {
                    final int moveCost = !wasCounterCurrent && gameState.getBoard().isCounterCurrent(neighbourPosition) ? 1 : 0;
                    final Vector3 delta = neighbourPosition.copy().subtract(currentNode.getPosition());
                    final int turnCost = currentDirection.costTo(Direction.fromVector3(delta));

                    neighbour.setGraphCost(gCost);
                    neighbour.setHeuristicCost(getEstimatedPathCost(neighbourPosition, end) + moveCost + turnCost);

                    frontier.add(neighbour);
                    costSoFar.put(neighbourPosition, gCost);
                    cameFrom.put(neighbourPosition, currentPosition);
                }
            }
        }

        return null;
    }

    /**
     * Reconstructs the path from the destination to the start.
     * @param cameFrom the map of nodes to their previous node
     * @param destination the destination
     * @return the reconstructed path
     */
    private static LinkedList<Vector3> reconstructPath(Map<Vector3, Vector3> cameFrom, Vector3 destination) {
        final LinkedList<Vector3> path = new LinkedList<>();
        Vector3 current = destination;

        do {
            path.add(current);
        } while((current = cameFrom.get(current)) != null);

        Collections.reverse(path);

        return path;
    }

    /**
     * Returns the Manhattan distance between the start and end position.
     * @param start the start position
     * @param end the end position
     * @return the estimated path cost
     */
    private static int getEstimatedPathCost(Vector3 start, Vector3 end) {
        return Math.max(
                Math.abs(start.getQ() - end.getQ()),
                Math.max(
                        Math.abs(start.getS() - end.getS()),
                        Math.abs(start.getR() - end.getR())
                )
        );
    }

    /**
     * @param node the node to get the neighbours for
     * @return the neighbours of the given node
     */
    private static List<PathNode> getNeighbours(PathNode node) {
        final List<PathNode> neighbours = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            final Vector3 neighbourPosition = node.getPosition().copy().add(direction.toVector3());

            if (!gameState.getBoard().isBlocked(neighbourPosition))
                neighbours.add(getOrCreateNode(neighbourPosition));
        }

        return neighbours;
    }

    /**
     * @param position the position to get or create a node for
     * @return the node for the given position
     */
    private static synchronized PathNode getOrCreateNode(Vector3 position) {
        return CACHE.computeIfAbsent(position, PathNode::new);
    }

}
