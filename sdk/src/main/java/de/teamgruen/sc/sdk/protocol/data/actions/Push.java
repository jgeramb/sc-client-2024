package de.teamgruen.sc.sdk.protocol.data.actions;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.Data;

@Data
public class Push implements Action {

    @JacksonXmlProperty(isAttribute = true)
    private Direction direction;

    @Override
    public void perform(GameState gameState, Ship ship) {
        final Ship enemyShip = gameState.getShips()
                .stream()
                .filter(currentShip -> currentShip.getPosition().equals(ship.getPosition()))
                .findFirst()
                .orElse(null);

        if (enemyShip != null) {
            enemyShip.setPushed(true);
            enemyShip.getPosition().translate(this.direction.toVector3());
        }
    }

}
