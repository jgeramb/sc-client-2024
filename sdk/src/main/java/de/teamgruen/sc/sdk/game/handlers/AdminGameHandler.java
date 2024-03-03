package de.teamgruen.sc.sdk.game.handlers;

import java.util.List;

public interface AdminGameHandler extends GameHandler {

    default void onRoomCreated(String roomId, List<String> reservations) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void onPlayerJoined(int playerCount) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
