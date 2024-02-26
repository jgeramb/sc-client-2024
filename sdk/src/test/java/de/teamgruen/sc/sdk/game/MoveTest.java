package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoveTest {

    @Test
    public void testCopy() {
        final Move move = new Move(new Vector3(1, 2, 3), Direction.DOWN_LEFT);
        move.turn(Direction.UP_RIGHT);
        move.forward(1, 1);
        move.push(Direction.DOWN_RIGHT);
        move.passenger();
        move.segment(1, 2);
        move.finish();

        assertEquals(move, move.copy());
    }

    @Test
    public void testAppend() {
        final Move move = new Move(new Vector3(2, 1, -3), Direction.UP_RIGHT);
        move.turn(Direction.RIGHT);
        move.forward(1, 1);
        move.push(Direction.DOWN_RIGHT);
        move.passenger();
        move.segment(1, 4);
        move.finish();

        final Move childMove = new Move(new Vector3(3, 1, -4), Direction.UP_RIGHT);
        childMove.turn(Direction.DOWN_RIGHT);
        childMove.forward(1, 1);
        childMove.push(Direction.DOWN_LEFT);
        childMove.passenger();
        childMove.segment(2, 1);
        childMove.finish();

        move.append(childMove);

        assertEquals(new Vector3(3, 2, -5), move.getEndPosition());
        assertEquals(Direction.DOWN_RIGHT, move.getEndDirection());
        assertEquals(2, move.getDistance());
        assertEquals(4, move.getTotalCost());
        assertEquals(2, move.getPushes());
        assertEquals(2, move.getPassengers());
        assertEquals(2, move.getSegmentIndex());
        assertEquals(1, move.getSegmentColumn());
        assertTrue(move.isFinished());
    }

    @Test
    public void testTurn() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.turn(Direction.LEFT);

        assertEquals(Direction.LEFT, move.getEndDirection());
    }

    @Test
    public void testPush() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.push(Direction.LEFT);

        assertEquals(1, move.getPushes());
    }

    @Test
    public void testForward() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.forward(2, 2);

        assertEquals(new Vector3(2, 0, -2), move.getEndPosition());
    }

    @Test
    public void testPassenger() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.passenger();

        assertEquals(1, move.getPassengers());
    }

    @Test
    public void testSegment() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.segment(1, 2);

        assertEquals(1, move.getSegmentIndex());
        assertEquals(2, move.getSegmentColumn());
    }

    @Test
    public void testFinish() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.finish();

        assertTrue(move.isFinished());
    }

    @Test
    public void testGetAcceleration_Decelerate() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.forward(2, 2);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(1);

        assertEquals(1, move.getAcceleration(ship));
    }

    @Test
    public void testGetAcceleration_Accelerate() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.forward(2, 2);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(3);

        assertEquals(-1, move.getAcceleration(ship));
    }

    @Test
    public void testGetAcceleration_NoChange() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.forward(2, 2);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(2);

        assertEquals(0, move.getAcceleration(ship));
    }

    @Test
    public void testGetMinTurns_Zero() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);

        assertEquals(0, move.getMinTurns(new ExampleGameState()));
    }

    @Test
    public void testGetMinTurns_One() {
        final Move move = new Move(new Vector3(1, -1, 0), Direction.RIGHT);

        assertEquals(1, move.getMinTurns(new ExampleGameState()));
    }

    @Test
    public void testGetMinTurns_Two() {
        final Move move = new Move(new Vector3(4, 3, -7), Direction.DOWN_RIGHT);

        assertEquals(2, move.getMinTurns(new ExampleGameState()));
    }

    @Test
    public void testGetCoalCost() {
        final Move move = new Move(new Vector3(0, 0, 0), Direction.RIGHT);
        move.forward(2, 2);
        move.push(Direction.DOWN_RIGHT);
        move.turn(Direction.DOWN_LEFT);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(1);

        assertEquals(2, move.getCoalCost(ship));
    }

}
