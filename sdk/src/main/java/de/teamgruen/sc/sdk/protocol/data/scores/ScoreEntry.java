package de.teamgruen.sc.sdk.protocol.data.scores;

import lombok.Data;

@Data
public class ScoreEntry {

    private ScorePlayer player;
    private ScoreData score;

}
