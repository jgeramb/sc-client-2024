/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;

public class PathFinder {

    @Setter
    private static GameState gameState;

    /**
     * Find the shortest path from start to end using the A* algorithm.
     * @param direction the direction the ship is facing
     * @param start the start position
     * @param end the end position
     * @return the shortest path from start to end (start and end included)
     *         or null if no path was found
     */
    public static List<Vector3> findPath(@NonNull Direction direction, @NonNull Vector3 start, @NonNull Vector3 end) {
        PathNode currentNode = new PathNode(start);

        final Map<Vector3, PathNode> allNodes = new HashMap<>();
        final Queue<PathNode> frontier = new PriorityQueue<>();
        frontier.add(currentNode);

        while(!frontier.isEmpty()) {
            currentNode = frontier.poll();

            final Vector3 currentPosition = currentNode.getPosition();

            // early exit if the end position was reached
            if (currentPosition.equals(end))
                return reconstructPath(currentNode);

            final Direction currentDirection = currentNode.getDirection().orElse(direction);
            final boolean wasCounterCurrent = gameState.getBoard().isCounterCurrent(currentPosition);
            final int gCost = currentNode.getGraphCost();
            final int turnCost = currentNode.getTurnCost();

            for (Vector3 neighbour : getNeighbours(currentPosition)) {
                final PathNode neighbourNode = allNodes.getOrDefault(neighbour, new PathNode(neighbour));
                final Direction neighbourDirection = Direction.fromVector3(neighbour.copy().subtract(currentPosition));

                final boolean isCounterCurrent = gameState.getBoard().isCounterCurrent(neighbour);
                final int newGCost = gCost + ((!wasCounterCurrent || currentDirection != neighbourDirection) && isCounterCurrent ? 2 : 1);
                final int newTurnCost = turnCost + currentDirection.costTo(neighbourDirection);

                if(!allNodes.containsKey(neighbour) || newGCost < neighbourNode.getGraphCost() || newTurnCost < neighbourNode.getTurnCost()) {
                    neighbourNode.setPreviousNode(currentNode);
                    neighbourNode.setGraphCost(newGCost);
                    neighbourNode.setHeuristicCost(getEstimatedPathCost(neighbour, end));
                    neighbourNode.setTurnCost(newTurnCost);

                    frontier.add(neighbourNode);
                    allNodes.put(neighbour, neighbourNode);
                }
            }
        }

        return null;
    }

    /**
     * Reconstructs the path from the destination to the start.
     * @param destination the destination
     * @return the reconstructed path
     */
    static List<Vector3> reconstructPath(@NonNull PathNode destination) {
        final List<Vector3> path = new ArrayList<>();
        PathNode current = destination;

        do {
            path.add(0, current.getPosition());
        } while((current = current.getPreviousNode()) != null);

        return path;
    }

    /**
     * Returns the Manhattan distance between the start and end position.
     * @param start the start position
     * @param end the end position
     * @return the estimated path cost
     */
    static int getEstimatedPathCost(@NonNull Vector3 start, @NonNull Vector3 end) {
        return Math.max(
                Math.abs(start.getQ() - end.getQ()),
                Math.max(
                        Math.abs(start.getS() - end.getS()),
                        Math.abs(start.getR() - end.getR())
                )
        );
    }

    /**
     * @param position the current position
     * @return the neighbours of the given position
     */
    static List<Vector3> getNeighbours(@NonNull Vector3 position) {
        final List<Vector3> neighbours = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            final Vector3 neighbourPosition = position.copy().add(direction.toVector3());

            if (!gameState.getBoard().isBlocked(neighbourPosition))
                neighbours.add(neighbourPosition);
        }

        return neighbours;
    }

}
