package de.teamgruen.sc.sdk.protocol.serialization;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.Vector3;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.data.*;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.BoardData;
import de.teamgruen.sc.sdk.protocol.data.scores.*;
import de.teamgruen.sc.sdk.protocol.exceptions.DeserializationException;
import de.teamgruen.sc.sdk.protocol.exceptions.SerializationException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinPreparedRoomRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinRoomRequest;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.LeftPacket;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;
import de.teamgruen.sc.sdk.protocol.room.messages.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PacketSerializationUtilTest {

    @Test
    public void testParseXMLTagName_Incomplete() {
        assertNull(PacketSerializationUtil.parseXMLTagName("<"));
    }

    @Test
    public void testParseXMLTagName_Incomplete2() {
        assertNull(PacketSerializationUtil.parseXMLTagName("<tag"));
    }

    @Test
    public void testParseXMLTagName_Valid() {
        assertEquals("tag", PacketSerializationUtil.parseXMLTagName("<tag>"));
    }

    @Test
    public void testParseXMLTagName_Valid2() {
        assertEquals("tag", PacketSerializationUtil.parseXMLTagName("<tag attr=\"value\">"));
    }

    @Test
    public void testDeserialize_Null() {
        assertThrows(IllegalArgumentException.class, () -> PacketSerializationUtil.deserialize(null));
    }

    @Test
    public void testDeserialize_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> PacketSerializationUtil.deserialize("<"));
    }

    @Test
    public void testDeserialize_Unknown() {
        assertThrows(DeserializationException.class, () -> PacketSerializationUtil.deserialize("<tag></tag>"));
    }

    @Test
    public void testDeserialize_Joined() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<joined roomId=\"test\"/>");

        assertEquals(1, packets.size());
        assertInstanceOf(JoinedRoomResponse.class, packets.get(0));

        final JoinedRoomResponse packet = (JoinedRoomResponse) packets.get(0);

        assertEquals("test", packet.getRoomId());
    }

    @Test
    public void testDeserialize_Left() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<left roomId=\"test\"/>");

        assertEquals(1, packets.size());
        assertInstanceOf(LeftPacket.class, packets.get(0));

        final LeftPacket packet = (LeftPacket) packets.get(0);

        assertEquals("test", packet.getRoomId());
    }

    @Test
    public void testDeserialize_Room() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<room roomId=\"test\"></room>");

        assertEquals(1, packets.size());
        assertInstanceOf(RoomPacket.class, packets.get(0));

        final RoomPacket packet = (RoomPacket) packets.get(0);

        assertEquals("test", packet.getRoomId());
    }

    @Test
    public void testDeserialize_Room_Error() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<room roomId=\"test\"><data class=\"error\" message=\"test\"/></room>");

        assertEquals(1, packets.size());
        assertInstanceOf(RoomPacket.class, packets.get(0));

        final RoomPacket packet = (RoomPacket) packets.get(0);

        assertEquals("test", packet.getRoomId());
        assertInstanceOf(ErrorMessage.class, packet.getData());

        final ErrorMessage message = (ErrorMessage) packet.getData();

        assertEquals("test", message.getMessage());
    }

    @Test
    public void testDeserialize_Room_Welcome() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<room roomId=\"test\"><data class=\"welcomeMessage\" color=\"ONE\"/></room>");

        assertEquals(1, packets.size());
        assertInstanceOf(RoomPacket.class, packets.get(0));

        final RoomPacket packet = (RoomPacket) packets.get(0);

        assertEquals("test", packet.getRoomId());
        assertInstanceOf(WelcomeMessage.class, packet.getData());

        final WelcomeMessage message = (WelcomeMessage) packet.getData();

        assertEquals(Team.ONE, message.getTeam());
    }

    @Test
    public void testDeserialize_Room_Memento() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<room roomId=\"test\"><data class=\"memento\"><state class=\"state\" startTeam=\"ONE\" currentTeam=\"ONE\" turn=\"6\"><board nextDirection=\"DOWN_LEFT\"><segment direction=\"RIGHT\"><center q=\"2\" r=\"0\" s=\"-2\"/><field-array><island/><water/><water/><water/><water/></field-array><field-array><water/><island/><water/><water/><water/></field-array><field-array><water/><water/><island/><water/><water/></field-array><field-array><water/><water/><water/><island/><water/></field-array></segment><segment direction=\"DOWN_RIGHT\"><center q=\"2\" r=\"4\" s=\"-6\"/><field-array><water/><water/><water/><passenger direction=\"UP_LEFT\" passenger=\"1\"/><water/></field-array><field-array><water/><water/><passenger direction=\"UP_RIGHT\" passenger=\"1\"/><water/><water/></field-array><field-array><water/><passenger direction=\"RIGHT\" passenger=\"1\"/><water/><water/><water/></field-array><field-array><passenger direction=\"DOWN_RIGHT\" passenger=\"1\"/><water/><water/><water/><goal/></field-array></segment></board><ship team=\"ONE\" direction=\"LEFT\" speed=\"1\" coal=\"5\" passengers=\"2\" freeTurns=\"2\" points=\"16\"><position q=\"5\" r=\"0\" s=\"-5\"/></ship><ship team=\"TWO\" direction=\"RIGHT\" speed=\"2\" coal=\"6\" passengers=\"1\" freeTurns=\"1\" points=\"10\"><position q=\"4\" r=\"0\" s=\"-4\"/></ship><lastMove><actions><acceleration acc=\"1\"/><advance distance=\"2\"/><push direction=\"RIGHT\"/><turn direction=\"RIGHT\"/></actions></lastMove></state></data></room>");

        assertEquals(1, packets.size());
        assertInstanceOf(RoomPacket.class, packets.get(0));

        final RoomPacket packet = (RoomPacket) packets.get(0);

        assertEquals("test", packet.getRoomId());
        assertInstanceOf(MementoMessage.class, packet.getData());

        final MementoMessage message = (MementoMessage) packet.getData();
        final State state = message.getState();

        assertEquals("state", state.getClassName());
        assertEquals(Team.ONE, state.getStartTeam());
        assertEquals(Team.ONE, state.getCurrentTeam());
        assertEquals(6, state.getTurn());

        final BoardData board = state.getBoard();

        assertEquals(ExampleGameState.getSampleSegments(), board.getSegments());
        assertEquals(Direction.DOWN_LEFT, board.getNextDirection());

        final List<ShipData> ships = state.getShips();
        final ShipData[] expectedShipDataArray = {
                getShip(Team.ONE, new Vector3(5, 0, -5), Direction.LEFT, 1, 5, 2, 2, 16),
                getShip(Team.TWO, new Vector3(4, 0, -4), Direction.RIGHT, 2, 6, 1, 1, 10)
        };

        assertArrayEquals(expectedShipDataArray, ships.toArray(new ShipData[0]));

        final Move lastMove = state.getLastMove();
        final Action[] expectedActions = {
                ActionFactory.changeVelocity(1),
                ActionFactory.forward(2),
                ActionFactory.push(Direction.RIGHT),
                ActionFactory.turn(Direction.RIGHT)
        };

        assertArrayEquals(expectedActions, lastMove.actions().toArray(new Action[0]));
    }

    @Test
    public void testDeserialize_Room_MoveRequest() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<room roomId=\"test\"><data class=\"moveRequest\"/></room>");

        assertEquals(1, packets.size());
        assertInstanceOf(RoomPacket.class, packets.get(0));

        final RoomPacket packet = (RoomPacket) packets.get(0);

        assertEquals("test", packet.getRoomId());
        assertInstanceOf(MoveRequestMessage.class, packet.getData());
    }

    @Test
    public void testDeserialize_Room_Result() {
        final LinkedList<XMLProtocolPacket> packets = PacketSerializationUtil.deserialize("<room roomId=\"test\"><data class=\"result\"><winner team=\"ONE\"/><definition><fragment name=\"Siegpunkte\"><aggregation>SUM</aggregation><relevantForRanking>true</relevantForRanking></fragment><fragment name=\"Punkte\"><aggregation>AVERAGE</aggregation><relevantForRanking>true</relevantForRanking></fragment><fragment name=\"Kohle\"><aggregation>AVERAGE</aggregation><relevantForRanking>true</relevantForRanking></fragment><fragment name=\"Gewonnen\"><aggregation>AVERAGE</aggregation><relevantForRanking>true</relevantForRanking></fragment></definition><scores><entry><player name=\"Spieler 1\" team=\"ONE\"/><score cause=\"REGULAR\" reason=\"test\"><part>1</part><part>2</part><part>3</part><part>4</part></score></entry><entry><player name=\"Spieler 2\" team=\"TWO\"/><score cause=\"REGULAR\" reason=\"test2\"><part>5</part><part>6</part><part>7</part><part>8</part></score></entry></scores></data></room>");

        assertEquals(1, packets.size());
        assertInstanceOf(RoomPacket.class, packets.get(0));

        final RoomPacket packet = (RoomPacket) packets.get(0);

        assertEquals("test", packet.getRoomId());
        assertInstanceOf(ResultMessage.class, packet.getData());

        final ResultMessage message = (ResultMessage) packet.getData();

        assertNotNull(message.getWinner());
        assertEquals(Team.ONE, message.getWinner().getTeam());

        final Definition expectedDefinition = new Definition();
        expectedDefinition.setFragments(Arrays.asList(
                new ScoreFragment("Siegpunkte", ScoreAggregation.SUM, true),
                new ScoreFragment("Punkte", ScoreAggregation.AVERAGE, true),
                new ScoreFragment("Kohle", ScoreAggregation.AVERAGE, true),
                new ScoreFragment("Gewonnen", ScoreAggregation.AVERAGE, true)
        ));

        assertEquals(expectedDefinition, message.getDefinition());

        final Scores expectedScores = new Scores();
        expectedScores.setScores(Arrays.asList(
                getScoreEntry("Spieler 1", Team.ONE, "test", new int[] { 1, 2, 3, 4 }),
                getScoreEntry("Spieler 2", Team.TWO, "test2", new int[] { 5, 6, 7, 8 })
        ));

        assertEquals(expectedScores, message.getScores());
    }

    @Test
    public void testDeserialize_Move_Null() {
        assertThrows(DeserializationException.class, () -> PacketSerializationUtil.deserialize("<room roomId=\"test\"><data class=\"memento\"><state class=\"state\"><lastMove><actions><null/></actions></lastMove></state></data></room>"));
    }

    @Test
    public void testSerialize_Move() {
        final String xml = PacketSerializationUtil.serialize(new MovePacket("test", new Move(Arrays.asList(
                ActionFactory.changeVelocity(1),
                ActionFactory.forward(2),
                ActionFactory.push(Direction.RIGHT),
                ActionFactory.turn(Direction.RIGHT)
        ))));

        assertEquals("<room roomId=\"test\"><data class=\"move\"><actions><acceleration acc=\"1\"/><advance distance=\"2\"/><push direction=\"RIGHT\"/><turn direction=\"RIGHT\"/></actions></data></room>", xml);
    }

    @Test
    public void testSerialize_Move_Null() {
        assertThrows(SerializationException.class, () -> PacketSerializationUtil.serialize(
                new MovePacket("test", new Move(Collections.singletonList(null)))
        ));
    }

    @Test
    public void testSerialize_JoinGame() {
        assertEquals("<join/>", PacketSerializationUtil.serialize(new JoinGameRequest(null)));
    }

    @Test
    public void testSerialize_JoinGame_GameType() {
        assertEquals("<join gameType=\"test\"/>", PacketSerializationUtil.serialize(new JoinGameRequest("test")));
    }

    @Test
    public void testSerialize_JoinPreparedRoom() {
        final String xml = PacketSerializationUtil.serialize(new JoinPreparedRoomRequest("test"));

        assertEquals("<joinPrepared reservationCode=\"test\"/>", xml);
    }

    @Test
    public void testSerialize_JoinRoom() {
        final String xml = PacketSerializationUtil.serialize(new JoinRoomRequest("test"));

        assertEquals("<joinRoom roomId=\"test\"/>", xml);
    }

    private static ScoreEntry getScoreEntry(String playerName, Team team, String reason, int[] parts) {
        return new ScoreEntry(
                new ScorePlayer(playerName, team),
                new ScoreData(ScoreCause.REGULAR, reason, parts)
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
