/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.actions.Forward;
import de.teamgruen.sc.sdk.protocol.data.actions.Turn;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class Move {

    private Vector3 endPosition, enemyEndPosition;
    private Direction endDirection;
    private final List<Action> actions = new ArrayList<>();
    private int distance = 0, totalCost = 0;
    private int passengers = 0, pushes = 0, segmentIndex = 0, segmentColumn = 0;
    private boolean goal = false;

    private Move() {}

    public Move(Vector3 endPosition, Vector3 enemyEndPosition, Direction endDirection) {
        this.endPosition = endPosition.copy();
        this.enemyEndPosition = enemyEndPosition.copy();
        this.endDirection = endDirection;
    }

    public Move copy() {
        final Move move = new Move();
        move.append(this);

        return move;
    }

    public Move append(@NonNull Move move) {
        this.endPosition = move.endPosition;
        this.enemyEndPosition = move.enemyEndPosition;
        this.endDirection = move.endDirection;
        this.actions.addAll(move.actions);
        this.distance += move.distance;
        this.totalCost += move.totalCost;
        this.passengers += move.passengers;
        this.pushes += move.pushes;
        this.segmentIndex = move.segmentIndex;
        this.segmentColumn = move.segmentColumn;
        this.goal = move.goal;

        return this;
    }

    public void turn(@NonNull Direction direction) {
        this.endDirection = direction;
        this.actions.add(ActionFactory.turn(direction));
    }

    public void push(@NonNull Direction direction) {
        this.enemyEndPosition.add(direction.toVector3());
        this.actions.add(ActionFactory.push(direction));
        this.totalCost++;
        this.pushes++;
    }

    public void forward(int distance, int cost) {
        this.endPosition.add(this.endDirection.toVector3().multiply(distance));

        // merge with last forward action if possible
        if(!this.actions.isEmpty() && this.actions.get(this.actions.size() - 1) instanceof Forward forward)
            forward.setDistance(forward.getDistance() + distance);
        else
            this.actions.add(ActionFactory.forward(distance));

        this.distance += distance;
        this.totalCost += cost;
    }

    public void passenger() {
        this.passengers++;
    }

    public void segment(int index, int column) {
        this.segmentIndex = index;
        this.segmentColumn = column;
    }

    public void goal() {
        this.goal = true;
    }

    public int getAcceleration(@NonNull Ship ship) {
        return this.totalCost - ship.getSpeed();
    }

    public int getCoalCost(@NonNull Ship ship) {
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
