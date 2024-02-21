package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.protocol.data.Team;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;

import java.util.List;
import java.util.Map;

public interface GameHandler {

    // lobby
    void onRoomJoin(String roomId);

    // game
    void onBoardUpdate(GameState gameState);
    List<Action> getNextActions(GameState gameState);
    void onGameEnd(Map<ScoreFragment, Integer> scores, Team winner);

    // other
    void onError(String message);

}
