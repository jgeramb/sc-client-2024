package de.teamgruen.sc.sdk.protocol.data.scores;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.Data;

@Data
public class Winner {

    @JacksonXmlProperty(isAttribute = true)
    private Team team;

}
