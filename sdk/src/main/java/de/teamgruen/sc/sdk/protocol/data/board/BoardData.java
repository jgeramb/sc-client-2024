package de.teamgruen.sc.sdk.protocol.data.board;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "board")
public class BoardData {

    @JacksonXmlProperty(isAttribute = true)
    private Direction nextDirection;
    @JacksonXmlProperty(localName = "segment")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<SegmentData> segments;

}
