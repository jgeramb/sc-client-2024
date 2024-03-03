package de.teamgruen.sc.sdk.game.handlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdminGameHandlerTest {

    @Test
    public void testDefaultOnRoomCreated() {
        assertThrows(UnsupportedOperationException.class, () -> (new AdminGameHandler() {}).onRoomCreated(null, null));
    }

    @Test
    public void testDefaultOnPlayerJoined() {
        assertThrows(UnsupportedOperationException.class, () -> (new AdminGameHandler() {}).onPlayerJoined(0));
    }

}
