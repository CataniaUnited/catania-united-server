package com.example.cataniaunited.game;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.InvalidTurnException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class for managing game logic, including game board creation,
 * player actions (placing settlements, roads, cities), dice rolling,
 * and game state transitions like starting a game and determining a winner.
 * <br>
 * Important: This Service is Application Scoped which means it is a Singleton that handles
 * all Games, there should be no lengthy calculations in this Class to ensure that different
 * Clients don't experience long waits.
 */
@ApplicationScoped
public class GameService {

    private static final Logger logger = Logger.getLogger(GameService.class);
    private static final ConcurrentHashMap<String, GameBoard> lobbyToGameboardMap = new ConcurrentHashMap<>();

    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;


    /**
     * Creates a new game board for the specified lobby.
     * The size of the game board is determined by the number of players in the lobby.
     *
     * @param lobbyId The ID of the lobby for which to create the game board.
     * @return The newly created {@link GameBoard}.
     * @throws GameException if the lobby is not found or an error occurs during board creation.
     */
    public GameBoard createGameboard(String lobbyId) throws GameException {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        GameBoard gameboard = new GameBoard(lobby.getPlayers().size());
        addGameboardToList(lobby.getLobbyId(), gameboard);
        return gameboard;
    }

    /**
     * Allows a player to place a settlement on the game board.
     *
     * @param lobbyId              The ID of the lobby.
     * @param playerId             The ID of the player placing the settlement.
     * @param settlementPositionId The ID of the position where the settlement is to be placed.
     * @throws GameException if it's not the player's turn, the position is invalid,
     *                       or other game rules are violated.
     */
    public void placeSettlement(String lobbyId, String playerId, int settlementPositionId) throws GameException {
        checkPlayerTurn(lobbyId, playerId);
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        PlayerColor color = lobbyService.getPlayerColor(lobbyId, playerId);
        gameboard.placeSettlement(playerService.getPlayerById(playerId), color, settlementPositionId);
        playerService.addVictoryPoints(playerId, 1);
    }

    /**
     * Allows a player to upgrade an existing settlement to a city.
     *
     * @param lobbyId              The ID of the lobby.
     * @param playerId             The ID of the player upgrading the settlement.
     * @param settlementPositionId The ID of the position of the settlement to be upgraded.
     * @throws GameException if it's not the player's turn, the position is invalid,
     *                       no settlement exists, or other game rules are violated.
     */
    public void upgradeSettlement(String lobbyId, String playerId, int settlementPositionId) throws GameException {
        checkPlayerTurn(lobbyId, playerId);
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        PlayerColor color = lobbyService.getPlayerColor(lobbyId, playerId);
        gameboard.placeCity(playerService.getPlayerById(playerId), color, settlementPositionId);
        playerService.addVictoryPoints(playerId, 2);
    }

    /**
     * Allows a player to place a road on the game board.
     *
     * @param lobbyId  The ID of the lobby.
     * @param playerId The ID of the player placing the road.
     * @param roadId   The ID of the road to be placed.
     * @throws GameException if it's not the player's turn, the road ID is invalid,
     *                       or other game rules are violated.
     */
    public void placeRoad(String lobbyId, String playerId, int roadId) throws GameException {
        checkPlayerTurn(lobbyId, playerId);
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        PlayerColor color = lobbyService.getPlayerColor(lobbyId, playerId);
        gameboard.placeRoad(playerService.getPlayerById(playerId), color, roadId);
    }

    /**
     * Starts the game in the specified lobby.
     * This involves creating a game board, setting player order, and notifying players.
     *
     * @param lobbyId The ID of the lobby where the game is to be started.
     * @return A {@link MessageDTO} of type GAME_STARTED containing initial game state (player order, game board).
     * @throws GameException if the game cannot be started (e.g., already started, not enough players).
     */
    public MessageDTO startGame(String lobbyId) throws GameException {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);

        if (lobby.isGameStarted())
            throw new GameException("Game already started");
        if (lobby.getPlayers().size() < 2)
            throw new GameException("Need at least 2 players");

        GameBoard board = createGameboard(lobbyId);

