package de.teamgruen.sc.sdk;

import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.game.GamePhase;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.data.Move;
import de.teamgruen.sc.sdk.protocol.data.State;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.board.BoardData;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreCause;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreData;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpCloseException;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.LeftPacket;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;
import de.teamgruen.sc.sdk.protocol.room.messages.*;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class ClientPacketHandler {

    private final SoftwareChallengeClient client;
    private final GameHandler gameHandler;
    private String roomId;
    private GameState gameState;

    public void handlePacket(XMLProtocolPacket xmlProtocolPacket) {
        if (xmlProtocolPacket instanceof JoinedRoomResponse packet) {
            final String roomId = packet.getRoomId();

            this.roomId = roomId;
            this.gameHandler.onRoomJoin(roomId);
        } else if(xmlProtocolPacket instanceof RoomPacket packet) {
            final String roomId = packet.getRoomId();

            if (!Objects.equals(roomId, this.roomId))
                return;

            final RoomMessage data = packet.getData();

            if (data instanceof WelcomeMessage message)
                this.gameState = new GameState(message.getTeam());
            else if (data instanceof MementoMessage message) {
                final State state = message.getState();
                final BoardData board = state.getBoard();
                final Board stateBoard = this.gameState.getBoard();
                stateBoard.setNextSegmentDirection(board.getNextDirection());
                stateBoard.updateSegments(board.getSegments());

                this.gameState.updateShips(state.getShips());
                this.gameState.setTurns(state.getTurn());

                this.gameHandler.onBoardUpdate(this.gameState);
            } else if (data instanceof MoveRequestMessage) {
                final List<Action> actions = this.gameHandler.getNextActions(this.gameState);

                if (actions.isEmpty())
                    return;

                final Move move = new Move();
                move.setActions(actions);

                final MovePacket movePacket = new MovePacket();
                movePacket.setRoomId(this.roomId);
                movePacket.setMove(move);

                this.client.sendPacket(movePacket);
            } else if (data instanceof ResultMessage message) {
                final List<ScoreFragment> fragments = message.getDefinition().getFragments();

                message.getScores().getScores().forEach(entry -> {
                    if (entry.getPlayer().getTeam() != this.gameState.getPlayerTeam())
                        return;

                    final ScoreData score = entry.getScore();
                    final GamePhase gamePhase = score.getCause().equals(ScoreCause.REGULAR)
                            ? GamePhase.FINISHED
                            : GamePhase.ABORTED;
                    this.gameState.setGamePhase(gamePhase);

                    switch (score.getCause()) {
                        case LEFT:
                            this.gameHandler.onError("Player left the game");
                            return;
                        case SOFT_TIMEOUT:
                            this.gameHandler.onError("Response to move request took too long");
                            return;
                        case HARD_TIMEOUT:
                            this.gameHandler.onError("No response to move request");
                            return;
                        case RULE_VIOLATION:
                            this.gameHandler.onError("Rule violation");
                            return;
                        case UNKNOWN:
                            this.gameHandler.onError("Unknown Error");
                            return;
                    }

                    final Map<ScoreFragment, Integer> scores = new HashMap<>();
                    final int[] parts = score.getParts();

                    for (int i = 0; i < fragments.size(); i++)
                        scores.put(fragments.get(i), parts[i]);

                    this.gameHandler.onGameEnd(scores, message.getWinner() != null ? message.getWinner().getTeam() : null);
                });
            }
        } else if(xmlProtocolPacket instanceof LeftPacket) {
            try {
                this.client.stop();
            } catch (TcpCloseException ex) {
                this.gameHandler.onError("Failed to close connection: " + ex.getMessage());
            }
        } else
            this.gameHandler.onError("Unhandled packet: " + xmlProtocolPacket.getClass().getSimpleName());
    }

}
