/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.serialization;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.admin.AuthenticationRequest;
import de.teamgruen.sc.sdk.protocol.admin.PlayerJoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.admin.PrepareRoomRequest;
import de.teamgruen.sc.sdk.protocol.admin.PreparedRoomResponse;
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
import de.teamgruen.sc.sdk.protocol.responses.ErrorPacket;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.LeftPacket;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;
import de.teamgruen.sc.sdk.protocol.room.messages.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
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
    public void testDeserialize_NullRootTag() {
        assertThrows(IllegalArgumentException.class, () -> PacketSerializationUtil.deserializeXML(null, ""));
    }

    @Test
    public void testDeserialize_Invalid() {
        assertThrows(DeserializationException.class, () -> PacketSerializationUtil.deserializeXML("invalid", "<"));
    }

    @Test
    public void testDeserialize_Unknown() {
        assertThrows(DeserializationException.class, () -> PacketSerializationUtil.deserializeXML("", "<tag></tag>"));
    }

    @Test
    public void testDeserialize_ErrorPacket() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("errorpacket", "<errorpacket message=\"test\" />");

        assertInstanceOf(ErrorPacket.class, packet);
        assertEquals("test", ((ErrorPacket) packet).getMessage());
    }

    @Test
    public void testDeserialize_Joined() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("joined", "<joined roomId=\"test\"/>");

        assertInstanceOf(JoinedRoomResponse.class, packet);
        assertEquals("test", ((JoinedRoomResponse) packet).getRoomId());
    }

    @Test
    public void testDeserialize_Prepared() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("prepared", "<prepared roomId=\"test\"><reservation>test2</reservation><reservation>test3</reservation></prepared>");

        assertInstanceOf(PreparedRoomResponse.class, packet);

        final PreparedRoomResponse response = (PreparedRoomResponse) packet;

        assertEquals("test", response.getRoomId());
        assertEquals(List.of("test2", "test3"), response.getReservations());
    }

    @Test
    public void testDeserialize_PlayerJoinedRoom() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("joinedGameRoom", "<joinedGameRoom roomId=\"test\" playerCount=\"2\"/>");

        assertInstanceOf(PlayerJoinedRoomResponse.class, packet);

        final PlayerJoinedRoomResponse response = (PlayerJoinedRoomResponse) packet;

        assertEquals("test", response.getRoomId());
        assertEquals(2, response.getPlayerCount());
    }

    @Test
    public void testDeserialize_Left() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("left", "<left roomId=\"test\"/>");

        assertInstanceOf(LeftPacket.class, packet);
        assertEquals("test", ((LeftPacket) packet).getRoomId());
    }

    @Test
    public void testDeserialize_Room() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("room", "<room roomId=\"test\"></room>");

        assertInstanceOf(RoomPacket.class, packet);
        assertEquals("test", ((RoomPacket) packet).getRoomId());
    }

    @Test
    public void testDeserialize_Room_Error() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("room", "<room roomId=\"test\"><data class=\"error\" message=\"test\"/></room>");

        assertInstanceOf(RoomPacket.class, packet);

        final RoomPacket roomPacket = (RoomPacket) packet;

        assertEquals("test", roomPacket.getRoomId());
        assertInstanceOf(ErrorMessage.class, roomPacket.getData());

        final ErrorMessage message = (ErrorMessage) roomPacket.getData();

        assertEquals("test", message.getMessage());
    }

    @Test
    public void testDeserialize_Room_Welcome() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("room", "<room roomId=\"test\"><data class=\"welcomeMessage\" color=\"ONE\"/></room>");

        assertInstanceOf(RoomPacket.class, packet);

        final RoomPacket roomPacket = (RoomPacket) packet;

        assertEquals("test", roomPacket.getRoomId());
        assertInstanceOf(WelcomeMessage.class, roomPacket.getData());

        final WelcomeMessage message = (WelcomeMessage) roomPacket.getData();

        assertEquals(Team.ONE, message.getTeam());
    }

    @Test
    public void testDeserialize_Room_Memento() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("room", "<room roomId=\"test\"><data class=\"memento\"><state class=\"state\" startTeam=\"ONE\" currentTeam=\"ONE\" turn=\"6\"><board nextDirection=\"DOWN_LEFT\"><segment direction=\"RIGHT\"><center q=\"0\" r=\"0\" s=\"0\"/><field-array><water/><water/><water/><water/><water/></field-array><field-array><water/><water/><water/><water/><water/></field-array><field-array><water/><island/><water/><water/><water/></field-array><field-array><water/><water/><water/><water/><water/></field-array></segment><segment direction=\"DOWN_RIGHT\"><center q=\"0\" r=\"4\" s=\"-4\"/><field-array><water/><passenger direction=\"RIGHT\" passenger=\"1\"/><water/><water/><water/></field-array><field-array><water/><water/><water/><water/><water/></field-array><field-array><water/><water/><water/><water/><island/></field-array><field-array><water/><water/><water/><water/><water/></field-array></segment><segment direction=\"DOWN_LEFT\"><center q=\"-4\" r=\"8\" s=\"-4\"/><field-array><passenger direction=\"UP_LEFT\" passenger=\"1\"/><water/><water/><island/><water/></field-array><field-array><water/><water/><water/><water/><island/></field-array><field-array><water/><water/><water/><water/><water/></field-array><field-array><water/><goal/><goal/><goal/><water/></field-array></segment></board><ship team=\"ONE\" direction=\"LEFT\" speed=\"1\" coal=\"5\" passengers=\"2\" freeTurns=\"2\" points=\"16\" stuck=\"false\"><position q=\"2\" r=\"0\" s=\"-2\"/></ship><ship team=\"TWO\" direction=\"RIGHT\" speed=\"2\" coal=\"6\" passengers=\"1\" freeTurns=\"1\" points=\"10\" stuck=\"false\"><position q=\"1\" r=\"0\" s=\"-1\"/></ship><lastMove><actions><acceleration acc=\"1\"/><advance distance=\"2\"/><push direction=\"RIGHT\"/><turn direction=\"RIGHT\"/></actions></lastMove></state></data></room>");

        assertInstanceOf(RoomPacket.class, packet);

        final RoomPacket roomPacket = (RoomPacket) packet;

        assertEquals("test", roomPacket.getRoomId());
        assertInstanceOf(MementoMessage.class, roomPacket.getData());

        final MementoMessage message = (MementoMessage) roomPacket.getData();
        final State state = message.getState();

        assertEquals("state", state.getClassName());
        assertEquals(Team.ONE, state.getStartTeam());
        assertEquals(Team.ONE, state.getCurrentTeam());
        assertEquals(6, state.getTurn());
        assertEquals(ExampleGameState.getSampleShips(), state.getShips());

        final BoardData board = state.getBoard();

        assertEquals(ExampleGameState.getSampleSegments(), board.getSegments());
        assertEquals(Direction.DOWN_LEFT, board.getNextDirection());

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
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("room", "<room roomId=\"test\"><data class=\"moveRequest\"/></room>");

        assertInstanceOf(RoomPacket.class, packet);

        final RoomPacket roomPacket = (RoomPacket) packet;

        assertEquals("test", roomPacket.getRoomId());
        assertInstanceOf(MoveRequestMessage.class, roomPacket.getData());
    }

    @Test
    public void testDeserialize_Room_Result() {
        final XMLProtocolPacket packet = PacketSerializationUtil.deserializeXML("room", "<room roomId=\"test\"><data class=\"result\"><winner team=\"ONE\" regular=\"true\" reason=\"test\"/><definition><fragment name=\"Siegpunkte\"><aggregation>SUM</aggregation><relevantForRanking>true</relevantForRanking></fragment><fragment name=\"Punkte\"><aggregation>AVERAGE</aggregation><relevantForRanking>true</relevantForRanking></fragment><fragment name=\"Kohle\"><aggregation>AVERAGE</aggregation><relevantForRanking>true</relevantForRanking></fragment><fragment name=\"Gewonnen\"><aggregation>AVERAGE</aggregation><relevantForRanking>true</relevantForRanking></fragment></definition><scores><entry><player name=\"Spieler 1\" team=\"ONE\"/><score><part>1</part><part>2</part><part>3</part><part>4</part></score></entry><entry><player name=\"Spieler 2\" team=\"TWO\"/><score><part>5</part><part>6</part><part>7</part><part>8</part></score></entry></scores></data></room>");

        assertInstanceOf(RoomPacket.class, packet);

        final RoomPacket roomPacket = (RoomPacket) packet;

        assertEquals("test", roomPacket.getRoomId());
        assertInstanceOf(ResultMessage.class, roomPacket.getData());

        final ResultMessage message = (ResultMessage) roomPacket.getData();

        final Winner winner = message.getWinner();
        assertNotNull(winner);
        assertEquals(Team.ONE, winner.getTeam());
        assertTrue(winner.isRegular());
        assertEquals("test", winner.getReason());

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
                getScoreEntry("Spieler 1", Team.ONE, new int[] { 1, 2, 3, 4 }),
                getScoreEntry("Spieler 2", Team.TWO, new int[] { 5, 6, 7, 8 })
        ));

        assertEquals(expectedScores, message.getScores());
    }

    @Test
    public void testDeserialize_Move_Null() {
        assertThrows(DeserializationException.class, () -> PacketSerializationUtil.deserializeXML("room", "<room roomId=\"test\"><data class=\"memento\"><state class=\"state\"><lastMove><actions><null/></actions></lastMove></state></data></room>"));
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

    @Test
    public void testSerialize_Authenticate() {
        assertEquals("<authenticate password=\"test\"/>", PacketSerializationUtil.serialize(new AuthenticationRequest("test")));
    }

    @Test
    public void testSerialize_PrepareRoom() {
        final PrepareRoomRequest packet = new PrepareRoomRequest("test", true, List.of(
                new RoomSlot("test2", true, true),
                new RoomSlot("test3", false, false)
        ));

        assertEquals("<prepare gameType=\"test\" pause=\"true\"><slot displayName=\"test2\" canTimeout=\"true\" reserved=\"true\"/><slot displayName=\"test3\" canTimeout=\"false\" reserved=\"false\"/></prepare>", PacketSerializationUtil.serialize(packet));
    }

    private static ScoreEntry getScoreEntry(String playerName, Team team, int[] parts) {
        return new ScoreEntry(
                new ScorePlayer(playerName, team),
                new ScoreData(parts)
        );
    }

}
