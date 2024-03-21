/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoveTest {

    @Test
    public void testToString() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.DOWN_LEFT);
        move.turn(Direction.UP_RIGHT);
        move.forward(1, 1);
        move.segment(1, 2);

        assertEquals("Move(endPosition=Vector3(q=1, r=-1, s=0), enemyEndPosition=Vector3(q=1, r=1, s=1), endDirection=UP_RIGHT, actions=[Turn(direction=UP_RIGHT), Forward(distance=1)], distance=1, totalCost=1, passengers=0, pushes=0, segmentIndex=1, segmentColumn=2, goal=false)", move.toString());
    }

    @Test
    public void testCopy() {
        final Move move = new Move(new Vector3(1, 2, 3), new Vector3(3, 2, 1), Direction.DOWN_LEFT);
        move.turn(Direction.UP_RIGHT);
        move.forward(1, 1);
        move.push(Direction.DOWN_RIGHT);
        move.passenger();
        move.segment(1, 2);
        move.goal();

        assertEquals(move, move.copy());
    }

    @Test
    public void testTurn() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.turn(Direction.LEFT);

        assertEquals(Direction.LEFT, move.getEndDirection());
    }

    @Test
    public void testPush() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.push(Direction.LEFT);

        assertEquals(new Vector3(0, 1, 2), move.getEnemyEndPosition());
        assertEquals(1, move.getPushes());
        assertEquals(1, move.getTotalCost());
    }

    @Test
    public void testForward_Merge() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.forward(2, 2);
        move.forward(1, 1);

        assertEquals(new Vector3(3, 0, -3), move.getEndPosition());
        assertEquals(3, move.getDistance());
        assertEquals(3, move.getTotalCost());
    }

    @Test
    public void testForward() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.forward(2, 2);

        assertEquals(new Vector3(2, 0, -2), move.getEndPosition());
        assertEquals(2, move.getDistance());
        assertEquals(2, move.getTotalCost());
    }

    @Test
    public void testPassenger() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.passenger();

        assertEquals(1, move.getPassengers());
    }

    @Test
    public void testSegment() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.segment(1, 2);

        assertEquals(1, move.getSegmentIndex());
        assertEquals(2, move.getSegmentColumn());
    }

    @Test
    public void testGoal() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.goal();

        assertTrue(move.isGoal());
    }

    @Test
    public void testGetAcceleration_Decelerate() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.forward(2, 2);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(1);

        assertEquals(1, move.getAcceleration(ship));
    }

    @Test
    public void testGetAcceleration_Accelerate() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.forward(2, 2);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(3);

        assertEquals(-1, move.getAcceleration(ship));
    }

    @Test
    public void testGetAcceleration_NoChange() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.forward(2, 2);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(2);

        assertEquals(0, move.getAcceleration(ship));
    }

    @Test
    public void testAppend() {
        final Move move = new Move(new Vector3(-1, 0, 1), new Vector3(1, 0, -1), Direction.UP_RIGHT);
        move.turn(Direction.RIGHT);
        move.forward(2, 2);
        move.push(Direction.DOWN_RIGHT);
        move.passenger();
        move.segment(0, 2);
        move.goal();

        final Move childMove = new Move(new Vector3(1, 0, -1), new Vector3(1, 1, -2), Direction.RIGHT);
        childMove.turn(Direction.DOWN_RIGHT);
        childMove.forward(1, 1);
        childMove.push(Direction.RIGHT);
        childMove.passenger();
        childMove.segment(2, 1);
        childMove.goal();

        move.append(childMove);

        assertEquals(new Vector3(1, 1, -2), move.getEndPosition());
        assertEquals(new Vector3(2, 1, -3), move.getEnemyEndPosition());
        assertEquals(Direction.DOWN_RIGHT, move.getEndDirection());
        assertEquals(3, move.getDistance());
        assertEquals(5, move.getTotalCost());
        assertEquals(2, move.getPushes());
        assertEquals(2, move.getPassengers());
        assertEquals(2, move.getSegmentIndex());
        assertEquals(1, move.getSegmentColumn());
        assertTrue(move.isGoal());
    }

    @Test
    public void testGetCoalCost() {
        final Move move = new Move(new Vector3(0, 0, 0), new Vector3(1, 1, 1), Direction.RIGHT);
        move.forward(2, 2);
        move.push(Direction.DOWN_RIGHT);
        move.turn(Direction.DOWN_LEFT);

        final Ship ship = new Ship(Team.ONE);
        ship.setSpeed(1);

        assertEquals(2, move.getCoalCost(ship));
    }

}
