package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.util.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@RequiredArgsConstructor
public class Ship {

    private final Team team;
    private final Set<Vector3> visitedSegments = new HashSet<>();
    private boolean pushed = false;
    private int passengers = 0, coal = 6, speed = 1;
    private Direction direction = Direction.RIGHT;
    private Vector3 position = null;

}
