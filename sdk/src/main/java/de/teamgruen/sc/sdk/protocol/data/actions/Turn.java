/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.actions;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.Data;
import lombok.NonNull;

@Data
public class Turn implements Action {

    @JacksonXmlProperty(isAttribute = true)
    private Direction direction;

    @Override
    public void perform(@NonNull GameState gameState) {
        gameState.getPlayerShip().setDirection(this.direction);
    }

}
