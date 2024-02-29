package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Position;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testGetDirectionCosts_ZeroMaxTurns() {
        final GameState gameState = new ExampleGameState();

        assertEquals(1, gameState.getDirectionCosts(Direction.RIGHT, new Vector3(1, 0, -1), 0).size());
    }

    @Test
    public void testGetDirectionCosts_NoAvailableTurns() {
        final GameState gameState = new ExampleGameState();

        final Map<Direction, Integer> actualDirectionCosts = gameState.getDirectionCosts(
                Direction.DOWN_RIGHT,
                new Vector3(4, 3, -7),
                1
        );

        assertTrue(actualDirectionCosts.isEmpty());
    }

    @Test
    public void testGetDirectionCosts() {
        final GameState gameState = new ExampleGameState();

        final Map<Direction, Integer> actualDirectionCosts = gameState.getDirectionCosts(
                Direction.RIGHT,
                new Vector3(1, 1, -2),
                3
        );

        assertEquals(0, actualDirectionCosts.get(Direction.RIGHT));
        assertEquals(1, actualDirectionCosts.get(Direction.DOWN_RIGHT));
        assertEquals(2, actualDirectionCosts.get(Direction.DOWN_LEFT));
        assertEquals(3, actualDirectionCosts.get(Direction.LEFT));
        assertEquals(2, actualDirectionCosts.get(Direction.UP_LEFT));
        assertEquals(1, actualDirectionCosts.get(Direction.UP_RIGHT));
    }

    @Test
    public void testGetMinTurns_Zero() {
        final GameState gameState = new ExampleGameState();

        assertEquals(0, gameState.getMinTurns(Direction.RIGHT, new Vector3(1, 0, -1)));
    }

    @Test
    public void testGetMinTurns_One() {
        final GameState gameState = new ExampleGameState();

        assertEquals(1, gameState.getMinTurns(Direction.DOWN_LEFT, new Vector3(5, 3, -8)));
    }

    @Test
    public void testGetMinTurns_Two() {
        final GameState gameState = new ExampleGameState();

        assertEquals(2, gameState.getMinTurns(Direction.RIGHT, new Vector3(4, 3, -7)));
    }

}
