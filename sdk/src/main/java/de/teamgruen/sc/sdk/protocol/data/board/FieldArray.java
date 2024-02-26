package de.teamgruen.sc.sdk.protocol.data.board;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.data.board.fields.*;
import de.teamgruen.sc.sdk.protocol.serialization.SubTypeListDeserializer;
import de.teamgruen.sc.sdk.protocol.serialization.SubTypeListSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JacksonXmlRootElement(localName = "field-array")
@JsonDeserialize(using = FieldArray.Deserializer.class)
@JsonSerialize(using = FieldArray.Serializer.class)
public class FieldArray {

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Field> fields;

    public static class Deserializer extends SubTypeListDeserializer<FieldArray, Field> {

        private static final Map<String, Class<? extends Field>> SUB_TYPES = Map.of(
                "water", Water.class,
                "island", Island.class,
                "passenger", Passenger.class,
                "goal", Finish.class
        );

        public Deserializer() {
            super(FieldArray.class, "fields", SUB_TYPES);
        }

        @Override
        public FieldArray getNewInstance() {
            FieldArray fieldArray = new FieldArray();
            fieldArray.fields = new ArrayList<>();

            return fieldArray;
        }

    }

    public static class Serializer extends SubTypeListSerializer<FieldArray, Field> {

        private static final Map<Class<? extends Field>, String> SUB_TYPES = Map.of(
                Water.class, "water",
                Island.class, "island",
                Passenger.class, "passenger",
                Finish.class, "goal"
        );

        public Serializer() {
            super(FieldArray.class, "actions", SUB_TYPES);
        }

    }

}
