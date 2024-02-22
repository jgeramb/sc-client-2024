package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.BoardSegment;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.game.util.Vector3;
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

    public Ship getPlayerShip() {
        return this.getShip(this.playerTeam);
    }

    public List<Ship> getEnemyShips() {
        return this.ships.stream().filter(ship -> ship.getTeam() != this.playerTeam).toList();
    }

    public void updateShips(List<ShipData> shipDataList) {
        shipDataList.forEach(ship -> {
            final Ship stateShip = this.getShip(ship.getTeam());

            if(stateShip == null)
                throw new NoSuchElementException("Ship not found for team " + ship.getTeam());

            final Vector3 position = ship.getPosition().toVector3();

            if (stateShip.getPosition() != null)
                stateShip.setPushed(!position.equals(stateShip.getPosition()));

            final BoardSegment segment = this.board.getSegmentOfField(position);

            if(segment != null && stateShip.getVisitedSegments().stream().noneMatch(segmentCenter -> segmentCenter.equals(segment.center())))
                stateShip.getVisitedSegments().add(segment.center());

            stateShip.setPosition(position);
            stateShip.setDirection(ship.getDirection());
            stateShip.setPassengers(ship.getPassengers());
            stateShip.setCoal(ship.getCoal());
            stateShip.setSpeed(ship.getSpeed());
        });
    }

}
