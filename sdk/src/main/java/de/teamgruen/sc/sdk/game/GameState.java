/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Getter
@Setter
@RequiredArgsConstructor
public class GameState {

    protected final Board board = new Board();
    protected final List<Ship> ships = Arrays.stream(Team.values()).map(Ship::new).toList();
    protected final Team playerTeam;
    protected GamePhase gamePhase = GamePhase.LOBBY;
    protected Team currentTeam;
    protected int turn;

    public Ship getShip(Team team) {
        return this.ships.stream()
                .filter(ship -> ship.getTeam() == team)
                .findFirst()
                .orElse(null);
    }

    public Ship getPlayerShip() {
        return this.getShip(this.playerTeam);
    }

    public Ship getEnemyShip() {
        return this.ships.stream().filter(ship -> ship.getTeam() != this.playerTeam).findFirst().orElse(null);
    }

    /**
     * Updates the ships with the new ship data list.
     *
     * @param shipDataList The new ship data list
     */
    public void updateShips(@NonNull List<ShipData> shipDataList) {
        shipDataList.forEach(ship -> {
            final Ship stateShip = this.getShip(ship.getTeam());

            if(stateShip == null)
                throw new NoSuchElementException("Ship not found for team " + ship.getTeam());

            stateShip.setPosition(ship.getPosition().toVector3());
            stateShip.setDirection(ship.getDirection());
            stateShip.setPassengers(ship.getPassengers());
            stateShip.setCoal(ship.getCoal());
            stateShip.setSpeed(ship.getSpeed());
            stateShip.setFreeTurns(ship.getFreeTurns());
            stateShip.setPoints(ship.getPoints());
            stateShip.setStuck(ship.isStuck());
        });
    }

    /**
     * Returns the minimum movement points for the current game state.
     * @param ship the ship to get the minimum movement points for
     * @return the minimum movement points
     */
    public int getMinMovementPoints(Ship ship) {
        return Math.max(1, ship.getSpeed() - 1);
    }

    /**
     * Returns the maximum movement points for the current game state.
     * @param ship the ship to get the maximum movement points for
     * @return the maximum movement points
     */
    public int getMaxMovementPoints(Ship ship) {
        return Math.min(6, ship.getSpeed() + 1);
    }

}
