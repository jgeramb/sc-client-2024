package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.logging.AnsiColor;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.Team;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;

import java.util.Map;

public abstract class BaseGameHandler implements GameHandler {

    private final Logger logger;

    protected BaseGameHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onRoomJoin(String roomId) {
        this.logger.info("Joined room " + AnsiColor.PURPLE + roomId + AnsiColor.RESET);
    }

    @Override
    public void onGameEnd(Map<ScoreFragment, Integer> scores, Team winner) {
        final String spacer = AnsiColor.BLACK + "-".repeat(20) + AnsiColor.RESET;

        this.logger.info(spacer + " Results " + spacer);
        scores.forEach((scoreFragment, score) -> this.logger.info(scoreFragment.getName() + ": " + AnsiColor.PURPLE + score + AnsiColor.RESET));

        if(winner != null)
            this.logger.info("\nWinner: " + AnsiColor.PURPLE + winner.name() + AnsiColor.RESET);
    }

    @Override
    public void onError(String message) {
        this.logger.error(message);
    }

}
