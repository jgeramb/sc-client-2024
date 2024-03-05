/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.actions;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import lombok.Data;
import lombok.NonNull;

@Data
public class ChangeVelocity implements Action {

    @JacksonXmlProperty(isAttribute = true, localName = "acc")
    private int deltaVelocity;

    @Override
    public void perform(@NonNull GameState gameState) {
        final Ship ship = gameState.getPlayerShip();
        ship.setSpeed(ship.getSpeed() + this.deltaVelocity);
    }

}
