package de.teamgruen.sc.sdk.protocol.data;

import de.teamgruen.sc.sdk.game.util.Vector3;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Direction {

    RIGHT(1, 0, -1),
    DOWN_RIGHT(0, 1, -1),
    DOWN_LEFT(-1, 1, 0),
    LEFT(-1, 0, 1),
    UP_LEFT(0, -1, 1),
    UP_RIGHT(1, -1, 0);

    private final int q, r, s;

    public Vector3 toVector3() {
        return new Vector3((short) q, (short) r, (short) s);
    }

}
