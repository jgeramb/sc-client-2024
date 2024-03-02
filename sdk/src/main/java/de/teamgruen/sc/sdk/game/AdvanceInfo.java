package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.Data;

@Data
public class AdvanceInfo {

    private AdvanceInfo.Result result = AdvanceInfo.Result.NORMAL;
    private int cost, distance;

    public void incrementCost() {
        this.cost++;
    }

    public void incrementDistance() {
        this.distance++;
    }

    /**
     * Returns the end position of the advance.
     *
     * @param position  the start position
     * @param direction the direction of the advance
     * @return the end position
     */
    public Vector3 getEndPosition(Vector3 position, Direction direction) {
        return position.copy().add(direction.toVector3().multiply(this.distance));
    }

    public enum Result {

        NORMAL,
        COUNTER_CURRENT,
        BLOCKED,
        SHIP,
        PASSENGER,
        FINISH

    }

}
