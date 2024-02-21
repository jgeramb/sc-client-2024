package de.teamgruen.sc.sdk.protocol.data.board;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Position;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "segment")
public class SegmentData {

    @JacksonXmlProperty(isAttribute = true)
    private Direction direction;
    private Position center;
    @JacksonXmlProperty(localName = "field-array")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<FieldArray> columns;

}
