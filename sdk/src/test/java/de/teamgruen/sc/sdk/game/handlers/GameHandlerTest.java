/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game.handlers;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameHandlerTest {

    private static final GameState GAME_STATE = new ExampleGameState();

    @Test
    public void testDefaultOnRoomJoin() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onRoomJoin(null));
    }

    @Test
    public void testDefaultOnGameStart() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onGameStart(GAME_STATE));
    }

    @Test
    public void testDefaultOnBoardUpdate() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onBoardUpdate(GAME_STATE));
    }

    @Test
    public void testDefaultGetNextActions() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).getNextActions(GAME_STATE));
    }

    @Test
    public void testDefaultOnResults() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onResults(new LinkedHashMap<>(), GameResult.WIN, null));
    }

    @Test
    public void testDefaultOnError() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onError(null));
    }

}
