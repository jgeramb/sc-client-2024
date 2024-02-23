package de.teamgruen.sc.player.util;

import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;

import java.util.ArrayList;
import java.util.List;

public record ActionCombination(Direction direction, int distance, int extraCost) {

    public int getTotalDistance(Direction startDirection) {
        return this.distance + Math.abs(startDirection.ordinal() - this.direction.ordinal());
    }

    public List<Action> toActions(Ship ship) {
        final List<Action> actions = new ArrayList<>();

        if (this.distance != ship.getSpeed())
            actions.add(ActionFactory.changeVelocity(this.distance - ship.getSpeed()));

        if (this.direction != ship.getDirection())
            actions.add(ActionFactory.turn(this.direction));

        actions.add(ActionFactory.forward(this.distance));

        return actions;
    }

}
