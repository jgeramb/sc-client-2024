/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Position;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class GameStateTest {

    private GameState gameState;

    @BeforeEach
    public void setUp() {
        this.gameState = new ExampleGameState();
    }

    @Test
    public void testUpdateShips() {
        this.gameState.updateShips(List.of(new ShipData(Team.TWO, Direction.LEFT, 4, 3, 2, 1, 0, false, new Position(1, 2, 3))));

        final Ship stateShip = this.gameState.getEnemyShip();

        assertEquals(Team.TWO, stateShip.getTeam());
        assertEquals(new Vector3(1, 2, 3), stateShip.getPosition());
        assertEquals(Direction.LEFT, stateShip.getDirection());
        assertFalse(stateShip.isStuck());
        assertEquals(4, stateShip.getSpeed());
        assertEquals(3, stateShip.getCoal());
        assertEquals(2, stateShip.getPassengers());
        assertEquals(1, stateShip.getFreeTurns());
        assertEquals(0, stateShip.getPoints());
    }

    @Test
    public void testUpdateShips_NoShipForTeam() {
        assertThrows(NoSuchElementException.class, () -> this.gameState.updateShips(List.of(new ShipData(null, Direction.LEFT, 4, 3, 2, 1, 0, false, new Position(1, 2, 3)))));
    }

    @Test
    public void testGetShip() {
        this.gameState.getPlayerShip().setPoints(5);

        assertEquals(5, this.gameState.getShip(this.gameState.getPlayerTeam()).getPoints());
    }

    @Test
    public void testGetMinMovementPoints() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(6);

        assertEquals(5, this.gameState.getMinMovementPoints(playerShip));
    }

    @Test
    public void testGetMinMovementPoints_CappedMin() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(1);

        assertEquals(1, this.gameState.getMinMovementPoints(playerShip));
    }

    @Test
    public void testGetMaxMovementPoints() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(1);

        assertEquals(2, this.gameState.getMaxMovementPoints(playerShip));
    }

    @Test
    public void testGetMaxMovementPoints_CappedMax() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(6);

        assertEquals(6, this.gameState.getMaxMovementPoints(playerShip));
    }

}