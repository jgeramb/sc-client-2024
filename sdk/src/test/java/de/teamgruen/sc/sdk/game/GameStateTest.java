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
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
        assertEquals(4, stateShip.getSpeed());
        assertEquals(3, stateShip.getCoal());
        assertEquals(2, stateShip.getPassengers());
        assertEquals(1, stateShip.getFreeTurns());
        assertEquals(0, stateShip.getPoints());
    }

    @Test
    public void testUpdateShips_NoShipForTeam() {
        this.gameState.getShips().remove(this.gameState.getEnemyShip());

        assertThrows(NoSuchElementException.class, () -> this.gameState.updateShips(List.of(new ShipData(Team.TWO, Direction.LEFT, 4, 3, 2, 1, 0, false, new Position(1, 2, 3)))));
    }

    @Test
    public void testGetShip() {
        this.gameState.getPlayerShip().setPoints(5);

        assertEquals(5, this.gameState.getShip(this.gameState.getPlayerTeam()).getPoints());
    }

    @Test
    public void testGetDirectionCosts_ZeroMaxTurns() {
        assertEquals(1, this.gameState.getDirectionCosts(Direction.RIGHT, new Vector3(1, 0, -1), 0).size());
    }

    @Test
    public void testGetDirectionCosts_NoAvailableTurns() {
        final Map<Direction, Integer> actualDirectionCosts = this.gameState.getDirectionCosts(
                Direction.DOWN_LEFT,
                new Vector3(-3, 5, -2),
                1
        );

        assertTrue(actualDirectionCosts.isEmpty());
    }

    @Test
    public void testGetDirectionCosts() {
        final Map<Direction, Integer> actualDirectionCosts = this.gameState.getDirectionCosts(
                Direction.RIGHT,
                new Vector3(0, 1, -1),
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
        assertEquals(0, this.gameState.getMinTurns(Direction.RIGHT, new Vector3(1, 0, -1)));
    }

    @Test
    public void testGetMinTurns_One() {
        assertEquals(1, this.gameState.getMinTurns(Direction.RIGHT, new Vector3(2, 0, -2)));
    }

    @Test
    public void testGetMinTurns_Two() {
        assertEquals(2, this.gameState.getMinTurns(Direction.DOWN_RIGHT, new Vector3(-3, 5, -2)));
    }

    @Test
    public void testGetMinTurns_Three() {
        assertEquals(3, this.gameState.getMinTurns(Direction.DOWN_LEFT, new Vector3(-3, 5, -2)));
    }

    @Test
    public void testGetBestPushDirection_None() {
        this.gameState.getEnemyShip().setPosition(new Vector3(-3, 5, -2));

        assertNull(this.gameState.getBestPushDirection(Direction.DOWN_LEFT));
    }

    @Test
    public void testGetBestPushDirection_CounterCurrent() {
        this.gameState.getEnemyShip().setPosition(new Vector3(0, 4, -4));

        assertEquals(Direction.DOWN_LEFT, this.gameState.getBestPushDirection(Direction.DOWN_RIGHT));
    }

    @Test
    public void testGetBestPushDirection_Obstacles() {
        this.gameState.getEnemyShip().setPosition(new Vector3(1, 4, -5));

        assertEquals(Direction.RIGHT, this.gameState.getBestPushDirection(Direction.DOWN_RIGHT));
    }

    @Test
    public void testGetBestPushDirection_MinTurns() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setPosition(new Vector3(1, 5, -6));
        enemyShip.setDirection(Direction.DOWN_LEFT);

        assertEquals(Direction.DOWN_LEFT, this.gameState.getBestPushDirection(Direction.RIGHT));
    }

    @Test
    public void testGetBestPushDirection_SegmentDistance() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setPosition(new Vector3(2, 4, -6));
        enemyShip.setDirection(Direction.DOWN_LEFT);

        assertEquals(Direction.UP_LEFT, this.gameState.getBestPushDirection(Direction.RIGHT));
    }

    @Test
    public void testGetBestPushDirection_NoGoal() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setPosition(new Vector3(-5, 9, -4));
        enemyShip.setPassengers(2);
        enemyShip.setSpeed(3);
        enemyShip.setCoal(1);

        assertEquals(Direction.RIGHT, this.gameState.getBestPushDirection(Direction.DOWN_LEFT));
    }

    @Test
    public void testGetAdvanceLimit_OutOfMap() {
        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(-1, 0, 1),
                Direction.LEFT,
                0,
                1,
                0,
                0
        );

        assertEquals(AdvanceInfo.Result.BLOCKED, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Blocked() {
        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(0, -1, 1),
                Direction.RIGHT,
                0,
                1,
                0,
                0
        );

        assertEquals(AdvanceInfo.Result.BLOCKED, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Ship() {
        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(-1, 1, 0),
                Direction.LEFT,
                0,
                1,
                0,
                0
        );

        assertEquals(AdvanceInfo.Result.SHIP, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_CounterCurrent_Enter() {
        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(-1, 1, 0),
                Direction.RIGHT,
                0,
                1,
                0,
                0
        );

        assertEquals(AdvanceInfo.Result.COUNTER_CURRENT, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_CounterCurrent_Move() {
        this.gameState.getPlayerShip().setSpeed(2);

        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(-1, 0, 1),
                Direction.RIGHT,
                0,
                2,
                0,
                0
        );

        assertEquals(AdvanceInfo.Result.NORMAL, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Goal_Water() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(6);
        playerShip.setPassengers(2);

        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(-5, 8, -3),
                Direction.DOWN_LEFT,
                0,
                1,
                1,
                4
        );

        assertEquals(AdvanceInfo.Result.GOAL, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Goal_CounterCurrent() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(6);
        playerShip.setPassengers(2);

        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(-5, 9, -4),
                Direction.DOWN_LEFT,
                0,
                2,
                1,
                3
        );

        assertEquals(AdvanceInfo.Result.GOAL, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Passenger() {
        this.gameState.getPlayerShip().setSpeed(2);

        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(2, 1, -3),
                Direction.DOWN_RIGHT,
                0,
                2,
                1,
                0
        );

        assertEquals(AdvanceInfo.Result.PASSENGER, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Passenger_NotReachable() {
        this.gameState.getPlayerShip().setSpeed(2);

        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(2, 1, -3),
                Direction.DOWN_RIGHT,
                1,
                2,
                1,
                0
        );

        assertEquals(AdvanceInfo.Result.NORMAL, actualAdvanceLimit.getResult());
        assertEquals(2, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Normal() {
        this.gameState.getPlayerShip().setSpeed(5);

        final AdvanceInfo actualAdvanceLimit = this.gameState.getAdvanceLimit(
                new Vector3(2, -2, 0),
                Direction.DOWN_RIGHT,
                0,
                6,
                1,
                0
        );

        assertEquals(AdvanceInfo.Result.NORMAL, actualAdvanceLimit.getResult());
        assertEquals(6, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testAppendForwardMove_Ship() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-1, 1, 0));
        playerShip.setDirection(Direction.LEFT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, new Vector3(0, -1, 1), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setResult(AdvanceInfo.Result.SHIP);

        final int cost = this.gameState.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                move,
                advanceInfo,
                2
        );

        assertEquals(2, cost);
        assertEquals(List.of(ActionFactory.forward(1), ActionFactory.push(Direction.DOWN_LEFT)), move.getActions());
    }

    @Test
    public void testAppendForwardMove_Passenger() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(2, 1, -3));
        playerShip.setDirection(Direction.DOWN_RIGHT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, new Vector3(0, -1, 1), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setDistance(1);
        advanceInfo.setCost(1);
        advanceInfo.setResult(AdvanceInfo.Result.PASSENGER);

        final int cost = this.gameState.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                move,
                advanceInfo,
                0
        );

        assertEquals(1, cost);
        assertEquals(List.of(ActionFactory.forward(1)), move.getActions());
        assertEquals(1, move.getPassengers());
    }

    @Test
    public void testAppendForwardMove_Goal() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-5, 8, -3));
        playerShip.setDirection(Direction.DOWN_LEFT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, new Vector3(0, -1, 1), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setDistance(1);
        advanceInfo.setCost(2);
        advanceInfo.setResult(AdvanceInfo.Result.GOAL);

        final int cost = this.gameState.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                move,
                advanceInfo,
                0
        );

        assertEquals(2, cost);
        assertEquals(List.of(ActionFactory.forward(1)), move.getActions());
        assertTrue(move.isGoal());
    }

    @Test
    public void testAppendForwardMove_Normal() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(2, -2, 0));
        playerShip.setDirection(Direction.DOWN_RIGHT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, new Vector3(0, -1, 1), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setDistance(6);
        advanceInfo.setCost(6);
        advanceInfo.setResult(AdvanceInfo.Result.NORMAL);

        final int cost = this.gameState.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                move,
                advanceInfo,
                0
        );

        assertEquals(6, cost);
        assertEquals(List.of(ActionFactory.forward(6)), move.getActions());
    }

    @Test
    public void testGetMoves() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-1, -1, 2));

        final List<List<Action>> actualMoves = this.gameState.getMoves(
                playerShip.getPosition(),
                new Vector3(0, -1, 1),
                playerShip.getDirection(),
                playerShip.getFreeTurns(),
                1,
                1,
                2,
                playerShip.getCoal()
        ).stream().map(Move::getActions).toList();
        final List<List<Action>> expectedMoves = List.of(
                List.of(ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1), ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1), ActionFactory.turn(Direction.RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.forward(1)),
                List.of(ActionFactory.forward(1), ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.forward(1), ActionFactory.turn(Direction.LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.forward(1), ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.DOWN_LEFT), ActionFactory.forward(1))
        );

        assertTrue(expectedMoves.containsAll(actualMoves));
    }

}