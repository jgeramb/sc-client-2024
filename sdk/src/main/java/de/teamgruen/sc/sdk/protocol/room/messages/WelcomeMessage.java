package de.teamgruen.sc.sdk.protocol.room.messages;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.Data;

@Data
public class WelcomeMessage implements RoomMessage {

    @JacksonXmlProperty(isAttribute = true, localName = "color")
    private Team team;

}
