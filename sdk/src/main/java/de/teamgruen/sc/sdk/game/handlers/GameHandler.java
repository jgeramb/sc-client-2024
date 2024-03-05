/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game.handlers;

import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;

import java.util.LinkedHashMap;
import java.util.List;

public interface GameHandler {

    // lobby
    default void onRoomJoin(String roomId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // game
    default void onGameStart(GameState gameState) {
        throw new UnsupportedOperationException("Not implemented");
    }
    default void onBoardUpdate(GameState gameState) {
        throw new UnsupportedOperationException("Not implemented");
    }
    default List<Action> getNextActions(GameState gameState) {
        throw new UnsupportedOperationException("Not implemented");
    }
    default void onResults(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // other
    default void onError(String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
