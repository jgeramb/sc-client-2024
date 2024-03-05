/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.data.board.BoardData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "state")
public class State {

    @JacksonXmlProperty(isAttribute = true, localName = "class")
    private String className;
    @JacksonXmlProperty(isAttribute = true)
    private Team startTeam, currentTeam;
    @JacksonXmlProperty(isAttribute = true)
    private int turn;
    private BoardData board;
    @JacksonXmlProperty(localName = "ship")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ShipData> ships;
    private Move lastMove;

}
