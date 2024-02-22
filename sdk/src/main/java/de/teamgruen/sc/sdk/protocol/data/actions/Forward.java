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
    public void perform(GameState gameState, Ship ship) {
        ship.getPosition().translate(ship.getDirection().toVector3().multiply(this.distance));
    }

}
