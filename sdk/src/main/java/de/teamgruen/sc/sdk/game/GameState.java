package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Data
public class GameState {

    private final Board board = new Board();
    private final List<Ship> ships = new ArrayList<>();
    private final Team playerTeam;
    private GamePhase gamePhase = GamePhase.LOBBY;
    private int turns;

    public GameState(Team playerTeam) {
        this.playerTeam = playerTeam;

        for(Team team : Team.values())
            this.ships.add(new Ship(team));
    }

    public Ship getShip(Team team) {
        return this.ships.stream()
                .filter(ship -> ship.getTeam() == team)
                .findFirst()
                .orElse(null);
    }

    public void updateShips(List<ShipData> shipDataList) {
        shipDataList.forEach(ship -> {
            final Ship stateShip = this.getShip(ship.getTeam());

            if(stateShip == null)
                throw new NoSuchElementException("Ship not found for team " + ship.getTeam());

            if (stateShip.getPosition() != null)
                stateShip.setPushed(!ship.getPosition().toVector3().equals(stateShip.getPosition()));

            stateShip.setPosition(ship.getPosition().toVector3());
            stateShip.setDirection(ship.getDirection());
            stateShip.setPassengers(ship.getPassengers());
            stateShip.setCoal(ship.getCoal());
            stateShip.setSpeed(ship.getSpeed());
        });
    }

}
