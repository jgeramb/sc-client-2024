/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game.board;

import de.teamgruen.sc.sdk.game.*;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Island;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Passenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BoardTest {

    private GameState gameState;
    private Board board;

    @BeforeEach
    public void setUp() {
        this.gameState = new ExampleGameState();
        this.board = gameState.getBoard();
    }

    @Test
    public void testUpdateSegments_UpdatePassengers() {
        this.board.updateSegments(ExampleGameState.getSampleSegments().stream().peek(segment ->
            segment.getColumns().forEach(column -> column.getFields().forEach(field -> {
                if (field instanceof Passenger)
                    ((Passenger) field).setPassenger(0);
            })
        )).toList());

        assertTrue(this.board.getPassengerFields().isEmpty());
    }

    @Test
    public void testUpdateSegments() {
        assertEquals(Direction.DOWN_LEFT, this.board.getNextSegmentDirection());

        final BoardSegment actualSegment = this.board.getSegments().get(1);

        final Vector3 actualCenter = actualSegment.center();
        assertEquals(0, actualCenter.getQ());
        assertEquals(4, actualCenter.getR());
        assertEquals(-4, actualCenter.getS());

        final Direction actualDirection = actualSegment.direction();
        assertEquals(Direction.DOWN_RIGHT, actualDirection);

        final LinkedHashMap<Vector3, Field> actualFields = actualSegment.fields();
        final Map.Entry<Vector3, Field> actualField = actualFields.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(new Vector3(1, 2, -3)))
                .findFirst()
                .orElse(null);

        assertNotNull(actualField);
        assertInstanceOf(Passenger.class, actualField.getValue());

        final Passenger actualPassenger = (Passenger) actualField.getValue();

        assertEquals(Direction.RIGHT, actualPassenger.getDirection());
    }

    @Test
    public void testUpdateCounterCurrent() {
        final List<Vector3> expected = Arrays.asList(
                new Vector3(-1, 0, 1),
                new Vector3(0, 0, 0),
                new Vector3(0, 1, -1),
                new Vector3(0, 2, -2),
                new Vector3(0, 3, -3),
                new Vector3(0, 4, -4),
                new Vector3(-1, 5, -4),
                new Vector3(-2, 6, -4),
                new Vector3(-3, 7, -4),
                new Vector3(-4, 8, -4),
                new Vector3(-5, 9, -4),
                new Vector3(-6, 10, -4)
        );

        assertArrayEquals(vectorsToArray(expected), vectorsToArray(this.board.getCounterCurrent()));
    }

    @Test
    public void testIsCounterCurrent() {
        assertTrue(this.board.isCounterCurrent(new Vector3(-2, 6, -4)));
    }

    @Test
    public void testGetGoalFields() {
        assertTrue(this.board.getGoalFields().containsKey(new Vector3(-6, 10, -4)));
    }

    @Test
    public void testGetPassengerFields() {
        final Field actualField = this.board.getPassengerFields().get(new Vector3(-1, 7, -6));

        assertNotNull(actualField);
        assertInstanceOf(Passenger.class, actualField);
        assertEquals(Direction.UP_LEFT, ((Passenger) actualField).getDirection());
    }

    @Test
    public void testGetFieldAt() {
        final Field actualField = this.board.getFieldAt(new Vector3(-3, 6, -3));

        assertNotNull(actualField);
        assertInstanceOf(Island.class, actualField);
    }

    @Test
    public void testIsBlocked_Null() {
        assertTrue(this.board.isBlocked(new Vector3(-2, 0, 2)));
    }

    @Test
    public void testIsBlocked_Island() {
        assertTrue(this.board.isBlocked(new Vector3(1, -1, 0)));
    }

    @Test
    public void testIsBlocked_Passenger() {
        assertTrue(this.board.isBlocked(new Vector3(1, 2, -3)));
    }

    @Test
    public void testIsBlocked_Water() {
        assertFalse(this.board.isBlocked(new Vector3(1, 0, -1)));
    }

    @Test
    public void testIsBlocked_Goal() {
        assertFalse(this.board.isBlocked(new Vector3(-4, 10, -6)));
    }

    @Test
    public void testGetSegmentColumn() {
        assertEquals(1, this.board.getSegmentColumn(new Vector3(0, 4, -4)));
    }

    @Test
    public void testGetSegmentColumn_InvalidPosition() {
        assertThrows(IllegalArgumentException.class, () -> this.board.getSegmentColumn(new Vector3(-2, 0, 2)));
    }

    @Test
    public void testGetSegmentIndex() {
        assertEquals(1, this.board.getSegmentIndex(new Vector3(0, 4, -4)));
    }

    @Test
    public void testGetSegmentIndex_InvalidPosition() {
        assertThrows(IllegalArgumentException.class, () -> this.board.getSegmentIndex(new Vector3(-2, 0, 2)));
    }

    @Test
    public void testGetSegmentPosition() {
        final double actualSegmentPosition = this.board.getSegmentPosition(new Vector3(0, 0, 0));

        assertEquals(0.25, actualSegmentPosition);
    }

    @Test
    public void testGetSegmentDistance_Positive() {
        final double actualSegmentPosition = this.board.getSegmentDistance(new Vector3(0, 0, 0), new Vector3(1, 0, -1));

        assertEquals(0.25, actualSegmentPosition);
    }

    @Test
    public void testGetSegmentDistance_Negative() {
        final double actualSegmentPosition = this.board.getSegmentDistance(new Vector3(0, 0, 0), new Vector3(-1, 0, 1));

        assertEquals(-0.25, actualSegmentPosition);
    }

    @Test
    public void testGetDirectionCosts_ZeroMaxTurns() {
        assertEquals(1, this.board.getDirectionCosts(Direction.RIGHT, new Vector3(1, 0, -1), 0).size());
    }

    @Test
    public void testGetDirectionCosts_NoAvailableTurns() {
        final Map<Direction, Integer> actualDirectionCosts = this.board.getDirectionCosts(
                Direction.DOWN_LEFT,
                new Vector3(-3, 5, -2),
                1
        );

        assertTrue(actualDirectionCosts.isEmpty());
    }

    @Test
    public void testGetDirectionCosts() {
        final Map<Direction, Integer> actualDirectionCosts = this.board.getDirectionCosts(
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
        assertEquals(0, this.board.getMinTurns(Direction.RIGHT, new Vector3(1, 0, -1)));
    }

    @Test
    public void testGetMinTurns_One() {
        assertEquals(1, this.board.getMinTurns(Direction.RIGHT, new Vector3(2, 0, -2)));
    }

    @Test
    public void testGetMinTurns_Two() {
        assertEquals(2, this.board.getMinTurns(Direction.DOWN_RIGHT, new Vector3(-3, 5, -2)));
    }

    @Test
    public void testGetMinTurns_Three() {
        assertEquals(3, this.board.getMinTurns(Direction.DOWN_LEFT, new Vector3(-3, 5, -2)));
    }

    @Test
    public void testGetBestPushDirection_None() {
        assertNull(this.board.getBestPushDirection(
                Direction.DOWN_LEFT,
                this.gameState.getEnemyShip(),
                new Vector3(-3, 5, -2),
                false
        ));
    }

    @Test
    public void testGetBestPushDirection_AlreadyStuck() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setStuck(true);

        assertNull(this.board.getBestPushDirection(
                Direction.DOWN_LEFT,
                enemyShip,
                new Vector3(-3, 5, -2),
                false
        ));
    }

    @Test
    public void testGetBestPushDirection_CounterCurrent() {
        assertEquals(Direction.DOWN_LEFT, this.board.getBestPushDirection(
                Direction.DOWN_RIGHT,
                this.gameState.getEnemyShip(),
                new Vector3(0, 4, -4),
                false
        ));
    }

    @Test
    public void testGetBestPushDirection_Obstacles() {
        assertEquals(Direction.RIGHT, this.board.getBestPushDirection(
                Direction.DOWN_RIGHT,
                this.gameState.getEnemyShip(),
                new Vector3(1, 4, -5),
                false
        ));
    }

    @Test
    public void testGetBestPushDirection_MinCoalCost() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setDirection(Direction.DOWN_LEFT);

        assertEquals(Direction.DOWN_LEFT, this.board.getBestPushDirection(
                Direction.RIGHT,
                enemyShip,
                new Vector3(1, 5, -6),
                false
        ));
    }

    @Test
    public void testGetBestPushDirection_SegmentDistance() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setDirection(Direction.DOWN_LEFT);

        assertEquals(Direction.UP_LEFT, this.board.getBestPushDirection(
                Direction.RIGHT,
                enemyShip,
                new Vector3(2, 4, -6),
                false
        ));
    }

    @Test
    public void testGetBestPushDirection_NoGoal() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setPassengers(2);
        enemyShip.setSpeed(2);

        assertEquals(Direction.RIGHT, this.board.getBestPushDirection(
                Direction.DOWN_LEFT,
                enemyShip,
                new Vector3(-5, 9, -4),
                false
        ));
    }

    @Test
    public void testGetBestPushDirection_NoPassengers() {
        final Ship enemyShip = this.gameState.getEnemyShip();
        enemyShip.setDirection(Direction.DOWN_RIGHT);
        enemyShip.setSpeed(1);

        assertEquals(Direction.DOWN_RIGHT, this.board.getBestPushDirection(
                Direction.DOWN_RIGHT,
                enemyShip,
                new Vector3(0, 5, -5),
                false
        ));
    }

    @Test
    public void testGetAdvanceLimit_OutOfMap() {
        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                this.gameState.getPlayerShip(),
                new Vector3(-1, 0, 1),
                Direction.LEFT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                1
        );

        assertEquals(AdvanceInfo.Result.BLOCKED, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Blocked() {
        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                this.gameState.getPlayerShip(),
                new Vector3(0, -1, 1),
                Direction.RIGHT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                1
        );

        assertEquals(AdvanceInfo.Result.BLOCKED, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Ship() {
        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                this.gameState.getPlayerShip(),
                new Vector3(-1, 1, 0),
                Direction.LEFT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                1
        );

        assertEquals(AdvanceInfo.Result.SHIP, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_CounterCurrent_Enter() {
        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                this.gameState.getPlayerShip(),
                new Vector3(-1, 1, 0),
                Direction.RIGHT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                1
        );

        assertEquals(AdvanceInfo.Result.COUNTER_CURRENT, actualAdvanceLimit.getResult());
        assertEquals(0, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_CounterCurrent_Move() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(2);

        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                playerShip,
                new Vector3(-1, 0, 1),
                Direction.RIGHT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                2
        );

        assertEquals(AdvanceInfo.Result.NORMAL, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Goal_Water() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(6);
        playerShip.setPassengers(2);

        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                playerShip,
                new Vector3(-5, 8, -3),
                Direction.DOWN_LEFT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                1
        );

        assertEquals(AdvanceInfo.Result.GOAL, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Goal_CounterCurrent() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(6);
        playerShip.setPassengers(2);

        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                playerShip,
                new Vector3(-5, 9, -4),
                Direction.DOWN_LEFT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                2
        );

        assertEquals(AdvanceInfo.Result.GOAL, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Passenger() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(2);

        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                playerShip,
                new Vector3(2, 1, -3),
                Direction.DOWN_RIGHT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                2
        );

        assertEquals(AdvanceInfo.Result.PASSENGER, actualAdvanceLimit.getResult());
        assertEquals(1, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Passenger_NotReachable() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(2);

        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                playerShip,
                new Vector3(2, 1, -3),
                Direction.DOWN_RIGHT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                1,
                2
        );

        assertEquals(AdvanceInfo.Result.NORMAL, actualAdvanceLimit.getResult());
        assertEquals(2, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testGetAdvanceLimit_Normal() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setSpeed(5);

        final AdvanceInfo actualAdvanceLimit = this.board.getAdvanceLimit(
                playerShip,
                new Vector3(2, -2, 0),
                Direction.DOWN_RIGHT,
                this.gameState.getEnemyShip().getPosition(),
                1,
                0,
                6
        );

        assertEquals(AdvanceInfo.Result.NORMAL, actualAdvanceLimit.getResult());
        assertEquals(6, actualAdvanceLimit.getDistance());
    }

    @Test
    public void testAppendForwardMove_Ship_NotEnoughMovementPointsLeft() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-1, 1, 0));
        playerShip.setDirection(Direction.LEFT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, playerShip.getPosition(), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setResult(AdvanceInfo.Result.SHIP);

        this.board.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                this.gameState.getEnemyShip(),
                move,
                advanceInfo,
                1,
                false
        );

        assertEquals(0, move.getTotalCost());
    }

    @Test
    public void testAppendForwardMove_Ship_NoPushDirection() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-2, 4, -2));
        playerShip.setDirection(Direction.DOWN_LEFT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, new Vector3(-3, 5, -2), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setResult(AdvanceInfo.Result.SHIP);

        this.board.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                this.gameState.getEnemyShip(),
                move,
                advanceInfo,
                2,
                false
        );

        assertEquals(0, move.getTotalCost());
    }

    @Test
    public void testAppendForwardMove_Ship() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-1, 1, 0));
        playerShip.setDirection(Direction.LEFT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, playerShip.getPosition(), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setResult(AdvanceInfo.Result.SHIP);

        this.board.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                this.gameState.getEnemyShip(),
                move,
                advanceInfo,
                2,
                false
        );

        assertEquals(2, move.getTotalCost());
        assertEquals(List.of(ActionFactory.forward(1), ActionFactory.push(Direction.UP_LEFT)), move.getActions());
    }

    @Test
    public void testAppendForwardMove_Passenger() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(2, 1, -3));
        playerShip.setDirection(Direction.DOWN_RIGHT);

        final Vector3 position = playerShip.getPosition();
        final Direction direction = playerShip.getDirection();
        final Move move = new Move(position, playerShip.getPosition(), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setDistance(1);
        advanceInfo.setCost(1);
        advanceInfo.setResult(AdvanceInfo.Result.PASSENGER);

        this.board.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                this.gameState.getEnemyShip(),
                move,
                advanceInfo,
                0,
                false
        );

        assertEquals(1, move.getTotalCost());
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
        final Move move = new Move(position, playerShip.getPosition(), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setDistance(1);
        advanceInfo.setCost(2);
        advanceInfo.setResult(AdvanceInfo.Result.GOAL);

        this.board.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                this.gameState.getEnemyShip(),
                move,
                advanceInfo,
                0,
                false
        );

        assertEquals(2, move.getTotalCost());
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
        final Move move = new Move(position, playerShip.getPosition(), direction);
        final AdvanceInfo advanceInfo = new AdvanceInfo();
        advanceInfo.setDistance(6);
        advanceInfo.setCost(6);
        advanceInfo.setResult(AdvanceInfo.Result.NORMAL);

        this.board.appendForwardMove(
                advanceInfo.getEndPosition(position, direction),
                direction,
                this.gameState.getEnemyShip(),
                move,
                advanceInfo,
                0,
                false
        );

        assertEquals(6, move.getTotalCost());
        assertEquals(List.of(ActionFactory.forward(6)), move.getActions());
    }

    @Test
    public void testGetMoves() {
        final Ship playerShip = this.gameState.getPlayerShip(), enemyShip = this.gameState.getEnemyShip();
        final List<List<Action>> actualMoves = this.board.getMoves(
                playerShip,
                new Vector3(0, -1, 1),
                Direction.RIGHT,
                enemyShip,
                new Vector3(-2, 1, 1),
                2,
                1,
                1,
                2,
                0,
                false
        ).stream().map(Move::getActions).toList();
        final List<List<Action>> expectedMoves = List.of(
                List.of(ActionFactory.turn(Direction.DOWN_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.DOWN_LEFT), ActionFactory.forward(1), ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.DOWN_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(2)),
                List.of(ActionFactory.turn(Direction.LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1), ActionFactory.turn(Direction.LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.LEFT), ActionFactory.forward(2))
        );

        assertEquals(expectedMoves.size(), actualMoves.size());
        assertTrue(expectedMoves.containsAll(actualMoves));
    }

    @Test
    public void testGetMoves_PushRequired() {
        final Ship playerShip = this.gameState.getPlayerShip(), enemyShip = this.gameState.getEnemyShip();
        enemyShip.setPassengers(2);
        enemyShip.setSpeed(2);
        enemyShip.setDirection(Direction.DOWN_RIGHT);

        final List<List<Action>> actualMoves = this.board.getMoves(
                playerShip,
                new Vector3(-5, 9, -4),
                Direction.DOWN_LEFT,
                enemyShip,
                new Vector3(-6, 10, -4),
                4,
                1,
                1,
                1,
                0,
                true
        ).stream().map(Move::getActions).toList();
        final List<List<Action>> expectedMoves = List.of(
                List.of(ActionFactory.forward(1), ActionFactory.push(Direction.RIGHT)),
                List.of(ActionFactory.forward(1), ActionFactory.push(Direction.RIGHT), ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.forward(1), ActionFactory.push(Direction.RIGHT), ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(2)),
                List.of(ActionFactory.forward(1), ActionFactory.push(Direction.RIGHT), ActionFactory.turn(Direction.RIGHT), ActionFactory.forward(1), ActionFactory.push(Direction.RIGHT))
        );

        assertEquals(expectedMoves.size(), actualMoves.size());
        assertTrue(expectedMoves.containsAll(actualMoves));
    }

    @Test
    public void testGetFieldPositions() {
        final List<Vector3> expectedPositions = Arrays.asList(
                new Vector3(-1, -2, 3), new Vector3(-1, -1, 2), new Vector3(-1, 0, 1), new Vector3(-2, 1, 1), new Vector3(-3, 2, 1),
                new Vector3(0, -2, 2), new Vector3(0, -1, 1), new Vector3(0, 0, 0), new Vector3(-1, 1, 0), new Vector3(-2, 2, 0),
                new Vector3(1, -2, 1), new Vector3(1, -1, 0), new Vector3(1, 0, -1), new Vector3(0, 1, -1), new Vector3(-1, 2, -1),
                new Vector3(2, -2, 0), new Vector3(2, -1, -1), new Vector3(2, 0, -2), new Vector3(1, 1, -2), new Vector3(0, 2, -2)
        );

        assertEquals(expectedPositions, this.board.getFieldPositions(new Vector3(0, 0, 0), Direction.RIGHT));
    }

    public int[][] vectorsToArray(List<Vector3> vectors) {
        int[][] result = new int[vectors.size()][3];

        for (int i = 0; i < vectors.size(); i++) {
            final Vector3 vector = vectors.get(i);

            result[i][0] = vector.getQ();
            result[i][1] = vector.getR();
            result[i][2] = vector.getS();
        }

        return result;
    }

}
