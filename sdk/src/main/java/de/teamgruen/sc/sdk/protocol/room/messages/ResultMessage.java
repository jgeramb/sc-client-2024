package de.teamgruen.sc.sdk.protocol.room.messages;


import de.teamgruen.sc.sdk.protocol.data.scores.Definition;
import de.teamgruen.sc.sdk.protocol.data.scores.Scores;
import de.teamgruen.sc.sdk.protocol.data.scores.Winner;
import lombok.Data;

@Data
public class ResultMessage implements RoomMessage {

    private Definition definition;
    private Scores scores;
    private Winner winner;

}
