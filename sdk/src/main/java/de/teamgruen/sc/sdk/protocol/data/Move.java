package de.teamgruen.sc.sdk.protocol.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import de.teamgruen.sc.sdk.protocol.data.actions.*;
import de.teamgruen.sc.sdk.protocol.serialization.SubTypeListDeserializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonDeserialize(using = Move.Deserializer.class)
public class Move {

    @JacksonXmlElementWrapper
    private List<Action> actions;

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
            Move move = new Move();
            move.actions = new ArrayList<>();

            return move;
        }

    }

}
