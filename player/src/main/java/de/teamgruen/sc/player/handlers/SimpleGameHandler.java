package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;

import java.util.Collections;
import java.util.List;

public class SimpleGameHandler extends BaseGameHandler {

    public SimpleGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onBoardUpdate(GameState gameState) {
    }

    @Override
    public List<Action> getNextActions(GameState gameState) {
        return Collections.emptyList();
    }

}
