package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForwardTest {

    private GameState gameState;

    @BeforeEach
    public void setUp() {
        this.gameState = new ExampleGameState();
    }

    @Test
    public void testPerform() {
        final Ship playerShip = this.gameState.getPlayerShip();
        final Vector3 initialPosition = playerShip.getPosition().copy();

        final Forward action = ActionFactory.forward(2);
        action.perform(this.gameState);

        final Vector3 expectedPosition = initialPosition.add(playerShip.getDirection().toVector3().multiply(2));

        assertEquals(expectedPosition, playerShip.getPosition());
    }

}
