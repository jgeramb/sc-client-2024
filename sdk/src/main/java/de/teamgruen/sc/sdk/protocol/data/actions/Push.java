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
    public void perform(GameState gameState) {
        final Ship enemyShip = gameState.getEnemyShip();
        enemyShip.getPosition().add(this.direction.toVector3());
        enemyShip.setFreeTurns(2);
    }

}
