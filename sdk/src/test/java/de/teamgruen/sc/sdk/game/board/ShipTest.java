package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Team;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShipTest {

    private Ship ship;

    @BeforeAll
    public void setUp() {
        this.ship = new Ship(Team.ONE);
    }

    @Test
    public void testGetTeam() {
        assertEquals(Team.ONE, this.ship.getTeam());
    }

    @Test
    public void testGetCoal() {
        this.ship.setCoal(5);

        assertEquals(5, this.ship.getCoal());
    }

    @Test
    public void testGetSpeed() {
        this.ship.setSpeed(2);

        assertEquals(2, this.ship.getSpeed());
    }

    @Test
    public void testGetDirection() {
        this.ship.setDirection(Direction.LEFT);

        assertEquals(Direction.LEFT, this.ship.getDirection());
    }

    @Test
    public void testGetPosition() {
        this.ship.setPosition(new Vector3(1, 2, 3));

        assertEquals(new Vector3(1, 2, 3), this.ship.getPosition());
    }

    @Test
    public void testHasEnoughPassengers_NotEnough() {
        this.ship.setPassengers(1);

        assertFalse(this.ship.hasEnoughPassengers());
    }

    @Test
    public void testHasEnoughPassengers_Enough() {
        this.ship.setPassengers(2);

        assertTrue(this.ship.hasEnoughPassengers());
    }

    @Test
    public void testGetFreeTurns() {
        assertEquals(1, this.ship.getFreeTurns());
    }

}
