package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Single WebSocket endpoint (<code>/game</code>) that dispatches
 * client commands to the service layer and multicasts updates.
 */
@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    private static final Logger LOG = Logger.getLogger(GameWebSocket.class);

    @Inject LobbyService  lobbyService;
    @Inject PlayerService playerService;
    @Inject GameService   gameService;

    /* ------------------------------------------------------------ */
    /*  Connection lifecycle                                        */
    /* ------------------------------------------------------------ */

    @OnOpen
    public Uni<MessageDTO> onOpen(WebSocketConnection conn) {
        Player p = playerService.addPlayer(conn);

        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("playerId", p.getId());

        return Uni.createFrom().item(
                new MessageDTO(MessageType.CONNECTION_SUCCESSFUL, body)
        );
    }

    @OnClose
    public void onClose(WebSocketConnection conn) {
        playerService.removePlayer(conn);

        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("playerId", conn.id());
        conn.broadcast().sendTextAndAwait(
                new MessageDTO(MessageType.CLIENT_DISCONNECTED, body)
        );
    }

    @OnError
    public Uni<MessageDTO> onError(WebSocketConnection conn, Throwable err) {
        LOG.error("WS error", err);
        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("error", err.getMessage());
        return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, body));
    }

    /* ------------------------------------------------------------ */
    /*  Main dispatcher                                             */
    /* ------------------------------------------------------------ */

    @OnTextMessage
    public Uni<MessageDTO> onText(MessageDTO m, WebSocketConnection c) {
        try {
            LOG.infof("Received message: client=%s, type=%s",
                    c.id(), m.getType());

            return switch (m.getType()) {
                case CREATE_LOBBY     -> createLobby(m);
                case JOIN_LOBBY       -> joinLobby(m, c);
                case SET_USERNAME     -> setUsername(m, c);
                case START_GAME       -> handleStartGame(m);
                /* legacy helper – can be removed later */
                case CREATE_GAME_BOARD-> createGameBoard(m, c);

                case PLACE_SETTLEMENT -> placeSettlement(m, c);
                case PLACE_ROAD       -> placeRoad(m, c);
                case ROLL_DICE        -> handleDiceRoll(m, c);

                /* anything else from the client is invalid */
                default               -> throw new GameException(
                        "Client cmd not allowed: " + m.getType());
            };
        } catch (GameException ge) {
            LOG.errorf("Protocol error (%s)", ge.getMessage());
            ObjectNode body = JsonNodeFactory.instance.objectNode()
                    .put("error", ge.getMessage());
            return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, body));
        }
    }

    /* ------------------------------------------------------------ */
    /*  Handlers                                                    */
    /* ------------------------------------------------------------ */

    /* ---------- lobby & user ---------- */

    private Uni<MessageDTO> createLobby(MessageDTO m) {
        String lobbyId = lobbyService.createLobby(m.getPlayer());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.LOBBY_CREATED, m.getPlayer(), lobbyId)
        );
    }

    private Uni<MessageDTO> joinLobby(MessageDTO m, WebSocketConnection c) throws GameException {
        lobbyService.joinLobbyByCode(m.getLobbyId(), m.getPlayer());

        MessageDTO upd = new MessageDTO(
                MessageType.PLAYER_JOINED, m.getPlayer(), m.getLobbyId()
        );
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    private Uni<MessageDTO> setUsername(MessageDTO m, WebSocketConnection c) throws GameException {
        Player p = playerService.getPlayerByConnection(c);
        if (p == null) throw new GameException("No player session");

        p.setUsername(m.getPlayer());
        List<String> names = playerService.getAllPlayers().stream()
                .map(Player::getUsername).toList();

        MessageDTO upd = new MessageDTO(
                MessageType.LOBBY_UPDATED, p.getUsername(), null, names
        );
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    /* ---------- game start ---------- */

    private Uni<MessageDTO> handleStartGame(MessageDTO m) throws GameException {
        /* host / min-player checks + board generation happen inside GameService */
        MessageDTO startPacket = gameService.startGame(m.getLobbyId());

        /* multicast to all real players */
        lobbyService.notifyPlayers(m.getLobbyId(), startPacket);

        return Uni.createFrom().item(startPacket);
    }

    private Uni<MessageDTO> createGameBoard(MessageDTO m, WebSocketConnection c) throws GameException {
        GameBoard gb = gameService.createGameboard(m.getLobbyId());
        MessageDTO boardPacket = new MessageDTO(
                MessageType.GAME_BOARD_JSON, null, m.getLobbyId(), gb.getJson()
        );
        return c.broadcast().sendText(boardPacket)
                .chain(__ -> Uni.createFrom().item(boardPacket));
    }

    /* ---------- in-game actions ---------- */

    private Uni<MessageDTO> placeSettlement(MessageDTO m, WebSocketConnection c) throws GameException {
        gameService.placeSettlement(
                m.getLobbyId(), m.getPlayer(),
                m.getMessageNode("settlementPositionId").asInt()
        );

        if (playerService.checkForWin(m.getPlayer())) {
            return gameService.broadcastWin(c, m.getLobbyId(), m.getPlayer());
        }

        GameBoard gb = gameService.getGameboardByLobbyId(m.getLobbyId());
        MessageDTO upd = new MessageDTO(
                MessageType.PLACE_SETTLEMENT, m.getPlayer(), m.getLobbyId(), gb.getJson()
        );
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    private Uni<MessageDTO> placeRoad(MessageDTO m, WebSocketConnection c) throws GameException {
        gameService.placeRoad(
                m.getLobbyId(), m.getPlayer(),
                m.getMessageNode("roadId").asInt()
        );

        GameBoard gb = gameService.getGameboardByLobbyId(m.getLobbyId());
        MessageDTO upd = new MessageDTO(
                MessageType.PLACE_ROAD, m.getPlayer(), m.getLobbyId(), gb.getJson()
        );
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    /* ---------- dice (optional helper) ---------- */

    private Uni<MessageDTO> handleDiceRoll(MessageDTO m, WebSocketConnection c) throws GameException {
        ObjectNode diceJson = gameService.rollDice(m.getLobbyId());
        MessageDTO res = new MessageDTO(
                MessageType.DICE_RESULT, m.getPlayer(), m.getLobbyId(), diceJson
        );
        return c.broadcast().sendText(res)
                .chain(__ -> Uni.createFrom().item(res));
    }
}
