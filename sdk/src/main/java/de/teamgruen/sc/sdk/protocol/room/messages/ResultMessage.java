/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.room.messages;


import de.teamgruen.sc.sdk.protocol.data.scores.Definition;
import de.teamgruen.sc.sdk.protocol.data.scores.Scores;
import de.teamgruen.sc.sdk.protocol.data.scores.Winner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultMessage implements RoomMessage {

    private Definition definition;
    private Scores scores;
    private Winner winner;

}
