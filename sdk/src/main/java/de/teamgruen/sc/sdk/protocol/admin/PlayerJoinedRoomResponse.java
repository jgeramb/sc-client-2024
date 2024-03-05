/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.admin;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "joinedGameRoom")
public class PlayerJoinedRoomResponse implements AdminXMLProtocolPacket {

    @JacksonXmlProperty(isAttribute = true)
    private String roomId;
    @JacksonXmlProperty(isAttribute = true)
    private int playerCount;

}
