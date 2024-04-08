/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game.handlers;

import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;
import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.List;

public interface GameHandler {

    // lobby
    default void onRoomJoin(String roomId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // game
    default void onGameStart(@NonNull GameState gameState) {
        throw new UnsupportedOperationException("Not implemented");
    }
    default void onBoardUpdate(@NonNull GameState gameState) {
        throw new UnsupportedOperationException("Not implemented");
    }
    default List<Action> getNextActions(@NonNull GameState gameState) {
        throw new UnsupportedOperationException("Not implemented");
    }
    default void onResults(@NonNull LinkedHashMap<ScoreFragment, Integer> scores, @NonNull GameResult result, String reason) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // other
    default void onError(String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
