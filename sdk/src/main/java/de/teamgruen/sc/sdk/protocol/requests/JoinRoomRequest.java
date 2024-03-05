/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.requests;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;

@JacksonXmlRootElement(localName = "joinRoom")
public record JoinRoomRequest(@JacksonXmlProperty(isAttribute = true)
                              String roomId
) implements XMLProtocolPacket {
}
