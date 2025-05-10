package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/** WebSocket endpoint at <code>/game</code>. */
@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    /* ───────────────────────  dependencies  ─────────────────────── */

    @Inject LobbyService  lobbyService;
    @Inject PlayerService playerService;
    @Inject GameService   gameService;

    private static final Logger LOG = Logger.getLogger(GameWebSocket.class);

    /* ───────────────────────  life-cycle  ─────────────────────── */

    @OnOpen
    public Uni<MessageDTO> onOpen(WebSocketConnection conn) {
        Player p   = playerService.addPlayer(conn);
        ObjectNode b = JsonNodeFactory.instance.objectNode()
                .put("playerId", p.getId());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.CONNECTION_SUCCESSFUL, b));
    }

    @OnClose public void onClose(WebSocketConnection c) { playerService.removePlayer(c); }

    @OnError
    public Uni<MessageDTO> onError(WebSocketConnection c, Throwable t) {
        LOG.error("WS error", t);
        ObjectNode b = JsonNodeFactory.instance.objectNode()
                .put("error", t.getMessage());
        return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, b));
    }

    /* ───────────────────────  dispatcher  ─────────────────────── */

    @OnTextMessage
    public Uni<MessageDTO> onText(MessageDTO m, WebSocketConnection c) {
        try {
            return switch (m.getType()) {
                /* lobby management */
                case CREATE_LOBBY     -> createLobby(m);
                case JOIN_LOBBY       -> joinLobby(m, c);

                /* game bootstrap */
                case START_GAME       -> handleStartGame(m);

                /* in-game actions */
                case PLACE_SETTLEMENT -> placeSettlement(m, c);
                case PLACE_ROAD       -> placeRoad(m, c);

                /* everything else from the client is invalid */
                default -> throw new GameException("Client cmd not allowed: " + m.getType());
            };
        } catch (GameException ge) {
            LOG.errorf("Protocol error (%s)", ge.getMessage());
            ObjectNode err = JsonNodeFactory.instance.objectNode()
                    .put("error", ge.getMessage());
            return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, err));
        }
    }

    /* ───────────────────────  handlers  ─────────────────────── */

    private Uni<MessageDTO> createLobby(MessageDTO m) {
        String lobbyId = lobbyService.createLobby(m.getPlayer());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.LOBBY_CREATED, m.getPlayer(), lobbyId));
    }

    private Uni<MessageDTO> joinLobby(MessageDTO m, WebSocketConnection c) throws GameException {
        lobbyService.joinLobbyByCode(m.getLobbyId(), m.getPlayer());
        MessageDTO upd = new MessageDTO(MessageType.PLAYER_JOINED,
                m.getPlayer(), m.getLobbyId());
        return c.broadcast().sendText(upd).chain(__ -> Uni.createFrom().item(upd));
    }

    /* ----- placement ----- */

    private Uni<MessageDTO> placeSettlement(MessageDTO m, WebSocketConnection c) throws GameException {
        gameService.placeSettlement(m.getLobbyId(),
                m.getPlayer(),
                m.getMessageNode("settlementPositionId").asInt());

        GameBoard board = gameService.getBoard(m.getLobbyId());
        MessageDTO upd  = new MessageDTO(MessageType.PLACE_SETTLEMENT,
                m.getPlayer(), m.getLobbyId(), board.getJson());
        return c.broadcast().sendText(upd).chain(__ -> Uni.createFrom().item(upd));
    }

    private Uni<MessageDTO> placeRoad(MessageDTO m, WebSocketConnection c) throws GameException {
        gameService.placeRoad(m.getLobbyId(),
                m.getPlayer(),
                m.getMessageNode("roadId").asInt());

        GameBoard board = gameService.getBoard(m.getLobbyId());
        MessageDTO upd  = new MessageDTO(MessageType.PLACE_ROAD,
                m.getPlayer(), m.getLobbyId(), board.getJson());
        return c.broadcast().sendText(upd).chain(__ -> Uni.createFrom().item(upd));
    }

    /* ───────────────────────  START_GAME flow  ─────────────────────── */

    private Uni<MessageDTO> handleStartGame(MessageDTO m) throws GameException {

        /* 1) let GameService create the board & player order */
        MessageDTO startPkt = gameService.startGame(m.getLobbyId());

        /* 2) broadcast START_GAME */
        lobbyService.notifyPlayers(m.getLobbyId(), startPkt);

        /* 3) broadcast board for legacy client */
        GameBoard board = gameService.getBoard(m.getLobbyId());
        MessageDTO boardPkt = new MessageDTO(MessageType.GAME_BOARD_JSON,
                null, m.getLobbyId(), board.getJson());
        lobbyService.notifyPlayers(m.getLobbyId(), boardPkt);

        return Uni.createFrom().item(startPkt);
    }
}
