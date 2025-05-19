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

import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket endpoint for handling Catan game interactions.
 * This class manages WebSocket connections, receives messages from clients,
 * processes game actions, and broadcasts updates to connected clients.
 */
@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    private static final Logger logger = Logger.getLogger(GameWebSocket.class);
    private static final String COLOR_FIELD = "color";


    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    @Inject
    GameService gameService;

    /**
     * Handles a new WebSocket connection.
     * A new player is created and associated with the connection.
     * A success message with the player's ID is sent back to the client.
     *
     * @param connection The WebSocket connection established by the client.
     * @return A Uni emitting a {@link MessageDTO} indicating successful connection and the new player's ID.
     */
    @OnOpen
    public Uni<MessageDTO> onOpen(WebSocketConnection connection) {
        logger.infof("Client connected: %s", connection.id());
        Player player = playerService.addPlayer(connection);
        ObjectNode message = JsonNodeFactory.instance.objectNode().put("playerId", player.getUniqueId());
        return Uni.createFrom().item(new MessageDTO(MessageType.CONNECTION_SUCCESSFUL, message));
    }

    /**
     * Handles a WebSocket connection closure.
     * The associated player is removed from the game and a disconnection message
     * is broadcast to other clients in the same lobby (if any).
     *
     * @param connection The WebSocket connection that was closed.
     */
    @OnClose
    public void onClose(WebSocketConnection connection) {
        logger.infof("Client closed connection: %s", connection.id());
        playerService.removePlayerByConnectionId(connection);
        ObjectNode message = JsonNodeFactory.instance.objectNode().put("playerId", connection.id());
        connection.broadcast().sendTextAndAwait(new MessageDTO(MessageType.CLIENT_DISCONNECTED, message));
    }

    /**
     * Handles incoming text messages from a WebSocket client.
     * The message is parsed and routed to the appropriate handler based on its type.
     *
     * @param message    The {@link MessageDTO} received from the client.
     * @param connection The WebSocket connection from which the message was received.
     * @return A Uni emitting a {@link MessageDTO} as a response, or null if no direct response is needed for the client.
     * In case of an error, a Uni emitting an error message is returned.
     */
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
                case GET_GAME_BOARD -> getGameBoard(message); // TODO: Remove after regular game start is implemented
                case SET_ACTIVE_PLAYER -> setActivePlayer(message);
                case PLACE_SETTLEMENT -> placeSettlement(message, connection);
                case UPGRADE_SETTLEMENT -> upgradeSettlement(message, connection);
                case PLACE_ROAD -> placeRoad(message, connection);
                case ROLL_DICE -> handleDiceRoll(message, connection);
                case START_GAME -> handleStartGame(message);
                case GAME_STARTED -> null;
                case ERROR, CONNECTION_SUCCESSFUL, CLIENT_DISCONNECTED, LOBBY_CREATED, LOBBY_UPDATED, PLAYER_JOINED,
                     GAME_BOARD_JSON, GAME_WON, DICE_RESULT, PLAYER_RESOURCES ->
                        throw new GameException("Invalid client command");
            };
        } catch (GameException ge) {
            logger.errorf("Unexpected Error occurred: message = %s, error = %s", message, ge.getMessage());
            return Uni.createFrom().item(createErrorMessage(ge.getMessage()));
        }
    }

    /**
     * Handles errors occurring on a WebSocket connection.
     * Logs the error and sends an error message back to the client.
     *
     * @param connection The WebSocket connection where the error occurred.
     * @param error      The throwable representing the error.
     * @return A Uni emitting a {@link MessageDTO} containing an error message.
     */
    @OnError
    public Uni<MessageDTO> onError(WebSocketConnection connection, Throwable error) {
        logger.errorf("Unexpected Error occurred: connection = %s, error = %s", connection.id(), error.getMessage());
        return Uni.createFrom().item(createErrorMessage("Unexpected error"));
    }

    /**
     * Handles a request to place a road on the game board.
     *
     * @param message    The {@link MessageDTO} containing the lobby ID, player ID, and road ID.
     * @param connection The WebSocket connection of the player making the request.
     * @return A Uni emitting a {@link MessageDTO} with the updated game board and player resources,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the road ID is invalid or if the game service encounters an error.
     */
    Uni<MessageDTO> placeRoad(MessageDTO message, WebSocketConnection connection) throws GameException {
        JsonNode roadId = message.getMessageNode("roadId");
        try {
            int position = Integer.parseInt(roadId.toString());
            gameService.placeRoad(message.getLobbyId(), message.getPlayer(), position);
        } catch (NumberFormatException e) {
            throw new GameException("Invalid road id: id = %s", roadId.toString());
        }

        ObjectNode root = createGameBoardWithPlayers(message.getLobbyId());

        MessageDTO update = new MessageDTO(
                MessageType.PLACE_ROAD,
                message.getPlayer(),
                message.getLobbyId(),
                root
        );


        return lobbyService.notifyPlayers(message.getLobbyId(), update)
                .chain(() -> sendPlayerResources(message.getPlayer(), message.getLobbyId()))
                .chain(() -> Uni.createFrom().item(update));
    }

    /**
     * Creates a JSON object representing the game board along with details of all players in the specified lobby.
     * The player details include their username, victory points, and assigned color.
     *
     * @param lobbyId The ID of the lobby for which to retrieve the game board and player data.
     * @return An {@link ObjectNode} containing the "gameboard" (JSON representation of the game board)
     * and a "players" object mapping player IDs to their details.
     * @throws GameException if the lobby or game board cannot be found, or if a player in the lobby cannot be retrieved.
     */
    ObjectNode createGameBoardWithPlayers(String lobbyId) throws GameException {
        GameBoard gameboard = gameService.getGameboardByLobbyId(lobbyId);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("gameboard", gameboard.getJson());

        ObjectNode playersJson = root.putObject("players");
        for (String playerId : lobby.getPlayers()) {
            Player player = playerService.getPlayerById(playerId);
            if (player != null) {
                ObjectNode playerNode = player.toJson();
                PlayerColor color = lobby.getPlayerColor(player.getUniqueId());
                if (color != null) {
                    playerNode.put(COLOR_FIELD, color.getHexCode());
                }
                playersJson.set(player.getUniqueId(), playerNode);
            }
        }

        return root;
    }

    /**
     * Handles a request from a client to place a settlement on the game board.
     * This method uses the {@link #handleSettlementAction} generic handler to process the request.
     *
     * @param message    The {@link MessageDTO} containing the lobby ID, player ID, and settlement position ID.
     * @param connection The WebSocket connection of the player making the request.
     * @return A Uni emitting a {@link MessageDTO} with the updated game state (or win message),
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the settlement position ID is invalid or if the game service encounters an error
     *                       during settlement placement (e.g., rules violation, insufficient resources).
     */
    Uni<MessageDTO> placeSettlement(MessageDTO message, WebSocketConnection connection) throws GameException {
        SettlementAction placeAction = positionId -> gameService.placeSettlement(message.getLobbyId(), message.getPlayer(), positionId);
        return handleSettlementAction(message, connection, placeAction);
    }

    /**
     * Handles a request to upgrade a settlement to a city on the game board.
     *
     * @param message    The {@link MessageDTO} containing the lobby ID, player ID, and settlement position ID.
     * @param connection The WebSocket connection of the player making the request.
     * @return A Uni emitting a {@link MessageDTO} with the updated game state,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the game service encounters an error during settlement upgrade.
     */
    Uni<MessageDTO> upgradeSettlement(MessageDTO message, WebSocketConnection connection) throws GameException {
        SettlementAction upgradeAction = positionId -> gameService.upgradeSettlement(message.getLobbyId(), message.getPlayer(), positionId);
        return handleSettlementAction(message, connection, upgradeAction);
    }

    /**
     * Generic handler for settlement actions (place or upgrade).
     * It parses the settlement position, executes the provided action, checks for a win condition,
     * and then broadcasts the updated game state.
     *
     * @param message    The {@link MessageDTO} containing action details.
     * @param connection The WebSocket connection of the player.
     * @param action     The {@link SettlementAction} to execute (e.g., place or upgrade).
     * @return A Uni emitting a {@link MessageDTO} with the updated game state or a win message.
     * @throws GameException if the settlement position ID is invalid or the action fails.
     */
    Uni<MessageDTO> handleSettlementAction(MessageDTO message, WebSocketConnection connection, SettlementAction action) throws GameException {
        JsonNode settlementPosition = message.getMessageNode("settlementPositionId");
        try {
            int position = Integer.parseInt(settlementPosition.toString());
            action.execute(position);
        } catch (NumberFormatException e) {
            throw new GameException("Invalid settlement position id: id = %s", settlementPosition.toString());
        }

        if (playerService.checkForWin(message.getPlayer())) {
            return gameService.broadcastWin(connection, message.getLobbyId(), message.getPlayer());
        }

        ObjectNode payload = createGameBoardWithPlayers(message.getLobbyId());

        MessageDTO update = new MessageDTO(
                message.getType(),
                message.getPlayer(),
                message.getLobbyId(),
                payload
        );

        return lobbyService.notifyPlayers(message.getLobbyId(), update)
                .chain(() -> sendPlayerResources(message.getPlayer(), message.getLobbyId()))
                .chain(() -> Uni.createFrom().item(update));
    }

    /**
     * Handles a request for a player to join an existing lobby.
     *
     * @param message    The {@link MessageDTO} containing the lobby ID (code) and player ID.
     * @param connection The WebSocket connection of the player attempting to join.
     * @return A Uni emitting a {@link MessageDTO} confirming the player joined and their assigned color,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the lobby is not found or the player cannot join.
     */
    Uni<MessageDTO> joinLobby(MessageDTO message, WebSocketConnection connection) throws GameException {
        boolean joined = lobbyService.joinLobbyByCode(message.getLobbyId(), message.getPlayer());

        if (!joined) {
            throw new GameException("Failed to join lobby: lobby session not found or full");
        }

        PlayerColor color = lobbyService.getPlayerColor(message.getLobbyId(), message.getPlayer());

        ObjectNode colorNode = JsonNodeFactory.instance.objectNode();
        colorNode.put(COLOR_FIELD, color.getHexCode());
        MessageDTO playerJoinedMessage = new MessageDTO(MessageType.PLAYER_JOINED, message.getPlayer(), message.getLobbyId(), colorNode);
        return lobbyService.notifyPlayers(message.getLobbyId(), playerJoinedMessage)
                .chain(() -> sendPlayerResources(message.getPlayer(), message.getLobbyId()))
                .chain(() -> {
                    try {
                        ObjectNode updatedGameState = createGameBoardWithPlayers(message.getLobbyId());
                        MessageDTO boardUpdate = new MessageDTO(MessageType.GAME_BOARD_JSON, null, message.getLobbyId(), updatedGameState);
                        return lobbyService.notifyPlayers(message.getLobbyId(), boardUpdate);
                    } catch (GameException e) {
                        logger.errorf("Failed to generate updated game board: %s", e.getMessage());
                        return Uni.createFrom().item(playerJoinedMessage);  // fallback: still return success
                    }
                });
    }


    /**
     * Handles a request to create a new lobby.
     *
     * @param message The {@link MessageDTO} containing the host player's ID.
     * @return A Uni emitting a {@link MessageDTO} with the new lobby's ID and the host's assigned color.
     * @throws GameException if lobby creation fails.
     */

    Uni<MessageDTO> createLobby(MessageDTO message) throws GameException {
        String lobbyId = lobbyService.createLobby(message.getPlayer());
        PlayerColor color = lobbyService.getPlayerColor(lobbyId, message.getPlayer());
        ObjectNode colorNode = JsonNodeFactory.instance.objectNode();
        colorNode.put(COLOR_FIELD, color.getHexCode());
        return Uni.createFrom().item(
                new MessageDTO(MessageType.LOBBY_CREATED, message.getPlayer(), lobbyId, colorNode));
    }

    /**
     * Handles a request to set or update a player's username.
     *
     * @param message    The {@link MessageDTO} containing the new username. The player ID is inferred from the connection.
     * @param connection The WebSocket connection of the player setting their username.
     * @return A Uni emitting a {@link MessageDTO} confirming the username update, which is also broadcast to other players.
     * @throws GameException if the player session is not found.
     */
    Uni<MessageDTO> setUsername(MessageDTO message, WebSocketConnection connection) throws GameException {
        Player player = playerService.getPlayerByConnection(connection);
        if (player != null) {
            player.setUsername(message.getMessage().get("username").asText());
            List<String> allPlayers = playerService.getAllPlayers().stream()
                    .map(Player::getUsername).toList();
            MessageDTO update = new MessageDTO(MessageType.LOBBY_UPDATED, player.getUsername(), null, allPlayers);
            return lobbyService.notifyPlayers(message.getLobbyId(), update);
        }
        throw new GameException("No player session");
    }

    //TODO: Remove after implementation of player order
    Uni<MessageDTO> setActivePlayer(MessageDTO message) throws GameException {
        lobbyService.getLobbyById(message.getLobbyId()).setActivePlayer(message.getPlayer());
        return sendPlayerResources(message.getPlayer(), message.getLobbyId())
                .chain(() -> Uni.createFrom().item(new MessageDTO(MessageType.SET_ACTIVE_PLAYER, message.getPlayer(), message.getLobbyId())));
    }

    /**
     * Creates a {@link MessageDTO} for sending an error message to a client.
     *
     * @param errorMessage The error message string.
     * @return A {@link MessageDTO} of type ERROR containing the error message.
     */
    MessageDTO createErrorMessage(String errorMessage) {
        ObjectNode errorNode = JsonNodeFactory.instance.objectNode();
        errorNode.put("error", errorMessage);
        return new MessageDTO(MessageType.ERROR, errorNode);
    }

    /**
     * Handles a request to create a new game board for a lobby.
     *
     * @param message    The {@link MessageDTO} containing the lobby ID.
     * @param connection The WebSocket connection of the player initiating the request.
     * @return A Uni emitting a {@link MessageDTO} with the game board JSON,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if game board creation fails.
     */
    Uni<MessageDTO> createGameBoard(MessageDTO message, WebSocketConnection connection) throws GameException {
        GameBoard board = gameService.createGameboard(message.getLobbyId());

        ObjectNode gameData = createGameBoardWithPlayers(message.getLobbyId());
        gameData.setAll(board.getJson());

        MessageDTO updateJson = new MessageDTO(
                MessageType.GAME_BOARD_JSON,
                null,
                message.getLobbyId(),
                gameData
        );

        return lobbyService.notifyPlayers(message.getLobbyId(), updateJson)
                .chain(() -> sendPlayerResources(message.getPlayer(), message.getLobbyId()))
                .replaceWith(Uni.createFrom().item(updateJson));

    }

    /**
     * Handles a request to get the current game board for a lobby.
     *
     * @param message The {@link MessageDTO} containing the lobby ID.
     * @return A Uni emitting a {@link MessageDTO} with the game board JSON, sent only to the requesting client.
     * @throws GameException if the game board cannot be retrieved.
     */
    private Uni<MessageDTO> getGameBoard(MessageDTO message) throws GameException {
        MessageDTO updateJson = new MessageDTO(
                MessageType.GAME_BOARD_JSON,
                null,
                message.getLobbyId(),
                createGameBoardObjectNode(message.getLobbyId())
        );
        return sendPlayerResources(message.getPlayer(), message.getLobbyId())
                .chain(() -> Uni.createFrom().item(updateJson));
    }

    /**
     * Creates a JSON object node containing the game board data for a specified lobby.
     *
     * @param lobbyId The ID of the lobby for which to retrieve the game board.
     * @return An {@link ObjectNode} with a "gameboard" field containing the JSON representation of the game board.
     * @throws GameException if the game board for the lobby cannot be found.
     */
    private ObjectNode createGameBoardObjectNode(String lobbyId) throws GameException {
        GameBoard gameboard = gameService.getGameboardByLobbyId(lobbyId);
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set("gameboard", gameboard.getJson());
        return payload;
    }

    /**
     * Handles a dice roll request from a client.
     * This method processes the dice roll, broadcasts the result to all players in the lobby,
     * and then sends updated resource information individually to each player in that lobby.
     *
     * @param message    The {@link MessageDTO} containing the player ID and lobby ID.
     * @param connection The WebSocket connection of the player who initiated the dice roll.
     *                   This connection is used as the source for broadcasting.
     * @return A Uni emitting the {@link MessageDTO} containing the dice roll result. This DTO is the one
     * that was broadcast. The primary purpose of the returned Uni is to chain asynchronous operations.
     * @throws GameException if an error occurs during dice rolling or retrieving lobby/player information.
     */
    Uni<MessageDTO> handleDiceRoll(MessageDTO message, WebSocketConnection connection) throws GameException {
        // Roll and broadcast dice
        ObjectNode diceResult = gameService.rollDice(message.getLobbyId());
        MessageDTO diceResultMessage = new MessageDTO(
                MessageType.DICE_RESULT,
                message.getPlayer(),
                message.getLobbyId(),
                diceResult
        );

        // send updated resources
        Lobby currentLobby = lobbyService.getLobbyById(message.getLobbyId());
        List<Uni<Void>> individualResourceSendUnis = currentLobby.getPlayers()
                .stream().map(pid -> sendPlayerResources(pid, message.getLobbyId())).collect(Collectors.toList());

        Uni<Void> resourceUpdatesUni = Uni.join().all(individualResourceSendUnis).andCollectFailures().replaceWithVoid();
        return lobbyService.notifyPlayers(message.getLobbyId(), diceResultMessage)
                .chain(() -> resourceUpdatesUni)
                .chain(() -> Uni.createFrom().item(diceResultMessage));
    }

    /**
     * Handles a request to start the game in a lobby.
     * Initializes the game, sets player order, creates the game board, and notifies all players.
     *
     * @param message The {@link MessageDTO} containing the lobby ID.
     * @return A Uni emitting the {@link MessageDTO} confirming the game start.
     * @throws GameException if the game cannot be started (e.g., not enough players, game already started).
     */
    private Uni<MessageDTO> handleStartGame(MessageDTO message) throws GameException {
        MessageDTO startPkt = gameService.startGame(message.getLobbyId());

        GameBoard board = gameService.getGameboardByLobbyId(message.getLobbyId());
        MessageDTO boardPkt = new MessageDTO(MessageType.GAME_BOARD_JSON,
                null, message.getLobbyId(), board.getJson());
        lobbyService.notifyPlayers(message.getLobbyId(), boardPkt);
        return lobbyService.notifyPlayers(message.getLobbyId(), startPkt)
                .chain(() -> lobbyService.notifyPlayers(message.getLobbyId(), boardPkt))
                .onFailure(GameException.class)
                .recoverWithItem(boardPkt);
    }

    /**
     * Sends a message containing the player's current resources to their WebSocket connection.
     *
     * @param playerId The id of the {@link Player} whose resources are to be sent.
     * @param lobbyId  The ID of the lobby the player is in (used for constructing the {@link MessageDTO}).
     * @return A {@link Uni<Void>} that completes when the send operation is initiated, or fails if the send fails.
     * Logs an error on failure to send.
     */
    Uni<Void> sendPlayerResources(String playerId, String lobbyId) {
        Player player = playerService.getPlayerById(playerId);
        if (player == null) {
            logger.warnf("Player not found in lobby for resource update: lobbyId = %s, playerId = %s", lobbyId, playerId);
            return Uni.createFrom().voidItem();
        }
        ObjectNode resourcesPayload = player.getResourceJSON();
        MessageDTO resourceMsg = new MessageDTO(
                MessageType.PLAYER_RESOURCES,
                player.getUniqueId(),
                lobbyId,
                resourcesPayload
        );

        logger.infof("Sending player resources: lobbyId = %s, playerId = %s, resources = %s", lobbyId, playerId, resourcesPayload.toString());
        return player.sendMessage(resourceMsg);
    }

}

@FunctionalInterface
interface SettlementAction {
    void execute(int settlementPositionId) throws GameException;
}