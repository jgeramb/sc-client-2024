package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.player.util.ActionUtil;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;

import java.util.List;

public class RandomGameHandler extends BaseGameHandler {

    public RandomGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onBoardUpdate(GameState gameState) {
    }

    @Override
    public List<Action> getNextActions(GameState gameState) {
        return List.of(ActionUtil.getRandomAction(gameState));
    }

}
