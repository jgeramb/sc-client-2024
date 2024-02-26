package de.teamgruen.sc.player.utilities.paths;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
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
     */
    public static List<Vector3> findPath(Vector3 start, Vector3 end) {
        final List<PathNode> openNodes = new ArrayList<>();
        final List<PathNode> closedNodes = new ArrayList<>();

        final PathNode endNode = getOrCreateNode(end);
        PathNode currentNode = getOrCreateNode(start);
        currentNode.setGraphCost(0);
        currentNode.setHeuristicCost(getEstimatedPathCost(start, end));

        openNodes.add(currentNode);

        while(!openNodes.isEmpty()) {
            // Sort open nodes by fCost (ascending)
            openNodes.sort(Comparator.comparingInt(PathNode::getTotalCost));
            openNodes.sort(Comparator.comparingInt(PathNode::getGraphCost).reversed());
            currentNode = openNodes.get(0);

            openNodes.remove(currentNode);
            closedNodes.add(currentNode);

            int g = currentNode.getGraphCost() + 1;

            // Check if end is reached
            if(closedNodes.contains(endNode))
                break;

            for(PathNode neighbour : getNeighbours(currentNode)) {
                if (neighbour.isObstacle())
                    continue;

                if (closedNodes.contains(neighbour))
                    continue;

                if (!openNodes.contains(neighbour)) {
                    neighbour.setGraphCost(g);
                    neighbour.setHeuristicCost(getEstimatedPathCost(neighbour.getPosition(), end));

                    openNodes.add(neighbour);
                } else if (neighbour.getTotalCost() > g + neighbour.getHeuristicCost())
                    neighbour.setGraphCost(g);
            }
        }

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

    private static int getEstimatedPathCost(Vector3 start, Vector3 end) {
        final int manhattenDistance = Math.max(
                Math.abs(start.getQ() - end.getQ()),
                Math.max(
                        Math.abs(start.getS() - end.getS()),
                        Math.abs(start.getR() - end.getR())
                )
        );
        int requiredTurns;

        try {
            requiredTurns = Direction.fromVector3(start).costTo(Direction.fromVector3(end));
        } catch (IllegalArgumentException ignore) {
            requiredTurns = 0;
        }

        return manhattenDistance + requiredTurns;
    }

    private static List<PathNode> getNeighbours(PathNode node) {
        final List<PathNode> neighbours = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            final Vector3 neighbourPosition = node.getPosition().copy().add(direction.toVector3());

            if (isInBounds(neighbourPosition))
                neighbours.add(getOrCreateNode(neighbourPosition));
        }

        return neighbours;
    }

    private static boolean isInBounds(Vector3 position) {
        return gameState.getBoard().getFieldAt(position) != null;
    }

    private static PathNode getOrCreateNode(Vector3 position) {
        return CACHE.computeIfAbsent(position, PathFinder::createPathNode);
    }

    private static PathNode createPathNode(Vector3 position) {
        return new PathNode(position, gameState.getBoard().isBlocked(position));
    }

}
