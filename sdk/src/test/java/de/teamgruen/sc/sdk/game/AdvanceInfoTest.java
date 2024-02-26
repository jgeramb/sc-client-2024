package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.protocol.data.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdvanceInfoTest {

    @Test
    public void testIncrementCost() {
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.incrementCost();

        assert advanceInfo.getCost() == 1;
    }

    @Test
    public void testIncrementDistance() {
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.incrementDistance();

        assert advanceInfo.getDistance() == 1;
    }

    @Test
    public void testGetEndPosition_DistanceZero() {
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        final Vector3 position = new Vector3(0, 0, 0);
        final Direction direction = Direction.RIGHT;
        final Vector3 actualEndPosition = advanceInfo.getEndPosition(position, direction);

        assertEquals(new Vector3(0, 0, 0), actualEndPosition);
    }

    @Test
    public void testGetEndPosition_DistanceFive() {
        final AdvanceInfo advanceInfo = new AdvanceInfo();

        for (int i = 0; i < 5; i++)
            advanceInfo.incrementDistance();

        final Vector3 position = new Vector3(0, 0, 0);
        final Direction direction = Direction.RIGHT;
        final Vector3 actualEndPosition = advanceInfo.getEndPosition(position, direction);

        assertEquals(new Vector3(5, 0, -5), actualEndPosition);
    }

}
