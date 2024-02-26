package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Ship {

    private final Team team;
    private boolean pushed = false;
    private int passengers = 0, coal = 6, speed = 1;
    private Direction direction = Direction.RIGHT;
    private Vector3 position = null;

    public boolean hasEnoughPassengers() {
        return this.passengers >= 2;
    }

    public int getFreeTurns() {
        return this.pushed ? 2 : 1;
    }

}
