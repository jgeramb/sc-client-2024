/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.utilities;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.actions.ChangeVelocity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MoveUtilTest {

    private GameState gameState;

    @BeforeEach
    public void setUp() {
        this.gameState = new ExampleGameState();
    }

    @Test
    public void testGetMostEfficientMove() {
        this.gameState.getPlayerShip().setCoal(0);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(1),
                ActionFactory.turn(Direction.DOWN_RIGHT),
                ActionFactory.forward(1)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testGetMostEfficientMove_Goal() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-5, 9, -4));
        playerShip.setDirection(Direction.DOWN_RIGHT);
        playerShip.setPassengers(2);
        playerShip.setSpeed(3);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(-1),
                ActionFactory.turn(Direction.DOWN_LEFT),
                ActionFactory.forward(1)
        );

        assertTrue(actualMove.isPresent());

        final Move move = actualMove.get();

        assertEquals(expectedActions, move.getActions());
        assertTrue(move.isGoal());
    }

    @Test
    public void testGetMostEfficientMove_Passenger() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(0, 5, -5));
        playerShip.setDirection(Direction.LEFT);
        playerShip.setSpeed(2);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(-1),
                ActionFactory.turn(Direction.DOWN_LEFT),
                ActionFactory.forward(1)
        );

        assertTrue(actualMove.isPresent());

        final Move move = actualMove.get();

        assertEquals(expectedActions, move.getActions());
        assertTrue(move.getPassengers() > 0);
    }

    @Test
    public void testGetMostEfficientMove_NoForecastDueToTimeout() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-2, 4, -2));
        playerShip.setDirection(Direction.DOWN_LEFT);
        playerShip.setSpeed(1);
        playerShip.setCoal(0);
        playerShip.setFreeTurns(0);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, -1);
        final List<Action> expectedActions = List.of(
                ActionFactory.forward(1)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testGetMostEfficientMove_TurnToBestNext_CurrentDirection() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-3, 7, -4));
        playerShip.setDirection(Direction.DOWN_LEFT);
        playerShip.setPassengers(2);
        playerShip.setSpeed(2);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(1),
                ActionFactory.forward(2)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testGetMostEfficientMove_TurnToBestNext() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-2, 2, 0));
        playerShip.setDirection(Direction.DOWN_RIGHT);
        playerShip.setSpeed(1);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.forward(1),
                ActionFactory.turn(Direction.RIGHT)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testGetMostEfficientMove_LooseIntentionally() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-2, 4, -2));
        playerShip.setDirection(Direction.DOWN_LEFT);
        playerShip.setSpeed(1);
        playerShip.setCoal(0);
        playerShip.setFreeTurns(0);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.forward(1)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testGetMostEfficientMove_NoMovesAvailable() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-3, 5, -2));
        playerShip.setDirection(Direction.DOWN_LEFT);
        playerShip.setSpeed(1);
        playerShip.setCoal(0);

        assertFalse(MoveUtil.getMostEfficientMove(this.gameState, 500).isPresent());
    }

    @Test
    public void testGetMostEfficientMove_PushRequired() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-4, 7, -3));
        playerShip.setDirection(Direction.LEFT);
        playerShip.setSpeed(5);
        playerShip.setCoal(0);

        this.gameState.getEnemyShip().setPosition(new Vector3(-5, 8, -3));

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(-1),
                ActionFactory.turn(Direction.DOWN_LEFT),
                ActionFactory.forward(1),
                ActionFactory.push(Direction.DOWN_LEFT),
                ActionFactory.forward(1),
                ActionFactory.push(Direction.RIGHT)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testGetMostEfficientMove_EndsAtLastSegmentBorder() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-5, 9, -4));
        playerShip.setDirection(Direction.DOWN_LEFT);
        playerShip.setSpeed(2);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.getMostEfficientMove(this.gameState, 500);
        final List<Action> expectedActions = List.of(
                ActionFactory.forward(1),
                ActionFactory.turn(Direction.DOWN_RIGHT)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testGetPossibleMoves() {
        final Ship playerShip = this.gameState.getPlayerShip(), enemyShip = this.gameState.getEnemyShip();
        final List<List<Action>> actualMoves = MoveUtil.getPossibleMoves(
                this.gameState,
                0,
                playerShip,
                playerShip.getPosition(),
                playerShip.getDirection(),
                enemyShip,
                enemyShip.getPosition(),
                0,
                1,
                1,
                6,
                0,
                false
        ).keySet().stream().map(Move::getActions).toList();

        final List<List<Action>> expectedMoves = List.of(
                List.of(ActionFactory.forward(1)),
                List.of(ActionFactory.changeVelocity(2), ActionFactory.forward(1), ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.changeVelocity(1), ActionFactory.forward(1), ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.changeVelocity(1), ActionFactory.forward(1), ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.changeVelocity(1), ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.changeVelocity(2), ActionFactory.turn(Direction.DOWN_RIGHT), ActionFactory.forward(2)),
                List.of(ActionFactory.turn(Direction.UP_LEFT), ActionFactory.forward(1)),
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.changeVelocity(1), ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1), ActionFactory.turn(Direction.RIGHT), ActionFactory.forward(1))
        );

        assertEquals(expectedMoves.size(), actualMoves.size());
        assertTrue(expectedMoves.containsAll(actualMoves));
    }

    @Test
    public void testGetPossibleMoves_MoreCoal() {
        this.gameState.setTurn(2);

        final Ship playerShip = this.gameState.getPlayerShip(), enemyShip = this.gameState.getEnemyShip();
        final List<List<Action>> actualMoves = MoveUtil.getPossibleMoves(
                this.gameState,
                0,
                playerShip,
                new Vector3(-3, 5, -2),
                Direction.DOWN_LEFT,
                enemyShip,
                enemyShip.getPosition(),
                0,
                1,
                playerShip.getFreeTurns(),
                2,
                0,
                false
        ).keySet().stream().map(Move::getActions).toList();
        final List<List<Action>> expectedMoves = List.of(
                List.of(ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(1)),
                List.of(ActionFactory.changeVelocity(1), ActionFactory.turn(Direction.UP_RIGHT), ActionFactory.forward(2))
        );

        assertTrue(expectedMoves.containsAll(actualMoves));
    }

    @Test
    public void testAddAcceleration_Accelerate() {
        final Move move = new Move(new Vector3(-1, -1, 2), new Vector3(-2, 1, 1), Direction.RIGHT);
        move.forward(1, 2);

        MoveUtil.addAcceleration(1, move);

        assertInstanceOf(ChangeVelocity.class, move.getActions().get(0));
        assertEquals(1, ((ChangeVelocity) move.getActions().get(0)).getDeltaVelocity());
    }

    @Test
    public void testAddAcceleration_Decelerate() {
        final Move move = new Move(new Vector3(-1, -1, 2), new Vector3(-2, 1, 1), Direction.RIGHT);
        move.forward(1, 1);

        MoveUtil.addAcceleration(2, move);

        assertInstanceOf(ChangeVelocity.class, move.getActions().get(0));
        assertEquals(-1, ((ChangeVelocity) move.getActions().get(0)).getDeltaVelocity());
    }

    @Test
    public void testGetAccelerationCoal_SegmentCost() {
        assertEquals(1, MoveUtil.getAccelerationCoal(
                0,
                3,
                false,
                false,
                6
        ));
    }

    @Test
    public void testGetAccelerationCoal_EnemyAhead() {
        assertEquals(1, MoveUtil.getAccelerationCoal(
                0,
                0,
                true,
                false,
                6
        ));
    }

    @Test
    public void testGetAccelerationCoal_NoCoalAvailable() {
        assertEquals(0, MoveUtil.getAccelerationCoal(
                0,
                3,
                true,
                true,
                0
        ));
    }

    @Test
    public void testMoveFromPath_NoPath() {
        assertTrue(MoveUtil.moveFromPath(this.gameState, new LinkedList<>()).isEmpty());
    }

    @Test
    public void testMoveFromPath() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(0, 0, 0));
        playerShip.setDirection(Direction.DOWN_RIGHT);
        playerShip.setSpeed(5);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(0, 0, 0),
                new Vector3(0, 1, -1),
                new Vector3(0, 2, -2),
                new Vector3(0, 3, -3),
                new Vector3(1, 3, -4),
                new Vector3(2, 3, -5)
        )));
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(1),
                ActionFactory.forward(3),
                ActionFactory.turn(Direction.RIGHT),
                ActionFactory.forward(2)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testMoveFromPath_Push() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(0, 0, 0));
        playerShip.setDirection(Direction.DOWN_LEFT);
        playerShip.setSpeed(3);

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(0, 0, 0),
                new Vector3(-1, 1, 0),
                new Vector3(-2, 1, 1)
        )));
        final List<Action> expectedActions = List.of(
                ActionFactory.forward(1),
                ActionFactory.turn(Direction.LEFT),
                ActionFactory.forward(1),
                ActionFactory.push(Direction.UP_RIGHT)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testMoveFromPath_Push_NoDirectionAvailable() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-2, 3, -1));
        playerShip.setDirection(Direction.DOWN_RIGHT);
        playerShip.setSpeed(2);

        this.gameState.getEnemyShip().setPosition(new Vector3(-3, 5, -2));

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(-2, 3, -1),
                new Vector3(-2, 4, -2),
                new Vector3(-3, 5, -2)
        )));
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(-1),
                ActionFactory.forward(1),
                ActionFactory.turn(Direction.DOWN_LEFT)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testMoveFromPath_NotEnoughMovementPoints() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(0, 0, 0));
        playerShip.setDirection(Direction.RIGHT);
        playerShip.setSpeed(1);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(-2, 1, 1),
                new Vector3(-1, 1, 0),
                new Vector3(0, 1, -1)
        )));
        final List<Action> expectedActions = List.of(
                ActionFactory.forward(1)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testMoveFromPath_CannotTurnToDestination() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(0, 0, 0));
        playerShip.setDirection(Direction.RIGHT);
        playerShip.setSpeed(2);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(0, 0, 0),
                new Vector3(-1, 1, 0)
        )));

        assertFalse(actualMove.isPresent());
    }

    @Test
    public void testMoveFromPath_TurnToNext() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(0, 0, 0));
        playerShip.setDirection(Direction.RIGHT);
        playerShip.setSpeed(1);
        playerShip.setCoal(0);

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(0, 0, 0),
                new Vector3(1, 0, -1),
                new Vector3(2, 0, -2),
                new Vector3(2, 1, -3)
        )));
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(1),
                ActionFactory.forward(2),
                ActionFactory.turn(Direction.DOWN_RIGHT)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testMoveFromPath_ReachHigherSpeed() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(-6, 8, -2));
        playerShip.setDirection(Direction.DOWN_RIGHT);
        playerShip.setSpeed(1);
        playerShip.setCoal(1);

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(-6, 8, -2),
                new Vector3(-6, 9, -3),
                new Vector3(-6, 10, -4)
        )));
        final List<Action> expectedActions = List.of(
                ActionFactory.forward(1)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

    @Test
    public void testMoveFromPath_ReachLowerSpeed() {
        final Ship playerShip = this.gameState.getPlayerShip();
        playerShip.setPosition(new Vector3(2, -2, 0));
        playerShip.setDirection(Direction.DOWN_RIGHT);
        playerShip.setSpeed(4);
        playerShip.setCoal(1);

        final Optional<Move> actualMove = MoveUtil.moveFromPath(this.gameState, new LinkedList<>(List.of(
                new Vector3(2, -2, 0),
                new Vector3(2, -1, -1),
                new Vector3(2, 0, -2),
                new Vector3(2, 1, -3),
                new Vector3(2, 2, -4)
        )));
        final List<Action> expectedActions = List.of(
                ActionFactory.changeVelocity(-2),
                ActionFactory.forward(2)
        );

        assertTrue(actualMove.isPresent());
        assertEquals(expectedActions, actualMove.get().getActions());
    }

}
