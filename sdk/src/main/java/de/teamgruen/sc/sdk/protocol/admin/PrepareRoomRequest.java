package de.teamgruen.sc.sdk.protocol.admin;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.data.RoomSlot;

import java.util.List;

@JacksonXmlRootElement(localName = "prepare")
public record PrepareRoomRequest(@JacksonXmlProperty(isAttribute = true)
                                 String gameType,
                                 @JacksonXmlProperty(isAttribute = true)
                                 boolean pause,
                                 @JacksonXmlProperty(localName = "slot")
                                 @JacksonXmlElementWrapper(useWrapping = false)
                                 List<RoomSlot> slots) implements AdminXMLProtocolPacket {
}
