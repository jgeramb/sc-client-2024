/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.actions.Turn;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class Move {

    private Vector3 endPosition;
    private Direction endDirection;
    private final List<Action> actions = new ArrayList<>();
    private int distance = 0, totalCost = 0;
    private int passengers = 0, pushes = 0, segmentIndex = 0, segmentColumn = 0;
    private boolean finished = false;

    private Move() {}

    public Move(Vector3 endPosition, Direction endDirection) {
        this.endPosition = endPosition.copy();
        this.endDirection = endDirection;
    }

    public Move copy() {
        final Move move = new Move();
        move.append(this);

        return move;
    }

    public void append(Move move) {
        this.endPosition = move.endPosition;
        this.endDirection = move.endDirection;
        this.actions.addAll(move.actions);
        this.distance += move.distance;
        this.totalCost += move.totalCost;
        this.passengers += move.passengers;
        this.pushes += move.pushes;
        this.segmentIndex = move.segmentIndex;
        this.segmentColumn = move.segmentColumn;
        this.finished = move.finished;
    }

    public void turn(Direction direction) {
        this.endDirection = direction;
        this.actions.add(ActionFactory.turn(direction));
    }

    public void push(Direction direction) {
        this.actions.add(ActionFactory.push(direction));
        this.totalCost++;
        this.pushes++;
    }

    public void forward(int total, int cost) {
        this.endPosition.add(this.endDirection.toVector3().multiply(total));
        this.actions.add(ActionFactory.forward(total));
        this.distance += total;
        this.totalCost += cost;
    }

    public void passenger() {
        this.passengers++;
    }

    public void segment(int index, int column) {
        this.segmentIndex = index;
        this.segmentColumn = column;
    }

    public void finish() {
        this.finished = true;
    }

    public int getAcceleration(Ship ship) {
        return this.totalCost - ship.getSpeed();
    }

    public int getMinTurns(GameState gameState) {
        return gameState.getMinTurns(this.endDirection, this.endPosition);
    }

    public int getCoalCost(Ship ship) {
        Direction direction = ship.getDirection();
        int turnCosts = -ship.getFreeTurns();

        for (Action action : this.actions) {
            if (action instanceof Turn turn) {
                final Direction newDirection = turn.getDirection();

                turnCosts += direction.costTo(newDirection);
                direction = newDirection;
            }
        }

        return Math.max(0, Math.abs(this.getAcceleration(ship)) - 1) + Math.max(0, turnCosts);
    }

}