        List<String> order = new ArrayList<>(lobby.getPlayers());
        Collections.shuffle(order);
        lobby.setPlayerOrder(order);
        lobby.setActivePlayer(order.get(0));
        lobby.setGameStarted(true);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.putPOJO("playerOrder", order);
        payload.set("gameboard", board.getJson());

        MessageDTO dto = new MessageDTO(
                MessageType.GAME_STARTED, null, lobbyId, payload);

        order.stream()
                .map(playerService::getPlayerById)
                .filter(Objects::nonNull)
                .forEach(p -> p.sendMessage(dto));

        logger.infof("Game started in lobby: lobbyId=%s, order=%s", lobbyId, order);
        return dto;
    }

    /**
     * Retrieves the JSON representation of the game board for a given lobby.
     *
     * @param lobbyId The ID of the lobby.
     * @return An {@link ObjectNode} containing the game board's JSON structure.
     * @throws GameException if the game board for the lobby is not found.
     */
    public ObjectNode getGameboardJsonByLobbyId(String lobbyId) throws GameException {
        GameBoard gameBoard = getGameboardByLobbyId(lobbyId);
        return gameBoard.getJson();
    }

    /**
     * Retrieves the {@link GameBoard} instance for a given lobby.
     *
     * @param lobbyId The ID of the lobby.
     * @return The {@link GameBoard} associated with the lobby.
     * @throws GameException if the game board for the lobby is not found.
     */
    public GameBoard getGameboardByLobbyId(String lobbyId) throws GameException {
        GameBoard gameboard = lobbyToGameboardMap.get(lobbyId);
        if (gameboard == null) {
            logger.errorf("Gameboard for Lobby not found: id = %s", lobbyId);
            throw new GameException("Gameboard for Lobby not found: id = %s", lobbyId);
        }
        return gameboard;
    }

    /**
     * Checks if it is currently the specified player's turn in the given lobby.
     *
     * @param lobbyId  The ID of the lobby.
     * @param playerId The ID of the player.
     * @throws GameException if it is not the player's turn or if the lobby/player is not found.
     */
    private void checkPlayerTurn(String lobbyId, String playerId) throws GameException {
        if (!lobbyService.isPlayerTurn(lobbyId, playerId)) {
            logger.errorf("It is not the players turn: playerId=%s, lobbyId=%s", playerId, lobbyId);
            throw new InvalidTurnException();
        }
    }

    /**
     * Adds a game board to the internal map, associating it with a lobby ID.
     *
     * @param lobbyId   The ID of the lobby.
     * @param gameboard The {@link GameBoard} to add.
     */
    void addGameboardToList(String lobbyId, GameBoard gameboard) {
        lobbyToGameboardMap.put(lobbyId, gameboard);
    }


    /**
     * Simulates a dice roll for the game in the specified lobby.
     * The result of the roll determines resource distribution.
     *
     * @param lobbyId The ID of the lobby where the dice are rolled.
     * @return An {@link ObjectNode} containing the results of the two dice and their total.
     * @throws GameException if the game board for the lobby is not found.
     */
    public ObjectNode rollDice(String lobbyId, String playerId) throws GameException {
        checkPlayerTurn(lobbyId, playerId);
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        return gameboard.rollDice();
    }

    /**
     * Broadcasts a game win message to all players in the lobby.
     * The message includes the winner's username and a leaderboard.
     *
     * @param connection     The WebSocket connection (used for broadcasting).
     * @param lobbyId        The ID of the lobby where the game was won.
     * @param winnerPlayerId The ID of the player who won the game.
     * @return A Uni emitting a {@link MessageDTO} of type GAME_WON.
     */
    public Uni<MessageDTO> broadcastWin(WebSocketConnection connection, String lobbyId, String winnerPlayerId) {
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
        return connection.broadcast().sendText(messageDTO).chain(i -> Uni.createFrom().item(messageDTO));

    }

    /**
     * Clears all game boards from the memory. Intended for testing purposes.
     */
    public void clearGameBoardsForTesting() {
        lobbyToGameboardMap.clear();
        logger.info("All game boards have been cleared for testing.");
    }
}