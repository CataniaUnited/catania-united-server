package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    private static final Logger logger = Logger.getLogger(GameWebSocket.class);

    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    @Inject
    GameService gameService;

    @OnOpen
    public Uni<MessageDTO> onOpen(WebSocketConnection connection) {
        logger.infof("Client connected: %s", connection.id());
        Player player = playerService.addPlayer(connection);
        ObjectNode message = JsonNodeFactory.instance.objectNode().put("playerId", player.getUniqueId());
        return Uni.createFrom().item(new MessageDTO(MessageType.CONNECTION_SUCCESSFUL, message));
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        logger.infof("Client closed connection: %s", connection.id());
        playerService.removePlayerByConnectionId(connection);
        ObjectNode message = JsonNodeFactory.instance.objectNode().put("playerId", connection.id());
        connection.broadcast().sendTextAndAwait(new MessageDTO(MessageType.CLIENT_DISCONNECTED, message));
    }

    @OnTextMessage
    public Uni<MessageDTO> onTextMessage(MessageDTO message, WebSocketConnection connection) {
        try {
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
                        GAME_BOARD_JSON, GAME_WON, DICE_RESULT, PLAYER_RESOURCES -> throw new GameException("Invalid client command");
            };
        } catch (GameException ge) {
            logger.errorf("Unexpected Error occurred: message = %s, error = %s", message, ge.getMessage());
            return Uni.createFrom().item(createErrorMessage(ge.getMessage()));
        }
    }

    @OnError
    public Uni<MessageDTO> onError(WebSocketConnection connection, Throwable error) {
        logger.errorf("Unexpected Error occurred: connection = %s, error = %s", connection.id(), error.getMessage());
        return Uni.createFrom().item(createErrorMessage("Unexpected error"));
    }

    Uni<MessageDTO> placeRoad(MessageDTO message, WebSocketConnection connection) throws GameException {
        JsonNode roadId = message.getMessageNode("roadId");
        try {
            int position = Integer.parseInt(roadId.toString());
            gameService.placeRoad(message.getLobbyId(), message.getPlayer(), position);
        } catch (NumberFormatException e) {
            throw new GameException("Invalid road id: id = %s", roadId.toString());
        }
        GameBoard updatedGameboard = gameService.getGameboardByLobbyId(message.getLobbyId());
        MessageDTO update = new MessageDTO(MessageType.PLACE_ROAD, message.getPlayer(), message.getLobbyId(), updatedGameboard.getJson());
        return connection.broadcast().sendText(update).chain(i -> Uni.createFrom().item(update));
    }

    Uni<MessageDTO> placeSettlement(MessageDTO message, WebSocketConnection connection) throws GameException {
        JsonNode settlementPosition = message.getMessageNode("settlementPositionId");
        try {
            int position = Integer.parseInt(settlementPosition.toString());
            gameService.placeSettlement(message.getLobbyId(), message.getPlayer(), position);

        } catch (NumberFormatException e) {
            throw new GameException("Invalid settlement position id: id = %s", settlementPosition.toString());
        }

        if (playerService.checkForWin(message.getPlayer())) {
            return gameService.broadcastWin(connection, message.getLobbyId(), message.getPlayer());
        }

        GameBoard updatedGameboard = gameService.getGameboardByLobbyId(message.getLobbyId());
        MessageDTO update = new MessageDTO(MessageType.PLACE_SETTLEMENT, message.getPlayer(), message.getLobbyId(), updatedGameboard.getJson());

        return connection.broadcast().sendText(update).chain(i -> Uni.createFrom().item(update));
    }

    Uni<MessageDTO> joinLobby(MessageDTO message, WebSocketConnection connection) throws GameException {
        boolean joined = lobbyService.joinLobbyByCode(message.getLobbyId(), message.getPlayer());
        PlayerColor color = lobbyService.getPlayerColor(message.getLobbyId(), message.getPlayer());
        if (joined) {
            ObjectNode colorNode = JsonNodeFactory.instance.objectNode();
            colorNode.put("color", color.getHexCode());
            MessageDTO update = new MessageDTO(MessageType.PLAYER_JOINED, message.getPlayer(), message.getLobbyId(), colorNode);
            return connection.broadcast().sendText(update).chain(i -> Uni.createFrom().item(update));
        }
        throw new GameException("No lobby session");
    }

    Uni<MessageDTO> createLobby(MessageDTO message) throws GameException {
        String lobbyId = lobbyService.createLobby(message.getPlayer());
        PlayerColor color = lobbyService.getPlayerColor(lobbyId, message.getPlayer());
        ObjectNode colorNode = JsonNodeFactory.instance.objectNode();
        colorNode.put("color", color.getHexCode());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.LOBBY_CREATED, message.getPlayer(), lobbyId, colorNode));
    }

    Uni<MessageDTO> setUsername(MessageDTO message, WebSocketConnection connection) throws GameException {
        Player player = playerService.getPlayerByConnection(connection);
        if (player != null) {
            player.setUsername(message.getPlayer());
            List<String> allPlayers = playerService.getAllPlayers().stream()
                    .map(Player::getUsername).toList();
            MessageDTO update = new MessageDTO(MessageType.LOBBY_UPDATED, player.getUsername(), null, allPlayers);
            return connection.broadcast().sendText(update).chain(i -> Uni.createFrom().item(update));
        }
        throw new GameException("No player session");
    }

    //TODO: Remove after implementation of player order
    Uni<MessageDTO> setActivePlayer(MessageDTO message) throws GameException {
        lobbyService.getLobbyById(message.getLobbyId()).setActivePlayer(message.getPlayer());
        return Uni.createFrom().item(new MessageDTO(MessageType.SET_ACTIVE_PLAYER, message.getPlayer(), message.getLobbyId()));
    }


    MessageDTO createErrorMessage(String errorMessage) {
        ObjectNode errorNode = JsonNodeFactory.instance.objectNode();
        errorNode.put("error", errorMessage);
        return new MessageDTO(MessageType.ERROR, errorNode);
    }

    Uni<MessageDTO> createGameBoard(MessageDTO message, WebSocketConnection connection) throws GameException {
        GameBoard board = gameService.createGameboard(message.getLobbyId());
        MessageDTO updateJson = new MessageDTO(MessageType.GAME_BOARD_JSON, null, message.getLobbyId(), board.getJson());
        return connection.broadcast().sendText(updateJson).chain(i -> Uni.createFrom().item(updateJson));

    }

    Uni<MessageDTO> handleDiceRoll(MessageDTO message, WebSocketConnection connection) throws GameException {
        // Roll and broadcast dice
        ObjectNode diceResult = gameService.rollDice(message.getLobbyId());
        MessageDTO diceResultMessage = new MessageDTO(
                MessageType.DICE_RESULT,
                message.getPlayer(),
                message.getLobbyId(),
                diceResult
        );
        Uni<MessageDTO> broadcastDiceUni = connection.broadcast().sendText(diceResultMessage).chain(() -> Uni.createFrom().item(diceResultMessage));


        // send updated resources
        Lobby currentLobby = lobbyService.getLobbyById(message.getLobbyId());
        List<Uni<Void>> individualResourceSendUnis = new ArrayList<>();

        for (String playerIdInLobby : currentLobby.getPlayers()) {
            Player player = playerService.getPlayerById(playerIdInLobby);
            if (player == null) {
                logger.warnf("Player object not found for ID %s in lobby %s during resource update.", playerIdInLobby, currentLobby.getLobbyId());
                continue;
            }

            WebSocketConnection playerConnection = playerService.getConnectionByPlayerId(playerIdInLobby);
            if (playerConnection != null && playerConnection.isOpen()) {
                ObjectNode resourcesPayload = player.getResourceJSON();

                MessageDTO resourceMsg = new MessageDTO(
                        MessageType.PLAYER_RESOURCES,
                        playerIdInLobby,
                        message.getLobbyId(),
                        resourcesPayload
                );

                logger.infof("Sending PLAYER_RESOURCES to %s: %s", playerIdInLobby, resourcesPayload.toString());
                individualResourceSendUnis.add(
                        playerConnection.sendText(resourceMsg).onFailure().invoke(e -> logger.errorf("Failed to send PLAYER_RESOURCES to %s: %s", playerIdInLobby, e.getMessage()))
                );
            } else {
                logger.warnf("No open connection for player %s to send PLAYER_RESOURCES.", playerIdInLobby);
            }
        }

        Uni<Void> resourceUpdatesUni = Uni.createFrom().voidItem();
        if (!individualResourceSendUnis.isEmpty()) {
            resourceUpdatesUni = Uni.join().all(individualResourceSendUnis).andCollectFailures().replaceWithVoid();
        }

        Uni<Void> finalresourceUpdatesUni = resourceUpdatesUni;
        return broadcastDiceUni
                .chain(() -> finalresourceUpdatesUni)
                .chain(() -> Uni.createFrom().item(diceResultMessage));
    }
}