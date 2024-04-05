/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.player.utilities.MoveUtil;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.logging.Logger;
import lombok.NonNull;

public class SimpleGameHandler extends BaseGameHandler {

    public SimpleGameHandler(Logger logger) {
        super(logger);
    }

    @Override
    public void onBoardUpdate(@NonNull GameState gameState) {
        this.setNextMove(gameState, () -> MoveUtil.getMostEfficientMove(gameState, 750).orElse(null));
    }

}
