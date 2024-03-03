package de.teamgruen.sc.sdk.protocol.admin;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "prepared")
public class PreparedRoomResponse implements AdminXMLProtocolPacket {

    @JacksonXmlProperty(isAttribute = true)
    private String roomId;
    @JacksonXmlProperty(localName = "reservation")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<String> reservations;

}
