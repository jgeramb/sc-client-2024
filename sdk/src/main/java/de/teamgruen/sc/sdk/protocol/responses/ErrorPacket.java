package de.teamgruen.sc.sdk.protocol.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "errorpacket")
@JsonIgnoreProperties({ "originalRequest" })
public class ErrorPacket implements XMLProtocolPacket {

    @JacksonXmlProperty(isAttribute = true)
    private String message;

}
