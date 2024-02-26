package de.teamgruen.sc.sdk.protocol.requests;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;

@JacksonXmlRootElement(localName = "joinPrepared")
public record JoinPreparedRoomRequest(@JacksonXmlProperty(isAttribute = true)
                                      String reservationCode
) implements XMLProtocolPacket {

}
