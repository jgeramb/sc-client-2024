package de.teamgruen.sc.sdk.protocol.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.game.util.Vector3;
import lombok.Data;

@Data
public class Position {

    @JacksonXmlProperty(isAttribute = true)
    private int q, r, s;

    public Vector3 toVector3() {
        return new Vector3(this.q, this.r, this.s);
    }

}
