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
            logger.infof("Received message: client = %s, message = %s", connection.id(), message);
            return switch (message.getType()) {
                case CREATE_LOBBY -> createLobby(message);
                case JOIN_LOBBY -> joinLobby(message, connection);
                case SET_USERNAME -> setUsername(message, connection);
                case CREATE_GAME_BOARD ->
                        createGameBoard(message, connection); // TODO: Remove after regular game start is implemented
                case SET_ACTIVE_PLAYER -> setActivePlayer(message);
                case PLACE_SETTLEMENT -> placeSettlement(message, connection);
                case PLACE_ROAD -> placeRoad(message, connection);
                case ROLL_DICE -> handleDiceRoll(message, connection);
                case ERROR, CONNECTION_SUCCESSFUL, CLIENT_DISCONNECTED, LOBBY_CREATED, LOBBY_UPDATED, PLAYER_JOINED,
                     GAME_BOARD_JSON, GAME_WON, DICE_RESULT -> throw new GameException("Invalid client command");
            };
        } catch (GameException ge) {
            logger.errorf("Unexpected Error occurred: message = %s, error = %s", message, ge.getMessage());
            return Uni.createFrom().item(createErrorMessage(ge.getMessage()));
        }
    }


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


    Uni<MessageDTO> handleDiceRoll(MessageDTO message, WebSocketConnection connection) throws GameException {
        ObjectNode diceResult = gameService.rollDice(message.getLobbyId());
        MessageDTO resultMessage = new MessageDTO(
                MessageType.DICE_RESULT,
                message.getPlayer(),
                message.getLobbyId(),
                diceResult
        );

        return connection.broadcast()
                .sendText(resultMessage)
                .chain(() -> Uni.createFrom().item(resultMessage));
    }
}

