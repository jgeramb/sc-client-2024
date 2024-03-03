package de.teamgruen.sc.sdk.game.handlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameHandlerTest {

    @Test
    public void testDefaultOnRoomJoin() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onRoomJoin(null));
    }

    @Test
    public void testDefaultOnGameStart() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onGameStart(null));
    }

    @Test
    public void testDefaultOnBoardUpdate() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onBoardUpdate(null));
    }

    @Test
    public void testDefaultGetNextActions() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).getNextActions(null));
    }

    @Test
    public void testDefaultOnResults() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onResults(null, null));
    }

    @Test
    public void testDefaultOnError() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onError(null));
    }

}
