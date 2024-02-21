package de.teamgruen.sc.sdk.protocol.data.scores;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class ScoreData {

    @JacksonXmlProperty(isAttribute = true)
    private ScoreCause cause;
    @JacksonXmlProperty(isAttribute = true)
    private String reason;
    @JacksonXmlProperty(localName = "part")
    @JacksonXmlElementWrapper(useWrapping = false)
    private int[] parts;

}
