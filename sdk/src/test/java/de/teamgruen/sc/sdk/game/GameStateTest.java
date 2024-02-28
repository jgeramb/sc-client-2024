package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Position;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameStateTest {

    @Test
    public void testUpdateShips() {
        final GameState gameState = new GameState(Team.ONE);
        gameState.updateShips(List.of(new ShipData(Team.TWO, Direction.LEFT, 4, 3, 2, 1, 0, new Position(1, 2, 3))));

        final Ship stateShip = gameState.getEnemyShip();

        assertEquals(Team.TWO, stateShip.getTeam());
        assertEquals(new Vector3(1, 2, 3), stateShip.getPosition());
        assertEquals(Direction.LEFT, stateShip.getDirection());
        assertEquals(4, stateShip.getSpeed());
        assertEquals(3, stateShip.getCoal());
        assertEquals(2, stateShip.getPassengers());
        assertEquals(1, stateShip.getFreeTurns());
        assertEquals(0, stateShip.getPoints());
    }

    @Test
    public void testUpdateShips_NoShipForTeam() {
        final GameState gameState = new GameState(Team.ONE);
        gameState.getShips().remove(gameState.getEnemyShip());


        assertThrows(NoSuchElementException.class, () -> gameState.updateShips(List.of(new ShipData(Team.TWO, Direction.LEFT, 4, 3, 2, 1, 0, new Position(1, 2, 3)))));
    }

    @Test
    public void testGetShip() {
        final GameState gameState = new GameState(Team.ONE);
        gameState.getPlayerShip().setPoints(5);

        assertEquals(5, gameState.getShip(gameState.getPlayerTeam()).getPoints());
    }

}
