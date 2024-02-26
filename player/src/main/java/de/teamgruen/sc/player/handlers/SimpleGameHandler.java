package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.player.utilities.MoveUtil;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.Move;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SimpleGameHandler extends BaseGameHandler {

    private List<Action> nextActions = null;

    public SimpleGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onBoardUpdate(GameState gameState) {
        if(!gameState.getPlayerTeam().equals(gameState.getCurrentTeam()))
            return;

        final Ship playerShip = gameState.getPlayerShip();
        final Optional<Move> optionalMove = MoveUtil.getMostEfficientMove(gameState);

        if(optionalMove.isEmpty()) {
            this.nextActions = null;
            return;
        }

        final Move move = optionalMove.get();
        final List<Action> actions = move.getActions();
        actions.forEach(action -> action.perform(gameState));
        playerShip.setCoal(playerShip.getCoal() - move.getCoalCost(playerShip));

        this.nextActions = actions;
    }

    @Override
    public List<Action> getNextActions(GameState gameState) {
        if(this.nextActions == null || this.nextActions.isEmpty()) {
            this.onError("No actions available");
            return Collections.emptyList();
        }

        return this.nextActions;
    }

}
