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


@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {


    @Inject LobbyService  lobbyService;
    @Inject PlayerService playerService;
    @Inject GameService   gameService;

    private static final Logger LOG = Logger.getLogger(GameWebSocket.class);


    @OnOpen
    public Uni<MessageDTO> onOpen(WebSocketConnection conn) {
        Player p = playerService.addPlayer(conn);
        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("playerId", p.getId());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.CONNECTION_SUCCESSFUL, body)
        );
    }

    @OnClose public void onClose(WebSocketConnection c) { playerService.removePlayer(c); }

    @OnError
    public Uni<MessageDTO> onError(WebSocketConnection c, Throwable err) {
        LOG.error("WS error", err);
        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("error", err.getMessage());
        return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, body));
    }



    @OnTextMessage
    public Uni<MessageDTO> onText(MessageDTO m, WebSocketConnection c) {
        try {
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
            LOG.errorf("Protocol error (%s)", ge.getMessage());
            ObjectNode body = JsonNodeFactory.instance.objectNode()
                    .put("error", ge.getMessage());
            return Uni.createFrom().item(new MessageDTO(MessageType.ERROR, body));
        }
    }


    private Uni<MessageDTO> createLobby(MessageDTO m) {
        String lobbyId = lobbyService.createLobby(m.getPlayer());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.LOBBY_CREATED, m.getPlayer(), lobbyId)
        );
    }

    private Uni<MessageDTO> joinLobby(MessageDTO m,
                                      WebSocketConnection c) throws GameException {

        lobbyService.joinLobbyByCode(m.getLobbyId(), m.getPlayer());
        MessageDTO upd = new MessageDTO(
                MessageType.PLAYER_JOINED, m.getPlayer(), m.getLobbyId());
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    private Uni<MessageDTO> placeSettlement(MessageDTO m,
                                            WebSocketConnection c) throws GameException {

        gameService.placeSettlement(
                m.getLobbyId(),
                m.getPlayer(),
                m.getMessageNode("settlementPositionId").asInt());

        GameBoard gb = gameService.getGameboardByLobbyId(m.getLobbyId());
        MessageDTO upd = new MessageDTO(
                MessageType.PLACE_SETTLEMENT,
                m.getPlayer(),
                m.getLobbyId(),
                gb.getJson());
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }

    private Uni<MessageDTO> placeRoad(MessageDTO m,
                                      WebSocketConnection c) throws GameException {

        gameService.placeRoad(
                m.getLobbyId(),
                m.getPlayer(),
                m.getMessageNode("roadId").asInt());

        GameBoard gb = gameService.getGameboardByLobbyId(m.getLobbyId());
        MessageDTO upd = new MessageDTO(
                MessageType.PLACE_ROAD,
                m.getPlayer(),
                m.getLobbyId(),
                gb.getJson());
        return c.broadcast().sendText(upd)
                .chain(__ -> Uni.createFrom().item(upd));
    }


    private Uni<MessageDTO> handleStartGame(MessageDTO m) throws GameException {
        MessageDTO startPacket = gameService.startGame(m.getLobbyId());

        lobbyService.notifyPlayers(m.getLobbyId(), startPacket);

        GameBoard board = gameService.getGameboardByLobbyId(m.getLobbyId());
        MessageDTO boardPacket = new MessageDTO(
                MessageType.GAME_BOARD_JSON,
                null,
                m.getLobbyId(),
                board.getJson());

        lobbyService.notifyPlayers(m.getLobbyId(), boardPacket);

        /* Return START_GAME so the caller sees success */
        return Uni.createFrom().item(startPacket);
    }
}
