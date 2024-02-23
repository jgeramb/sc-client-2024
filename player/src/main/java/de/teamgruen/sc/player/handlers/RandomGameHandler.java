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
        final long startTime = System.currentTimeMillis();

        try {
            return ActionUtil.getRandomCombination(gameState)
                    .toActions(gameState.getPlayerShip());
        } finally {
            this.logger.debug("Time: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

}
