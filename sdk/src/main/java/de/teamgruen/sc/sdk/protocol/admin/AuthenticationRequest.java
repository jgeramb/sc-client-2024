package de.teamgruen.sc.sdk.protocol.admin;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "authenticate")
public record AuthenticationRequest(@JacksonXmlProperty(isAttribute = true)
                                    String password) implements AdminXMLProtocolPacket {
}
