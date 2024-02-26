package de.teamgruen.sc.sdk.protocol.requests;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@JacksonXmlRootElement(localName = "joinRoom")
public class JoinRoomRequest implements XMLProtocolPacket {

    @JacksonXmlProperty(isAttribute = true)
    private final String roomId;

}
