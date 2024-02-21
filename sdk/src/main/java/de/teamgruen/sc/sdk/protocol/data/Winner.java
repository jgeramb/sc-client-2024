package de.teamgruen.sc.sdk.protocol.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class Winner {

    @JacksonXmlProperty(isAttribute = true)
    private Team team;

}
