package de.teamgruen.sc.sdk.protocol.requests;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;

@JacksonXmlRootElement(localName = "join")
public class JoinGameRequest implements XMLProtocolPacket {
}