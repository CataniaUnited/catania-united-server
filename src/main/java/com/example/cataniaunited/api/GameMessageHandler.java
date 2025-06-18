package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.dto.PlayerInfo;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.fi.BuildingAction;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.game.trade.TradeRequest;
import com.example.cataniaunited.game.trade.TradingService;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.mapper.PlayerMapper;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Inject
    PlayerMapper playerMapper;

    @Inject
    TradingService tradingService;

    @Inject
    ObjectMapper objectMapper;


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
                case LEAVE_LOBBY -> leaveLobby(message);
                case SET_USERNAME -> setUsername(message);
                case PLACE_SETTLEMENT -> placeSettlement(message);
                case UPGRADE_SETTLEMENT -> upgradeSettlement(message);
                case PLACE_ROAD -> placeRoad(message);
                case ROLL_DICE -> handleDiceRoll(message);
                case PLACE_ROBBER -> placeRobber(message);
                case START_GAME -> handleStartGame(message);
                case SET_READY -> setReady(message);
                case TRADE_WITH_BANK -> handleTradeWithBank(message);
                case TRADE_WITH_PLAYER -> throw new GameException("Not yet implemented");
                case ERROR, CONNECTION_SUCCESSFUL, CLIENT_DISCONNECTED, LOBBY_CREATED, LOBBY_UPDATED, PLAYER_JOINED,
                        GAME_BOARD_JSON, GAME_WON, DICE_RESULT, ROBBER_PHASE, NEXT_TURN, GAME_STARTED, PLAYER_RESOURCE_UPDATE ->
                        throw new GameException("Invalid client command");
                case END_TURN -> endTurn(message);
            };
        } catch (GameException ge) {
            logger.errorf("Unexpected Error occurred: message = %s, error = %s", message, ge.getMessage());
            return Uni.createFrom().item(createErrorMessage(ge.getMessage()));
        }
    }

    Uni<MessageDTO> endTurn(MessageDTO message) throws GameException {
        Lobby lobby = lobbyService.getLobbyById(message.getLobbyId());
        gameService.checkRequiredPlayerStructures(message.getLobbyId(), message.getPlayer(), lobby.getRoundsPlayed());
        lobbyService.nextTurn(message.getLobbyId(), message.getPlayer());
        ObjectNode payload = getGameBoardInformation(message.getLobbyId());
        var response = new MessageDTO(MessageType.NEXT_TURN, message.getPlayer(), message.getLobbyId(), getLobbyPlayerInformation(message.getLobbyId()), payload);
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

        ObjectNode root = getGameBoardInformation(message.getLobbyId());

        MessageDTO update = new MessageDTO(
                MessageType.PLACE_ROAD,
                message.getPlayer(),
                message.getLobbyId(),
                getLobbyPlayerInformation(message.getLobbyId()),
                root
        );

        return lobbyService.notifyPlayers(message.getLobbyId(), update)
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
    ObjectNode getGameBoardInformation(String lobbyId) throws GameException {
        GameBoard gameboard = gameService.getGameboardByLobbyId(lobbyId);
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("gameboard", gameboard.getJson());
        return root;
    }

    /**
     * Handles a request from a client to place a settlement on the game board.
     * This method uses the {@link #handleSettlementAction} generic handler to process the request.
     *
     * @param message The {@link MessageDTO} containing the lobby ID, player ID, and building site ID.
     * @return A Uni emitting a {@link MessageDTO} with the updated game state (or win message),
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the building site ID is invalid or if the game service encounters an error
     *                       during settlement placement (e.g., rules violation, insufficient resources).
     */
    Uni<MessageDTO> placeSettlement(MessageDTO message) throws GameException {
        BuildingAction placeAction = positionId -> gameService.placeSettlement(message.getLobbyId(), message.getPlayer(), positionId);
        return handleSettlementAction(message, placeAction);
    }

    /**
     * Handles a request to upgrade a settlement to a city on the game board.
     *
     * @param message The {@link MessageDTO} containing the lobby ID, player ID, and building site ID.
     * @return A Uni emitting a {@link MessageDTO} with the updated game state,
     * which is also broadcast to other players in the lobby.
     * @throws GameException if the game service encounters an error during settlement upgrade.
     */
    Uni<MessageDTO> upgradeSettlement(MessageDTO message) throws GameException {
        BuildingAction upgradeAction = positionId -> gameService.upgradeSettlement(message.getLobbyId(), message.getPlayer(), positionId);
        return handleSettlementAction(message, upgradeAction);
    }

    /**
     * Generic handler for settlement actions (place or upgrade).
     * It parses the building site, executes the provided action, checks for a win condition,
     * and then broadcasts the updated game state.
     *
     * @param message The {@link MessageDTO} containing action details.
     * @param action  The {@link BuildingAction} to execute (e.g., place or upgrade).
     * @return A Uni emitting a {@link MessageDTO} with the updated game state or a win message.
     * @throws GameException if the building site ID is invalid or the action fails.
     */
    Uni<MessageDTO> handleSettlementAction(MessageDTO message, BuildingAction action) throws GameException {
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

        ObjectNode payload = getGameBoardInformation(message.getLobbyId());
        MessageDTO update = new MessageDTO(
                message.getType(),
                message.getPlayer(),
                message.getLobbyId(),
                getLobbyPlayerInformation(message.getLobbyId()),
                payload
        );

        return lobbyService.notifyPlayers(message.getLobbyId(), update)
                .chain(() -> Uni.createFrom().item(update));
    }

    Uni<MessageDTO> placeRobber(MessageDTO message) throws GameException {
        JsonNode tileId = message.getMessageNode("tileId");

        try {
            int robberTile = Integer.parseInt(tileId.toString());
            gameService.placeRobber(message.getLobbyId(), message.getPlayer(), robberTile);
        } catch (NumberFormatException e) {
            throw new GameException("Invalid tile id = %s", tileId.toString());
        }

        ObjectNode root = getGameBoardInformation(message.getLobbyId());

        MessageDTO update = new MessageDTO(
                MessageType.PLACE_ROBBER,
                message.getPlayer(),
                message.getLobbyId(),
                getLobbyPlayerInformation(message.getLobbyId()),
                root
        );

        return lobbyService.notifyPlayers(message.getLobbyId(), update)
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
        MessageDTO playerJoinedMessage = new MessageDTO(
                MessageType.PLAYER_JOINED,
                message.getPlayer(),
                message.getLobbyId(),
                getLobbyPlayerInformation(message.getLobbyId()),
                payload);
        return lobbyService.notifyPlayers(message.getLobbyId(), playerJoinedMessage);
    }

    Uni<MessageDTO> leaveLobby(MessageDTO message) throws GameException {
        lobbyService.leaveLobby(message.getLobbyId(), message.getPlayer());
        var response = new MessageDTO(MessageType.LOBBY_UPDATED, message.getPlayer(), message.getLobbyId(), getLobbyPlayerInformation(message.getLobbyId()));
        return lobbyService.notifyPlayers(message.getLobbyId(), response);
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
        return Uni.createFrom().item(
                new MessageDTO(MessageType.LOBBY_CREATED, message.getPlayer(), lobbyId, getLobbyPlayerInformation(lobbyId)));
    }

    /**
     * Handles a request to set or update a player's username.
     *
     * @param message The {@link MessageDTO} containing the new username. The player ID is inferred from the connection.
     * @return A Uni emitting a {@link MessageDTO} confirming the username update, which is also broadcast to other players.
     * @throws GameException if the player session is not found.
     */
    Uni<MessageDTO> setUsername(MessageDTO message) throws GameException {
        String username = message.getMessageNode("username").asText();
        playerService.setUsername(message.getPlayer(), username);
        MessageDTO update = new MessageDTO(MessageType.LOBBY_UPDATED, message.getPlayer(), message.getLobbyId(), getLobbyPlayerInformation(message.getLobbyId()));
        return lobbyService.notifyPlayers(message.getLobbyId(), update);
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
                getLobbyPlayerInformation(message.getLobbyId()),
                diceResult
        );

        return lobbyService.notifyPlayers(message.getLobbyId(), diceResultMessage)
            .call(() -> {
                try {
                    if(gameService.isRobberPlaced(message.getLobbyId())){
                        ObjectNode payload = getGameBoardInformation(message.getLobbyId());
                        MessageDTO waitMessage = new MessageDTO(
                                MessageType.ROBBER_PHASE,
                                message.getPlayer(),
                                message.getLobbyId(),
                                getLobbyPlayerInformation(message.getLobbyId()),
                                payload
                        );
                        return lobbyService.notifyPlayers(message.getLobbyId(), waitMessage);
                    }
                } catch (GameException e) {
                    throw new RuntimeException(e);
                }
                return Uni.createFrom().voidItem();
            })
            .replaceWith(diceResultMessage);
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
        ObjectNode payload = getGameBoardInformation(message.getLobbyId());
        MessageDTO response = new MessageDTO(
                MessageType.GAME_STARTED,
                message.getPlayer(),
                message.getLobbyId(),
                getLobbyPlayerInformation(message.getLobbyId()),
                payload);
        return lobbyService.notifyPlayers(message.getLobbyId(), response);
    }

    /**
     * Broadcasts a game win message to all players in the lobby.
     * The message includes the winner's username and a leaderboard.
     *
     * @param lobbyId        The ID of the lobby where the game was won.
     * @param winnerPlayerId The ID of the player who won the game.
     * @return A Uni emitting a {@link MessageDTO} of type GAME_WON.
     */
    Uni<MessageDTO> broadcastWin(String lobbyId, String winnerPlayerId) throws GameException {
        ObjectNode message = JsonNodeFactory.instance.objectNode();
        Player winner = playerService.getPlayerById(winnerPlayerId);
        message.put("winner", winner.getUsername());

        var players = getLobbyPlayerInformation(lobbyId);

        //Build leaderboard
        ArrayNode leaderboard = message.putArray("leaderboard");
        players.values().stream()
                .sorted(Comparator.comparingInt(PlayerInfo::victoryPoints).reversed())
                .forEach(leaderboard::addPOJO);

        MessageDTO messageDTO = new MessageDTO(MessageType.GAME_WON, winnerPlayerId, lobbyId, players, message);
        logger.infof("Player %s has won the game in lobby %s", winnerPlayerId, lobbyId);
        return lobbyService.notifyPlayers(lobbyId, messageDTO);
    }

    Uni<MessageDTO> setReady(MessageDTO message) throws GameException {
        logger.infof("Toggle ready state of player: lobbyId = %s, playerId = %s", message.getLobbyId(), message.getPlayer());
        lobbyService.toggleReady(message.getLobbyId(), message.getPlayer());
        var response = new MessageDTO(MessageType.LOBBY_UPDATED, message.getPlayer(), message.getLobbyId(), getLobbyPlayerInformation(message.getLobbyId()));
        return lobbyService.notifyPlayers(message.getLobbyId(), response);
    }

    Map<String, PlayerInfo> getLobbyPlayerInformation(String lobbyId) throws GameException {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        return lobby.getPlayers().stream()
                .map(pid -> playerService.getPlayerById(pid))
                .filter(Objects::nonNull)
                .map(player -> playerMapper.toDto(player, lobby))
                .collect(
                        Collectors.toMap(
                                PlayerInfo::id,
                                playerInfo -> playerInfo
                        )
                );
    }

    /**
     * Handles a request from a player to trade resources with the bank.
     * This method now deserializes the message payload into a TradeRequest object
     * before passing it to the TradingService.
     *
     * @param message The {@link MessageDTO} containing trade details.
     * @return A Uni emitting a {@link MessageDTO} of type {@link MessageType#PLAYER_RESOURCE_UPDATE}
     *         containing the updated player information, broadcast to all players in the lobby.
     * @throws GameException if the trade is invalid (e.g., bad format, not player's turn,
     *                       insufficient resources, or other issues from {@link TradingService}).
     */
    Uni<MessageDTO> handleTradeWithBank(MessageDTO message) throws GameException {
        // Check if player is active player -> else can't trade
        lobbyService.checkPlayerTurn(message.getLobbyId(), message.getPlayer());

        TradeRequest tradeRequest;
        try {
            // Deserialize the JSON payload into the TradeRequest record.
            tradeRequest = objectMapper.treeToValue(message.getMessage(), TradeRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            logger.errorf("Failed to parse trade request: %s", e.getMessage());
            throw new GameException("Invalid trade request format.");
        }

        // Try to trade with the clean TradeRequest object -> if not successful GameException
        tradingService.handleBankTradeRequest(message.getPlayer(), tradeRequest);

        // Trade successful, get updated player information (which includes resources)
        Map<String, PlayerInfo> updatedPlayerInfos = getLobbyPlayerInformation(message.getLobbyId());


        MessageDTO updateResponse = new MessageDTO(
                MessageType.PLAYER_RESOURCE_UPDATE,
                message.getPlayer(),
                message.getLobbyId(),
                updatedPlayerInfos
        );

        logger.infof("Player %s completed trade with bank in lobby %s. Broadcasting PLAYER_RESOURCE_UPDATE.", message.getPlayer(), message.getLobbyId());

        // Notify all players in the lobby about the new Resource Distribution
        return lobbyService.notifyPlayers(message.getLobbyId(), updateResponse)
                .chain(() -> Uni.createFrom().item(updateResponse));
    }
}