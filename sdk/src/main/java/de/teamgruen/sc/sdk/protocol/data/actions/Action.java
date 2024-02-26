package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.game.GameState;

public interface Action {

    void perform(GameState gameState);

}
