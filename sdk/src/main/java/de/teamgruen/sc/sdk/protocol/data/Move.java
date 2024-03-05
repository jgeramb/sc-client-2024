/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import de.teamgruen.sc.sdk.protocol.data.actions.*;
import de.teamgruen.sc.sdk.protocol.serialization.SubTypeListDeserializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonDeserialize(using = Move.Deserializer.class)
public record Move(@JacksonXmlElementWrapper List<Action> actions) {

    public static class Deserializer extends SubTypeListDeserializer<Move, Action> {

        private static final Map<String, Class<? extends Action>> SUB_TYPES = Map.of(
                "acceleration", ChangeVelocity.class,
                "advance", Forward.class,
                "push", Push.class,
                "turn", Turn.class
        );

        public Deserializer() {
            super(Move.class, "actions", SUB_TYPES);
        }

        @Override
        public Move getNewInstance() {
            return new Move(new ArrayList<>());
        }

    }

}
