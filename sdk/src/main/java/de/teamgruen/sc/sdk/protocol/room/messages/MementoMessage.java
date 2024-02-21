package de.teamgruen.sc.sdk.protocol.room.messages;

import de.teamgruen.sc.sdk.protocol.data.State;
import lombok.Data;

@Data
public class MementoMessage implements RoomMessage {

    private State state;

}
