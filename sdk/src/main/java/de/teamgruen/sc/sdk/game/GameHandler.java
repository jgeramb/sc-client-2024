package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;

import java.util.LinkedHashMap;
import java.util.List;

public interface GameHandler {

    // lobby
    void onRoomJoin(String roomId);

    // game
    void onGameStart(GameState gameState);
    void onBoardUpdate(GameState gameState);
    List<Action> getNextActions(GameState gameState);
    void onGameEnd(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result);

    // other
    void onError(String message);

}
