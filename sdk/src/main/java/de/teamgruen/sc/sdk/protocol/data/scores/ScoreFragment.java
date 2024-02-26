package de.teamgruen.sc.sdk.protocol.data.scores;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreFragment {

    private String name;
    private ScoreAggregation aggregation;
    private boolean relevantForRanking;

}
