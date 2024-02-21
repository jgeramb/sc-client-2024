package de.teamgruen.sc.sdk.protocol.data.scores;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

@Data
public class Definition {

    @JacksonXmlProperty(localName = "fragment")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ScoreFragment> fragments;

}
