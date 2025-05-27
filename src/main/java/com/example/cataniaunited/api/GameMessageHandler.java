package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.fi.SettlementAction;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class GameMessageHandler {

    private static final Logger logger = Logger.getLogger(GameMessageHandler.class);
    private static final String COLOR_FIELD = "color";

    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    @Inject
    GameService gameService;

    public Uni<MessageDTO> handleInitialConnection(WebSocketConnection connection) {
        Player player = playerService.addPlayer(connection);
        ObjectNode message = JsonNodeFactory.instance.objectNode().put("playerId", player.getUniqueId());
        return Uni.createFrom().item(new MessageDTO(MessageType.CONNECTION_SUCCESSFUL, message));
    }

    public void handleDisconnect(WebSocketConnection connection) {
        playerService.removePlayerByConnectionId(connection);
        ObjectNode message = JsonNodeFactory.instance.objectNode().put("playerId", connection.id());
        connection.broadcast().sendTextAndAwait(new MessageDTO(MessageType.CLIENT_DISCONNECTED, message));
    }

    public Uni<MessageDTO> handleGameMessage(MessageDTO message) {
        try {
            logger.infof("Handle message: message = %s", message);
            return switch (message.getType()) {
                case CREATE_LOBBY -> createLobby(message);
                case JOIN_LOBBY -> joinLobby(message);
                case SET_USERNAME -> setUsername(message);
                case PLACE_SETTLEMENT -> placeSettlement(message);
                case UPGRADE_SETTLEMENT -> upgradeSettlement(message);
                case PLACE_ROAD -> placeRoad(message);
                case ROLL_DICE -> handleDiceRoll(message);
                case START_GAME -> handleStartGame(message);
                case ERROR, CONNECTION_SUCCESSFUL, CLIENT_DISCONNECTED, LOBBY_CREATED, LOBBY_UPDATED, PLAYER_JOINED,
                     GAME_BOARD_JSON, GAME_WON, DICE_RESULT, PLAYER_RESOURCES, NEXT_TURN, GAME_STARTED ->
                        throw new GameException("Invalid client command");
                case END_TURN -> endTurn(message);
            };
        } catch (GameException ge) {
            logger.errorf("Unexpected Error occurred: message = %s, error = %s", message, ge.getMessage());
            return Uni.createFrom().item(createErrorMessage(ge.getMessage()));
        }
    }

    Uni<MessageDTO> endTurn(MessageDTO message) throws GameException {
        String activePlayerId = lobbyService.nextTurn(message.getLobbyId(), message.getPlayer());
        ObjectNode payload = createGameBoardWithPlayers(message.getLobbyId());
        payload.put("activePlayerId", activePlayerId);
        var response = new MessageDTO(MessageType.NEXT_TURN, payload);
        return lobbyService.notifyPlayers(message.getLobbyId(), response);
    }

    /**
     * Handles a request to place a road on the game board.
     *
     * @param message The {@link MessageDTO} containing the lobby ID, player ID, and road ID.
     * @return A Uni emitting a {@link MessageDTO} with the updated game board and player resources,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the road ID is invalid or if the game service encounters an error.
     */
    Uni<MessageDTO> placeRoad(MessageDTO message) throws GameException {
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
        root.set("players", getLobbyPlayerInformation(lobbyId));
        root.put("activePlayerId", lobby.getActivePlayer());
        return root;
    }

    ObjectNode getLobbyPlayerInformation(String lobbyId) throws GameException {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        ObjectNode playersJson = JsonNodeFactory.instance.objectNode();
        for (String playerId : lobby.getPlayerOrder()) {
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
        return playersJson;
    }

    /**
     * Handles a request from a client to place a settlement on the game board.
     * This method uses the {@link #handleSettlementAction} generic handler to process the request.
     *
     * @param message The {@link MessageDTO} containing the lobby ID, player ID, and settlement position ID.
     * @return A Uni emitting a {@link MessageDTO} with the updated game state (or win message),
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the settlement position ID is invalid or if the game service encounters an error
     *                       during settlement placement (e.g., rules violation, insufficient resources).
     */
    Uni<MessageDTO> placeSettlement(MessageDTO message) throws GameException {
        SettlementAction placeAction = positionId -> gameService.placeSettlement(message.getLobbyId(), message.getPlayer(), positionId);
        return handleSettlementAction(message, placeAction);
    }

    /**
     * Handles a request to upgrade a settlement to a city on the game board.
     *
     * @param message The {@link MessageDTO} containing the lobby ID, player ID, and settlement position ID.
     * @return A Uni emitting a {@link MessageDTO} with the updated game state,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the game service encounters an error during settlement upgrade.
     */
    Uni<MessageDTO> upgradeSettlement(MessageDTO message) throws GameException {
        SettlementAction upgradeAction = positionId -> gameService.upgradeSettlement(message.getLobbyId(), message.getPlayer(), positionId);
        return handleSettlementAction(message, upgradeAction);
    }

    /**
     * Generic handler for settlement actions (place or upgrade).
     * It parses the settlement position, executes the provided action, checks for a win condition,
     * and then broadcasts the updated game state.
     *
     * @param message The {@link MessageDTO} containing action details.
     * @param action  The {@link SettlementAction} to execute (e.g., place or upgrade).
     * @return A Uni emitting a {@link MessageDTO} with the updated game state or a win message.
     * @throws GameException if the settlement position ID is invalid or the action fails.
     */
    Uni<MessageDTO> handleSettlementAction(MessageDTO message, SettlementAction action) throws GameException {
        JsonNode settlementPosition = message.getMessageNode("settlementPositionId");
        try {
            int position = Integer.parseInt(settlementPosition.toString());
            action.execute(position);
        } catch (NumberFormatException e) {
            throw new GameException("Invalid settlement position id: id = %s", settlementPosition.toString());
        }

        if (playerService.checkForWin(message.getPlayer())) {
            return broadcastWin(message.getLobbyId(), message.getPlayer());
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
     * @param message The {@link MessageDTO} containing the lobby ID (code) and player ID.
     * @return A Uni emitting a {@link MessageDTO} confirming the player joined and their assigned color,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the lobby is not found or the player cannot join.
     */
    Uni<MessageDTO> joinLobby(MessageDTO message) throws GameException {
        boolean joined = lobbyService.joinLobbyByCode(message.getLobbyId(), message.getPlayer());

        if (!joined) {
            throw new GameException("Failed to join lobby: lobby session not found or full");
        }

        PlayerColor color = lobbyService.getPlayerColor(message.getLobbyId(), message.getPlayer());

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(COLOR_FIELD, color.getHexCode());
        payload.set("players", getLobbyPlayerInformation(message.getLobbyId()));
        MessageDTO playerJoinedMessage = new MessageDTO(MessageType.PLAYER_JOINED, message.getPlayer(), message.getLobbyId(), payload);
        return lobbyService.notifyPlayers(message.getLobbyId(), playerJoinedMessage);
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
     * @param message The {@link MessageDTO} containing the new username. The player ID is inferred from the connection.
     * @return A Uni emitting a {@link MessageDTO} confirming the username update, which is also broadcast to other players.
     * @throws GameException if the player session is not found.
     */
    Uni<MessageDTO> setUsername(MessageDTO message) throws GameException {
        Player player = playerService.getPlayerById(message.getPlayer());
        if (player != null) {
            player.setUsername(message.getMessage().get("username").asText());
            List<String> allPlayers = playerService.getAllPlayers().stream()
                    .map(Player::getUsername).toList();
            MessageDTO update = new MessageDTO(MessageType.LOBBY_UPDATED, player.getUniqueId(), null, allPlayers);
            return lobbyService.notifyPlayers(message.getLobbyId(), update);
        }
        throw new GameException("No player session");
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
     * Handles a dice roll request from a client.
     * This method processes the dice roll, broadcasts the result to all players in the lobby,
     * and then sends updated resource information individually to each player in that lobby.
     *
     * @param message The {@link MessageDTO} containing the player ID and lobby ID.
     * @return A Uni emitting the {@link MessageDTO} containing the dice roll result. This DTO is the one
     * that was broadcast. The primary purpose of the returned Uni is to chain asynchronous operations.
     * @throws GameException if an error occurs during dice rolling or retrieving lobby/player information.
     */
    Uni<MessageDTO> handleDiceRoll(MessageDTO message) throws GameException {
        // Roll and broadcast dice
        ObjectNode diceResult = gameService.rollDice(message.getLobbyId(), message.getPlayer());
        MessageDTO diceResultMessage = new MessageDTO(
                MessageType.DICE_RESULT,
                message.getPlayer(),
                message.getLobbyId(),
                diceResult
        );

        // send updated resources
        Lobby currentLobby = lobbyService.getLobbyById(message.getLobbyId());
        List<Uni<Void>> individualResourceSendUnis = currentLobby.getPlayers()
                .stream().map(pid -> sendPlayerResources(pid, message.getLobbyId())).toList();

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
        gameService.startGame(message.getLobbyId(), message.getPlayer());
        ObjectNode payload = createGameBoardWithPlayers(message.getLobbyId());
        MessageDTO response = new MessageDTO(MessageType.GAME_STARTED, null, message.getLobbyId(), payload);
        return lobbyService.notifyPlayers(message.getLobbyId(), response);
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

    /**
     * Broadcasts a game win message to all players in the lobby.
     * The message includes the winner's username and a leaderboard.
     *
     * @param lobbyId        The ID of the lobby where the game was won.
     * @param winnerPlayerId The ID of the player who won the game.
     * @return A Uni emitting a {@link MessageDTO} of type GAME_WON.
     */
    Uni<MessageDTO> broadcastWin(String lobbyId, String winnerPlayerId) {
        ObjectNode message = JsonNodeFactory.instance.objectNode();
        Player winner = playerService.getPlayerById(winnerPlayerId);
        message.put("winner", winner.getUsername());

        ArrayNode leaderboard = message.putArray("leaderboard");
        try {
            lobbyService.getLobbyById(lobbyId).getPlayers().stream()
                    .map(playerService::getPlayerById)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(Player::getVictoryPoints).reversed())
                    .forEach(p -> {
                        ObjectNode entry = leaderboard.addObject();
                        entry.put("username", p.getUsername());
                        entry.put("vp", p.getVictoryPoints());
                    });
        } catch (GameException e) {
            logger.errorf("Failed to fetch players for lobby %s: %s", lobbyId, e.getMessage());
            message.put("error", "Failed to build leaderboard");
        }

        MessageDTO messageDTO = new MessageDTO(MessageType.GAME_WON, winnerPlayerId, lobbyId, message);
        logger.infof("Player %s has won the game in lobby %s", winnerPlayerId, lobbyId);
        return lobbyService.notifyPlayers(lobbyId, messageDTO);
    }
}
