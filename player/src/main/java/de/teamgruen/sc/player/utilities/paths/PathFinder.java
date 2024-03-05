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
     */
    public static List<Vector3> findPath(@NonNull Vector3 start, @NonNull Vector3 end) {
        final List<PathNode> openNodes = new ArrayList<>();
        final List<PathNode> closedNodes = new ArrayList<>();

        PathNode lastNode = getOrCreateNode(start);
        Direction lastDirection = gameState.getPlayerShip().getDirection();

        final PathNode endNode = getOrCreateNode(end);
        PathNode currentNode = lastNode;
        currentNode.setGraphCost(0);
        currentNode.setHeuristicCost(getEstimatedPathCost(start, end));

        openNodes.add(currentNode);

        while(!openNodes.isEmpty()) {
            // get the node with the lowest fCost
            currentNode = openNodes.stream()
                    .min(Comparator.comparingInt(PathNode::getTotalCost))
                    .orElse(null);

            openNodes.remove(currentNode);
            closedNodes.add(currentNode);

            final Vector3 deltaPosition = currentNode.getPosition().copy().subtract(lastNode.getPosition());
            final int gCost = currentNode.getGraphCost()
                    + lastDirection.costTo(Direction.fromVector3(deltaPosition))
                    + 1;

            // check if end is reached
            if(closedNodes.contains(endNode))
                break;

            for(PathNode neighbour : getNeighbours(currentNode)) {
                if (neighbour.isObstacle() || closedNodes.contains(neighbour))
                    continue;

                if (!openNodes.contains(neighbour)) {
                    neighbour.setGraphCost(gCost);
                    neighbour.setHeuristicCost(getEstimatedPathCost(neighbour.getPosition(), end));

                    openNodes.add(neighbour);
                } else if (neighbour.getTotalCost() > gCost + neighbour.getHeuristicCost())
                    neighbour.setGraphCost(gCost);
            }

            lastNode = currentNode;
        }

        // backtrace the path
        final List<Vector3> finalPath = new ArrayList<>();

        if(closedNodes.contains(endNode)) {
            currentNode = endNode;

            finalPath.add(end);

            for (int i = endNode.getGraphCost() - 1; i >= 0; i--) {
                final int currentG = i;
                final List<PathNode> currentNeighbours = getNeighbours(currentNode);

                currentNode = closedNodes
                        .stream()
                        .filter(currentNeighbours::contains)
                        .filter(node -> node.getGraphCost() == currentG)
                        .findFirst()
                        .orElse(null);

                if (currentNode == null)
                    break;

                finalPath.add(currentNode.getPosition());
            }

            Collections.reverse(finalPath);
        }

        return finalPath;
    }

    /**
     * Returns the estimated path cost from start to end.
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

            if (isInBounds(neighbourPosition))
                neighbours.add(getOrCreateNode(neighbourPosition));
        }

        return neighbours;
    }

    /**
     * @param position the position to check
     * @return whether the given position is a field on the board
     */
    private static boolean isInBounds(Vector3 position) {
        return gameState.getBoard().getFieldAt(position) != null;
    }

    /**
     * @param position the position to get or create a node for
     * @return the node for the given position
     */
    private static PathNode getOrCreateNode(Vector3 position) {
        return CACHE.computeIfAbsent(position, PathFinder::createPathNode);
    }

    /**
     * @param position the position to create a node for
     * @return the new node
     */
    private static PathNode createPathNode(Vector3 position) {
        return new PathNode(position, gameState.getBoard().isBlocked(position));
    }

}
