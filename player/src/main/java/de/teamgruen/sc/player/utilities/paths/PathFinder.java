/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;

public class PathFinder {

    static final Map<Vector3, PathNode> CACHE = new HashMap<>();
    @Setter
    private static GameState gameState;

    /**
     * Find the shortest path from start to end using the A* algorithm.
     * @param ship the ship to find the path for
     * @param start the start position
     * @param end the end position
     * @return the shortest path from start to end (start and end included)
     *         or null if no path was found
     */
    public static LinkedList<Vector3> findPath(Ship ship, @NonNull Vector3 start, @NonNull Vector3 end) {
        PathNode currentNode = getOrCreateNode(start);

        final Map<Vector3, Integer> turnsSoFar = new HashMap<>(Map.of(start, 0));
        final Map<Vector3, Integer> costSoFar = new HashMap<>(Map.of(start, 0));
        final PriorityQueue<PathNode> frontier = new PriorityQueue<>(Comparator.comparingInt(PathNode::getTotalCost));
        frontier.add(currentNode);

        final Map<Vector3, Vector3> cameFrom = new HashMap<>();
        cameFrom.put(start, null);

        while(!frontier.isEmpty()) {
            currentNode = frontier.poll();

            final Vector3 currentPosition = currentNode.getPosition();
            final Vector3 lastNode = cameFrom.get(currentPosition);
            final Direction currentDirection = lastNode != null
                    ? Direction.fromVector3(currentPosition.copy().subtract(lastNode))
                    : ship.getDirection();

            // early exit if the end position was reached
            if (currentPosition.equals(end))
                return reconstructPath(cameFrom, currentPosition);

            final boolean wasCounterCurrent = gameState.getBoard().isCounterCurrent(currentPosition);
            final int gCost = costSoFar.get(currentPosition);
            final int turnCost = turnsSoFar.get(currentPosition);

            for (PathNode neighbour : getNeighbours(currentNode)) {
                final Vector3 neighbourPosition = neighbour.getPosition();
                final boolean isCounterCurrent = gameState.getBoard().isCounterCurrent(neighbourPosition);
                final int newGCost = gCost + (!wasCounterCurrent && isCounterCurrent ? 2 : 1);
                final int newTurnCost = turnCost + currentDirection.costTo(Direction.fromVector3(neighbourPosition.copy().subtract(currentPosition)));

                if(newGCost < costSoFar.getOrDefault(neighbourPosition, Integer.MAX_VALUE)
                        || newTurnCost < turnsSoFar.getOrDefault(neighbourPosition, Integer.MAX_VALUE)) {
                    neighbour.setGraphCost(newGCost);
                    neighbour.setHeuristicCost(getEstimatedPathCost(neighbourPosition, end));
                    neighbour.setTurnCost(newTurnCost);

                    frontier.add(neighbour);
                    turnsSoFar.put(neighbourPosition, newTurnCost);
                    costSoFar.put(neighbourPosition, newGCost);
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
    static LinkedList<Vector3> reconstructPath(Map<Vector3, Vector3> cameFrom, Vector3 destination) {
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
    static int getEstimatedPathCost(Vector3 start, Vector3 end) {
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
    static List<PathNode> getNeighbours(PathNode node) {
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
    static synchronized PathNode getOrCreateNode(Vector3 position) {
        return CACHE.computeIfAbsent(position, PathNode::new);
    }

}
