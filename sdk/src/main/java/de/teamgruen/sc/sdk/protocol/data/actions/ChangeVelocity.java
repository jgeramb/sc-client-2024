package de.teamgruen.sc.sdk.protocol.data.actions;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import lombok.Data;

@Data
public class ChangeVelocity implements Action {

    @JacksonXmlProperty(isAttribute = true, localName = "acc")
    private int deltaVelocity;

    @Override
    public void perform(GameState gameState, Ship ship) {
        ship.setSpeed(ship.getSpeed() + this.deltaVelocity);
    }

}
