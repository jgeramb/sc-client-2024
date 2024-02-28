package de.teamgruen.sc.sdk.game;

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
    public void testDefaultOnGameEnd() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onGameEnd(null, null));
    }

    @Test
    public void testDefaultOnError() {
        assertThrows(UnsupportedOperationException.class, () -> (new GameHandler() {}).onError(null));
    }

}
