package de.teamgruen.sc.sdk.protocol.requests;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;

@JacksonXmlRootElement(localName = "join")
public record JoinGameRequest(@JacksonXmlProperty(isAttribute = true)
                              String gameType) implements XMLProtocolPacket {
}