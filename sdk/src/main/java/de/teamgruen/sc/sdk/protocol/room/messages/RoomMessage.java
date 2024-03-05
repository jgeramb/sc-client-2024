/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.room.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WelcomeMessage.class, name = "welcomeMessage"),
        @JsonSubTypes.Type(value = MementoMessage.class, name = "memento"),
        @JsonSubTypes.Type(value = MoveRequestMessage.class, name = "moveRequest"),
        @JsonSubTypes.Type(value = ResultMessage.class, name = "result"),
        @JsonSubTypes.Type(value = ErrorMessage.class, name = "error"),
})
public interface RoomMessage {
}