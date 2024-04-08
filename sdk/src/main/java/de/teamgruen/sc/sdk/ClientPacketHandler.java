/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk;

import de.teamgruen.sc.sdk.game.GamePhase;
import de.teamgruen.sc.sdk.game.GameResult;
import de.teamgruen.sc.sdk.game.GameState;
import de.teamgruen.sc.sdk.game.board.Board;
import de.teamgruen.sc.sdk.game.handlers.AdminGameHandler;
import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.admin.AdminXMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.admin.PlayerJoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.admin.PreparedRoomResponse;
import de.teamgruen.sc.sdk.protocol.data.Move;
import de.teamgruen.sc.sdk.protocol.data.State;
import de.teamgruen.sc.sdk.protocol.data.actions.Action;
import de.teamgruen.sc.sdk.protocol.data.board.BoardData;
import de.teamgruen.sc.sdk.protocol.data.scores.ScoreFragment;
import de.teamgruen.sc.sdk.protocol.data.scores.Winner;
import de.teamgruen.sc.sdk.protocol.responses.ErrorPacket;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.LeftPacket;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;
import de.teamgruen.sc.sdk.protocol.room.messages.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class ClientPacketHandler {

    private final SoftwareChallengeClient client;
    private final GameHandler gameHandler;
    private final GameState gameState = new GameState();
    private String roomId;

    public void handlePacket(@NonNull XMLProtocolPacket xmlProtocolPacket) {
        if(xmlProtocolPacket instanceof ErrorPacket packet)
            this.gameHandler.onError(packet.getMessage());
        else if(xmlProtocolPacket instanceof AdminXMLProtocolPacket) {
            if(!(this.gameHandler instanceof AdminGameHandler adminGameHandler))
                throw new IllegalStateException("Admin packet received but game handler is not an admin handler");

            if(xmlProtocolPacket instanceof PreparedRoomResponse packet)
                adminGameHandler.onRoomCreated(packet.getRoomId(), packet.getReservations());
            else if(xmlProtocolPacket instanceof PlayerJoinedRoomResponse packet)
                adminGameHandler.onPlayerJoined(packet.getPlayerCount());
            else
                this.gameHandler.onError("Unhandled admin packet: " + xmlProtocolPacket.getClass().getSimpleName());
        } else if (xmlProtocolPacket instanceof JoinedRoomResponse packet) {
            final String roomId = packet.getRoomId();

            this.roomId = roomId;
            this.gameHandler.onRoomJoin(roomId);
        } else if(xmlProtocolPacket instanceof RoomPacket packet) {
            final String roomId = packet.getRoomId();

            if ((this.roomId != null && !Objects.equals(roomId, this.roomId))
                    && !(this.gameHandler instanceof AdminGameHandler))
                return;

            final RoomMessage data = packet.getData();

            if (data instanceof WelcomeMessage message) {
                this.gameState.setPlayerTeam(message.getTeam());
                this.gameHandler.onGameStart(this.gameState);
            } else if (data instanceof MementoMessage message) {
                final State state = message.getState();
                final BoardData board = state.getBoard();
                final Board stateBoard = this.gameState.getBoard();
                stateBoard.setNextSegmentDirection(board.getNextDirection());
                stateBoard.updateSegments(board.getSegments());

                this.gameState.updateShips(state.getShips());
                this.gameState.setTurn(state.getTurn());
                this.gameState.setCurrentTeam(state.getCurrentTeam());

                this.gameHandler.onBoardUpdate(this.gameState);
            } else if (data instanceof MoveRequestMessage) {
                final List<Action> actions = this.gameHandler.getNextActions(this.gameState);

                this.client.sendPacket(new MovePacket(this.roomId, new Move(actions)));
            } else if (data instanceof ResultMessage message) {
                final List<ScoreFragment> fragments = message.getDefinition().getFragments();
                final Winner winner = message.getWinner();

                this.gameState.setGamePhase((winner != null && !winner.isRegular()) ? GamePhase.ABORTED : GamePhase.COMPLETED);

                if(this.gameState.getGamePhase() == GamePhase.ABORTED
                        && winner != null
                        && winner.getTeam() != this.gameState.getPlayerTeam()) {
                    this.gameHandler.onError(winner.getReason());
                    return;
                }

                message.getScores()
                        .getScores()
                        .stream()
                        .filter(entry -> entry.getPlayer().getTeam() == this.gameState.getPlayerTeam())
                        .forEach(entry -> {
                            final LinkedHashMap<ScoreFragment, Integer> scores = new LinkedHashMap<>();
                            final int[] parts = entry.getScore().getParts();

                            for (int i = 0; i < fragments.size(); i++)
                                scores.put(fragments.get(i), parts[i]);

                            final GameResult result = winner == null || winner.getTeam() == null
                                    ? GameResult.DRAW
                                    : winner.getTeam() == this.gameState.getPlayerTeam()
                                        ? GameResult.WIN
                                        : GameResult.LOOSE;

                            this.gameHandler.onResults(scores, result, winner == null ? null : winner.getReason());
                        });
            }
        } else if(xmlProtocolPacket instanceof LeftPacket) {
            try {
                this.client.stop();
            } catch (IOException ex) {
                this.gameHandler.onError("Failed to stop client: " + ex.getMessage());
            }
        } else
            this.gameHandler.onError("Unhandled packet: " + xmlProtocolPacket.getClass().getSimpleName());
    }

}
