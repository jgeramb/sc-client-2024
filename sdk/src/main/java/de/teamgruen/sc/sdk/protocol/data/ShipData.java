package de.teamgruen.sc.sdk.protocol.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "ship")
public class ShipData {

    @JacksonXmlProperty(isAttribute = true)
    private Team team;
    @JacksonXmlProperty(isAttribute = true)
    private Direction direction;
    @JacksonXmlProperty(isAttribute = true)
    private int speed, coal, passengers, freeTurns, points;
    private Position position;

}
