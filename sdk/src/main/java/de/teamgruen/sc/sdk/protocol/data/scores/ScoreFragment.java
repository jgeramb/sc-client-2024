package de.teamgruen.sc.sdk.protocol.data.scores;

import lombok.Data;

@Data
public class ScoreFragment {

    private String name;
    private ScoreAggregation aggregation;
    private boolean relevantForRanking;

}
