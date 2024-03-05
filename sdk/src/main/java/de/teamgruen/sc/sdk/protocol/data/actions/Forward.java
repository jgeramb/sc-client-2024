/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.actions;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import lombok.Data;

@Data
public class Forward implements Action {

    @JacksonXmlProperty(isAttribute = true)
    private int distance;

    @Override
    public void perform(GameState gameState) {
        final Ship ship = gameState.getPlayerShip();
        ship.getPosition().add(ship.getDirection().toVector3().multiply(this.distance));
    }

}
