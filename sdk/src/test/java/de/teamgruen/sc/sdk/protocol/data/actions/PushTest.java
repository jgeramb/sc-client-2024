package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PushTest {

    private GameState gameState;

    @BeforeEach
    public void setUp() {
        this.gameState = new ExampleGameState();
    }

    @Test
    public void testPerform() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        final Vector3 initialPosition = enemyShip.getPosition().copy();

        final Push action = ActionFactory.push(Direction.RIGHT);
        action.perform(this.gameState);

        final Vector3 expectedPosition = initialPosition.add(action.getDirection().toVector3());

        assertEquals(expectedPosition, enemyShip.getPosition());
        assertEquals(2, enemyShip.getFreeTurns());
    }

}
