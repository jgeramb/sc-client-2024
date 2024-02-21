package de.teamgruen.sc.sdk.protocol.room.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties({ "originalMessage" })
public class ErrorMessage implements RoomMessage {

    @JacksonXmlProperty(isAttribute = true)
    private String message;

}
