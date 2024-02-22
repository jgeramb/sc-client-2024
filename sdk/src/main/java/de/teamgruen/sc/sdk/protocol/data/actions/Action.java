package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;

public interface Action {

    void perform(GameState gameState, Ship ship);

}
