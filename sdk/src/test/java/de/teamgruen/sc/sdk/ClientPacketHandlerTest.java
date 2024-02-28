package de.teamgruen.sc.sdk;

import de.teamgruen.sc.sdk.game.ExampleGameState;
import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Ship;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.data.Direction;
import de.teamgruen.sc.sdk.protocol.data.ShipData;
import de.teamgruen.sc.sdk.protocol.data.State;
import de.teamgruen.sc.sdk.protocol.data.Team;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.actions.ActionFactory;
import de.teamgruen.sc.sdk.protocol.data.board.BoardData;
import de.teamgruen.sc.sdk.protocol.data.board.FieldArray;
import de.teamgruen.sc.sdk.protocol.data.board.SegmentData;
import de.teamgruen.sc.sdk.protocol.data.board.fields.Field;
import de.teamgruen.sc.sdk.protocol.data.scores.*;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.LeftPacket;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;
import de.teamgruen.sc.sdk.protocol.room.messages.MementoMessage;
import de.teamgruen.sc.sdk.protocol.room.messages.MoveRequestMessage;
import de.teamgruen.sc.sdk.protocol.room.messages.ResultMessage;
import de.teamgruen.sc.sdk.protocol.room.messages.WelcomeMessage;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ClientPacketHandlerTest {

    @Test
    public void testHandlePacket_JoinedRoom() {
        final AtomicBoolean called = new AtomicBoolean(false);

        new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
                assertEquals("test", roomId);

                called.set(true);
            }
        }).handlePacket(new JoinedRoomResponse("test"));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_NoRoomId() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
                called.set(true);
            }

            @Override
            public List<Action> getNextActions(GameState gameState) {
                return Collections.emptyList();
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new MoveRequestMessage()));

        assertFalse(called.get());
    }

    @Test
    public void testHandlePacket_Room_Welcome() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
                assertEquals(Team.ONE, gameState.getPlayerTeam());

                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_MementoMessage() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final List<SegmentData> sampleSegments = ExampleGameState.getSampleSegments();
        final List<ShipData> sampleShips = ExampleGameState.getSampleShips();

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onBoardUpdate(GameState gameState) {
                final Collection<Field> expectedFieldList = sampleSegments
                        .stream()
                        .map(boardSegment -> boardSegment.getColumns().stream().map(FieldArray::getFields).reduce(new ArrayList<>(), (result, current) -> {
                            result.addAll(current);
                            return result;
                        }))
                        .reduce(new ArrayList<>(), (result, current) -> {
                            result.addAll(current);
                            return result;
                        });
                final Collection<Field> actualFieldList = gameState.getBoard()
                        .getSegments()
                        .stream()
                        .map(boardSegment -> boardSegment.fields().values())
                        .reduce(new ArrayList<>(), (result, current) -> {
                            result.addAll(current);
                            return result;
                        });

                assertEquals(Direction.DOWN_LEFT, gameState.getBoard().getNextSegmentDirection());
                assertEquals(expectedFieldList, actualFieldList);

                assertEquals(sampleShips.stream().map(shipData -> {
                    final Ship ship = new Ship(shipData.getTeam());
                    ship.setDirection(shipData.getDirection());
                    ship.setPosition(shipData.getPosition().toVector3());
                    ship.setSpeed(shipData.getSpeed());
                    ship.setCoal(shipData.getCoal());
                    ship.setPassengers(shipData.getPassengers());
                    ship.setFreeTurns(shipData.getFreeTurns());
                    ship.setPoints(shipData.getPoints());

                    return ship;
                }).toList(), gameState.getShips());
                assertEquals(123, gameState.getTurn());
                assertEquals(Team.ONE, gameState.getCurrentTeam());

                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(new RoomPacket(
                "test",
                new MementoMessage(new State(
                        "state",
                        Team.ONE,
                        Team.ONE,
                        123,
                        new BoardData(Direction.DOWN_LEFT, sampleSegments),
                        sampleShips,
                        null
                ))
        ));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_MoveRequest() {
        final AtomicBoolean requestedAction = new AtomicBoolean(false),
                sentPacket = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(new SoftwareChallengeClient(null, 0, null) {
            @Override
            public void sendPacket(XMLProtocolPacket packet) {
                assertInstanceOf(MovePacket.class, packet);

                sentPacket.set(true);
            }
        }, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public List<Action> getNextActions(GameState gameState) {
                return requestedAction.getAndSet(true) ? List.of(ActionFactory.forward(1)) : Collections.emptyList();
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));

        for (int i = 0; i < 2; i++)
            handler.handlePacket(new RoomPacket("test", new MoveRequestMessage()));

        assertTrue(requestedAction.get() && sentPacket.get());
    }

    @Test
    public void testHandlePacket_Room_Result() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onGameEnd(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result) {
                final List<ScoreFragment> expectedFragments = getSampleFragments();
                final LinkedHashMap<ScoreFragment, Integer> expectedScoresMap = new LinkedHashMap<>();
                expectedScoresMap.put(expectedFragments.get(0), 1);
                expectedScoresMap.put(expectedFragments.get(1), 2);
                expectedScoresMap.put(expectedFragments.get(2), 3);
                expectedScoresMap.put(expectedFragments.get(3), 4);

                assertEquals(expectedScoresMap, scores);
                assertEquals(GameResult.WIN, result);

                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.REGULAR, Team.ONE));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_Result_Left() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onError(String message) {
                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.LEFT, Team.ONE));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_Result_SoftTimeout() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onError(String message) {
                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.SOFT_TIMEOUT, Team.ONE));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_Result_HardTimeout() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onError(String message) {
                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.HARD_TIMEOUT, Team.ONE));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_Result_RuleViolation() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onError(String message) {
                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.RULE_VIOLATION, Team.ONE));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_Result_Unknown() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onError(String message) {
                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.UNKNOWN, Team.ONE));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_Result_Loose() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onGameEnd(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result) {
                assertEquals(GameResult.LOOSE, result);

                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.REGULAR, Team.TWO));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Room_Result_Draw() {
        final AtomicBoolean called = new AtomicBoolean(false);

        final ClientPacketHandler handler = new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
            }

            @Override
            public void onGameStart(GameState gameState) {
            }

            @Override
            public void onGameEnd(LinkedHashMap<ScoreFragment, Integer> scores, GameResult result) {
                assertEquals(GameResult.DRAW, result);

                called.set(true);
            }
        });
        handler.handlePacket(new JoinedRoomResponse("test"));
        handler.handlePacket(new RoomPacket("test", new WelcomeMessage(Team.ONE)));
        handler.handlePacket(getResultPacket(ScoreCause.REGULAR, null));

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Left() {
        final AtomicBoolean called = new AtomicBoolean(false);

        new ClientPacketHandler(new SoftwareChallengeClient("", 0, null) {
            @Override
            public void stop() {
                called.set(true);
            }
        }, null).handlePacket(new LeftPacket());

        assertTrue(called.get());
    }

    @Test
    public void testHandlePacket_Unknown() {
        final AtomicBoolean called = new AtomicBoolean(false);

        new ClientPacketHandler(null, new GameHandler() {
            @Override
            public void onError(String message) {
                called.set(true);
            }
        }).handlePacket(new XMLProtocolPacket() {});

        assertTrue(called.get());
    }

    private static RoomPacket getResultPacket(ScoreCause cause, Team winner) {
        final List<ScoreFragment> expectedFragments = getSampleFragments();
        final List<ScoreEntry> expectedScoreEntries = List.of(new ScoreEntry(
                new ScorePlayer("Spieler 1", Team.ONE),
                new ScoreData(cause, "test", new int[] { 1, 2, 3, 4 })
        ));

        return new RoomPacket("test", new ResultMessage(
                new Definition(expectedFragments),
                new Scores(expectedScoreEntries),
                winner == null ? null : new Winner(winner)
        ));
    }

    private static List<ScoreFragment> getSampleFragments() {
        return Arrays.asList(
                new ScoreFragment("Siegpunkte", ScoreAggregation.SUM, true),
                new ScoreFragment("Punkte", ScoreAggregation.AVERAGE, true),
                new ScoreFragment("Kohle", ScoreAggregation.AVERAGE, true),
                new ScoreFragment("Gewonnen", ScoreAggregation.AVERAGE, true)
        );
    }

}
