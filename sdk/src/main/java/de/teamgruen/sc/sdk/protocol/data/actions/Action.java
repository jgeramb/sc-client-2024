package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;

public interface Action {

    void perform(Board board, Ship ship);

}
