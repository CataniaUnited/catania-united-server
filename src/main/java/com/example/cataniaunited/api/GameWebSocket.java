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

/**
 * Single WebSocket endpoint (<code>/game</code>) that dispatches
 * client commands to the service layer.
 */
@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    /* ───────────────────────── dependencies ───────────────────────── */

    @Inject LobbyService  lobbyService;
    @Inject PlayerService playerService;
    @Inject GameService   gameService;

    private static final Logger LOG = Logger.getLogger(GameWebSocket.class);

    /* ───────────────────────── lifecycle ──────────────────────────── */

    @OnOpen
    public Uni<MessageDTO> onOpen(WebSocketConnection conn) {
        Player p = playerService.addPlayer(conn);
        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("playerId", p.getId());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.CONNECTION_SUCCESSFUL, body)
        );
    }

    @OnClose public void onClose(WebSocketConnection c) {
        playerService.removePlayer(c);
        LOG.infov("Client {0} disconnected", c.id());
    }

    @OnError
    public Uni<MessageDTO> onError(WebSocketConnection c, Throwable t) {
        LOG.error("WS error", t);
        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("error", t.getMessage());
        return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, body));
    }

    /* ───────────────────────── dispatcher ─────────────────────────── */

    @OnTextMessage
    public Uni<MessageDTO> onText(MessageDTO m, WebSocketConnection c) {
        try {
            // “arrow” switch works under JDK ≥ 17 (CI uses 21)
            return switch (m.getType()) {
                case CREATE_LOBBY     -> createLobby(m);
                case JOIN_LOBBY       -> joinLobby(m, c);
                case START_GAME       -> handleStartGame(m);
                case PLACE_SETTLEMENT -> placeSettlement(m, c);
                case PLACE_ROAD       -> placeRoad(m, c);
                default -> throw new GameException(
                        "Client cmd not allowed: " + m.getType());
            };
        } catch (GameException ge) {
            LOG.errorf("Protocol error ({0})", ge.getMessage());
            ObjectNode body = JsonNodeFactory.instance.objectNode()
                    .put("error", ge.getMessage());
            return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, body));
        }
    }

    /* ───────────────────────── handlers ───────────────────────────── */

    private Uni<MessageDTO> createLobby(MessageDTO m) {
        String lobbyId = lobbyService.createLobby(m.getPlayer());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.LOBBY_CREATED, m.getPlayer(), lobbyId)
        );
    }

    private Uni<MessageDTO> joinLobby(MessageDTO m, WebSocketConnection c)
            throws GameException {

        lobbyService.joinLobbyByCode(m.getLobbyId(), m.getPlayer());
        MessageDTO upd = new MessageDTO(
                MessageType.PLAYER_JOINED, m.getPlayer(), m.getLobbyId());
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    private Uni<MessageDTO> placeSettlement(MessageDTO m, WebSocketConnection c)
            throws GameException {

        gameService.placeSettlement(
                m.getLobbyId(), m.getPlayer(),
                m.getMessageNode("settlementPositionId").asInt());

        GameBoard gb = gameService.getGameboardByLobbyId(m.getLobbyId());
        MessageDTO upd = new MessageDTO(
                MessageType.PLACE_SETTLEMENT, m.getPlayer(),
                m.getLobbyId(), gb.getJson());
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    private Uni<MessageDTO> placeRoad(MessageDTO m, WebSocketConnection c)
            throws GameException {

        gameService.placeRoad(
                m.getLobbyId(), m.getPlayer(),
                m.getMessageNode("roadId").asInt());

        GameBoard gb = gameService.getGameboardByLobbyId(m.getLobbyId());
        MessageDTO upd = new MessageDTO(
                MessageType.PLACE_ROAD, m.getPlayer(),
                m.getLobbyId(), gb.getJson());
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    /* ─────────────── “START_GAME” composite handler ──────────────── */

    private Uni<MessageDTO> handleStartGame(MessageDTO m) throws GameException {
        // Board generation + turn order inside service
        MessageDTO startPacket = gameService.startGame(m.getLobbyId());

        // Send to all real players
        lobbyService.notifyPlayers(m.getLobbyId(), startPacket);

        return Uni.createFrom().item(startPacket);
    }
}
