package de.teamgruen.sc.sdk.protocol.room;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.data.Move;
import de.teamgruen.sc.sdk.protocol.data.actions.*;
import de.teamgruen.sc.sdk.protocol.exceptions.SerializationException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import java.util.Map;

@JacksonXmlRootElement(localName = "room")
public record MovePacket(@JacksonXmlProperty(isAttribute = true)
                         String roomId,
                         @JacksonXmlProperty(localName = "data")
                         @JsonSerialize(using = MovePacket.MoveSerializer.class)
                         Move move
) implements XMLProtocolPacket {

    public static class MoveSerializer extends StdSerializer<Move> {

        private static final Map<Class<? extends Action>, String> SUB_TYPES = Map.of(
                ChangeVelocity.class, "acceleration",
                Forward.class, "advance",
                Push.class, "push",
                Turn.class, "turn"
        );

        protected MoveSerializer() {
            super(Move.class);
        }

        @Override
        public void serialize(Move move, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws SerializationException {
            final ToXmlGenerator toXmlGenerator = (ToXmlGenerator) jsonGenerator;
            final XMLStreamWriter staxWriter = toXmlGenerator.getStaxWriter();

            try {
                staxWriter.writeStartElement("data");
                staxWriter.writeAttribute("class", "move");
                toXmlGenerator.setNextName(new QName("actions"));
                jsonGenerator.writeStartObject();

                for (Action action : move.actions()) {
                    jsonGenerator.writeFieldName(SUB_TYPES.get(action.getClass()));
                    serializerProvider.defaultSerializeValue(action, jsonGenerator);
                }

                jsonGenerator.writeEndObject();
                staxWriter.writeEndElement();
            } catch (Exception ex) {
                throw new SerializationException(ex);
            }
        }

    }

}
