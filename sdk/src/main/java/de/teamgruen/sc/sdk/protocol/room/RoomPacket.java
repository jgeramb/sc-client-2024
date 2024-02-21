package de.teamgruen.sc.sdk.protocol.room;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.room.messages.RoomMessage;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "room")
public class RoomPacket implements XMLProtocolPacket {

    @JacksonXmlProperty(isAttribute = true)
    private String roomId;
    private RoomMessage data;

}
