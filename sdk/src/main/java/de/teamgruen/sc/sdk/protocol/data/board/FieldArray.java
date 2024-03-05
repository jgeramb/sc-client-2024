/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.board;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.data.board.fields.*;
import de.teamgruen.sc.sdk.protocol.serialization.SubTypeListDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@JacksonXmlRootElement(localName = "field-array")
@JsonDeserialize(using = FieldArray.Deserializer.class)
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
            return new FieldArray(new ArrayList<>());
        }

    }

}
