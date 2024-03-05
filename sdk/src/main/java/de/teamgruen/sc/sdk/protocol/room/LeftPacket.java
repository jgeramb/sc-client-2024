/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.room;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "left")
public class LeftPacket implements XMLProtocolPacket {

    @JacksonXmlProperty(isAttribute = true)
    private String roomId;

}
