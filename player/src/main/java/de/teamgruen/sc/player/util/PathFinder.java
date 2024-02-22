package de.teamgruen.sc.player.util;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Water;
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
        currentNode.setGCost(0);
        currentNode.setHCost(getEstimatedPathCost(start, end));

        openNodes.add(currentNode);

        while(!openNodes.isEmpty()) {
            // Sort open nodes by fCost (ascending)
            openNodes.sort(Comparator.comparingInt(PathNode::getFCost));
            openNodes.sort(Comparator.comparingInt(PathNode::getGCost).reversed());
            currentNode = openNodes.get(0);

            openNodes.remove(currentNode);
            closedNodes.add(currentNode);

            int g = currentNode.getGCost() + 1;

            // Check if end is reached
            if(closedNodes.contains(endNode))
                break;

            for(PathNode neighbour : getNeighbours(currentNode)) {
                if (neighbour.isObstacle())
                    continue;

                if (closedNodes.contains(neighbour))
                    continue;

                if (!openNodes.contains(neighbour)) {
                    neighbour.setGCost(g);
                    neighbour.setHCost(getEstimatedPathCost(neighbour.getPosition(), end));

                    openNodes.add(neighbour);
                } else if (neighbour.getFCost() > g + neighbour.getHCost())
                    neighbour.setGCost(g);
            }
        }

        final List<Vector3> finalPath = new ArrayList<>();

        if(closedNodes.contains(endNode)) {
            currentNode = endNode;

            finalPath.add(end);

            for (int i = endNode.getGCost() - 1; i >= 0; i--) {
                final int currentG = i;
                final List<PathNode> currentNeighbours = getNeighbours(currentNode);

                currentNode = closedNodes
                        .stream()
                        .filter(currentNeighbours::contains)
                        .filter(node -> node.getGCost() == currentG)
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
                        Math.abs(start.getR() - end.getR()),
                        Math.abs(start.getS() - end.getS())
                )
        );
        final int requiredTurns;
        final Direction startDirection = Direction.fromVector3(start);
        final Direction endDirection = Direction.fromVector3(end);

        if (startDirection != null && endDirection != null)
            requiredTurns = Math.abs(startDirection.ordinal() - endDirection.ordinal());
        else
            requiredTurns = 0;

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
        return new PathNode(position, !(gameState.getBoard().getFieldAt(position) instanceof Water));
    }

}
