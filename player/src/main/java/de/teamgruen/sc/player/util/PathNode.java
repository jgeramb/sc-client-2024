package de.teamgruen.sc.player.util;

import de.teamgruen.sc.sdk.game.util.Vector3;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class PathNode {

    private final Vector3 position;
    private final boolean isObstacle;
    private int gCost, hCost;

    public int getFCost() {
        return this.gCost + this.hCost;
    }

}
