package de.teamgruen.sc.player.handlers;

import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.logging.AnsiColor;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseGameHandler implements GameHandler {

    protected final Logger logger;

    protected BaseGameHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameStart(GameState gameState) {
        this.logger.info("Game started");
    }

    @Override
    public void onGameEnd() {
        this.logger.info("Game ended");
    }

    @Override
    public void onRoomJoin(String roomId) {
        this.logger.info("Joined room " + AnsiColor.PURPLE + roomId + AnsiColor.RESET);
    }

    @Override
    public void onResults(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result) {
        final int maxNameLength = scores.keySet()
                .stream()
                .mapToInt(scoreFragment -> scoreFragment.getName().length())
                .max()
                .orElse(0);

        final AtomicInteger maxValueLength = new AtomicInteger(0);
        final Map<String, String> scoreValues = new LinkedHashMap<>();
        scores.forEach((scoreFragment, score) -> {
            if(scoreFragment.isRelevantForRanking()) {
                final String scoreValue;
                final int scoreLength;

                if(scoreFragment.getName().equals("Gewonnen")) {
                    scoreValue = result.equals(GameResult.WIN)
                            ? AnsiColor.GREEN + "✓"
                            : result.equals(GameResult.LOOSE)
                            ? AnsiColor.RED + "✕"
                            : AnsiColor.WHITE + "/";
                    scoreLength = 1;
                } else {
                    scoreValue = AnsiColor.PURPLE.toString() + score;
                    scoreLength = String.valueOf(score).length();
                }

                maxValueLength.set(Math.max(maxValueLength.get(), scoreLength));
                scoreValues.put(scoreFragment.getName(), scoreValue);
            }
        });

        final String horizontalLine = AnsiColor.BLACK + "―".repeat(7 + maxValueLength.get()) + AnsiColor.RESET;

        this.logger.info(horizontalLine);
        scores.forEach((scoreFragment, score) -> {
            final String name = scoreFragment.getName();

            if(!scoreValues.containsKey(name))
                return;

            final int valueLength = name.equals("Gewonnen") ? 1 : String.valueOf(score).length();
            final String value = scoreValues.get(name);
            final String spacer = " ".repeat(maxNameLength - scoreFragment.getName().length())
                    + " ".repeat(maxValueLength.get() - valueLength);

            this.logger.info(scoreFragment.getName() + ": " + spacer + value + AnsiColor.RESET);
        });
        this.logger.info(horizontalLine);
    }

    @Override
    public void onError(String message) {
        this.logger.error(message);
    }

}
