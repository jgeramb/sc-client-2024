package de.teamgruen.sc.sdk.protocol.data.board.fields;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passenger implements Field {

    @JacksonXmlProperty(isAttribute = true)
    private Direction direction;
    @JacksonXmlProperty(isAttribute = true)
    private int passenger;

    @Override
    public boolean isObstacle() {
        return true;
    }

}
