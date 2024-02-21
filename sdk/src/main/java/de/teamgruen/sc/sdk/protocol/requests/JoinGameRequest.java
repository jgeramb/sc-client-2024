package de.teamgruen.sc.sdk.protocol.requests;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "join")
public class JoinGameRequest implements XMLProtocolPacket {

    @JacksonXmlProperty(isAttribute = true)
    private String gameType = null;

}
