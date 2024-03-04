package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChangeVelocityTest {

    private GameState gameState;

    @BeforeEach
    public void setUp() {
        this.gameState = new ExampleGameState();
    }

    @Test
    public void testPerform() {
        final Ship playerShip = this.gameState.getPlayerShip();
        final int initialSpeed = playerShip.getSpeed();

        final ChangeVelocity action = ActionFactory.changeVelocity(2);
        action.perform(this.gameState);

        assertEquals(initialSpeed + 2, playerShip.getSpeed());
    }

}
