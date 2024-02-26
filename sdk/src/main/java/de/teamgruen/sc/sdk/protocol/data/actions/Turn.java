package de.teamgruen.sc.sdk.protocol.data.actions;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.Data;

@Data
public class Turn implements Action {

    @JacksonXmlProperty(isAttribute = true)
    private Direction direction;

    @Override
    public void perform(GameState gameState) {
        gameState.getPlayerShip().setDirection(this.direction);
    }

}
