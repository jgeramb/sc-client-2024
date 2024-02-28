package de.teamgruen.sc.sdk.game;

import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.Position;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.Team;
import de.teamgruen.sc.sdk.protocol.data.board.FieldArray;
import de.teamgruen.sc.sdk.protocol.data.board.SegmentData;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.board.fields.FieldFactory;

import java.util.Arrays;
import java.util.List;

public class ExampleGameState extends GameState {

    public ExampleGameState() {
        super(Team.ONE);

        this.board.setNextSegmentDirection(Direction.DOWN_LEFT);
        this.board.updateSegments(getSampleSegments());
        this.getPlayerShip().setPosition(new Vector3(1, -1, 0));
        this.getEnemyShip().setPosition(new Vector3(0, 1, -1));
        this.gamePhase = GamePhase.RUNNING;
        this.currentTeam = Team.ONE;
        this.turn = 0;
    }

    public static List<SegmentData> getSampleSegments() {
        final Field water = FieldFactory.water(), island = FieldFactory.island();

        return List.of(
                /*
                I 0 0 0
                  0 I 0 0
                    0 0 I 0
                  0 0 0 I
                0 0 0 0
                */
                getSegmentData(2, 0, -2, Direction.RIGHT,
                        island, water, water, water, water,
                        water, island, water, water, water,
                        water, water, island, water, water,
                        water, water, water, island, water
                ),
                /*
                0         0
                0 P     0 0
                0 0  0  0 0
                0 0  P  P P
                  0  0  0
                     0
                */
                getSegmentData(2, 4, -6, Direction.DOWN_RIGHT,
                        water, water, water, FieldFactory.passenger(Direction.UP_LEFT, 1), water,
                        water, water, FieldFactory.passenger(Direction.UP_RIGHT, 1), water, water,
                        water, FieldFactory.passenger(Direction.RIGHT, 1), water, water, water,
                        FieldFactory.passenger(Direction.DOWN_RIGHT, 1), water, water, water, FieldFactory.finish()
                )
        );
    }

    private static SegmentData getSegmentData(int q, int r, int s, Direction direction, Field... fields) {
        final List<Field> fieldList = Arrays.asList(fields);
        final SegmentData segmentData = new SegmentData();

        segmentData.setCenter(new Position(q, r, s));
        segmentData.setDirection(direction);
        segmentData.setColumns(List.of(
                new FieldArray(fieldList.subList(0, 5)),
                new FieldArray(fieldList.subList(5, 10)),
                new FieldArray(fieldList.subList(10, 15)),
                new FieldArray(fieldList.subList(15, 20))
        ));

        return segmentData;
    }

    public static List<ShipData> getSampleShips() {
        return List.of(
                getShip(Team.ONE, new Vector3(5, 0, -5), Direction.LEFT, 1, 5, 2, 2, 16),
                getShip(Team.TWO, new Vector3(4, 0, -4), Direction.RIGHT, 2, 6, 1, 1, 10)
        );
    }

    private static ShipData getShip(Team team,
                                    Vector3 position,
                                    Direction direction,
                                    int speed, int coal,
                                    int passengers,
                                    int freeTurns,
                                    int points) {
        final ShipData ship = new ShipData();
        ship.setTeam(team);
        ship.setPosition(new Position(position.getQ(), position.getR(), position.getS()));
        ship.setDirection(direction);
        ship.setSpeed(speed);
        ship.setCoal(coal);
        ship.setPassengers(passengers);
        ship.setFreeTurns(freeTurns);
        ship.setPoints(points);

        return ship;
    }

}