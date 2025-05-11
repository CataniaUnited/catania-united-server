package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GameService {

    private static final Logger logger = Logger.getLogger(GameService.class);
    private static final ConcurrentHashMap<String, GameBoard> lobbyToGameboardMap = new ConcurrentHashMap<>();

    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;


    public GameBoard createGameboard(String lobbyId) throws GameException {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        GameBoard gameboard = new GameBoard(lobby.getPlayers().size());
        addGameboardToList(lobby.getLobbyId(), gameboard);
        return gameboard;
    }

    public void placeSettlement(String lobbyId, String playerId, int settlementPositionId) throws GameException {
        checkPlayerTurn(lobbyId, playerId);
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        PlayerColor color = lobbyService.getPlayerColor(lobbyId, playerId);
        gameboard.placeSettlement(playerService.getPlayerById(playerId), color, settlementPositionId);
        playerService.addVictoryPoints(playerId, 1);
    }

    public void placeRoad(String lobbyId, String playerId, int roadId) throws GameException {
        checkPlayerTurn(lobbyId, playerId);
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        PlayerColor color = lobbyService.getPlayerColor(lobbyId, playerId);
        gameboard.placeRoad(playerId, color, roadId);
    }

    public ObjectNode getGameboardJsonByLobbyId(String lobbyId) throws GameException {
        GameBoard gameBoard = getGameboardByLobbyId(lobbyId);
        return gameBoard.getJson();
    }

    public GameBoard getGameboardByLobbyId(String lobbyId) throws GameException {
        GameBoard gameboard = lobbyToGameboardMap.get(lobbyId);
        if (gameboard == null) {
            logger.errorf("Gameboard for Lobby not found: id = %s", lobbyId);
            throw new GameException("Gameboard for Lobby not found: id = %s", lobbyId);
        }
        return gameboard;
    }

    private void checkPlayerTurn(String lobbyId, String playerId) throws GameException {
        if (!lobbyService.isPlayerTurn(lobbyId, playerId)) {
            throw new GameException("It is not the players turn: playerId=%s, lobbyId=%s", playerId, lobbyId);
        }
    }

    void addGameboardToList(String lobbyId, GameBoard gameboard) {
        lobbyToGameboardMap.put(lobbyId, gameboard);
    }


    public ObjectNode rollDice(String lobbyId) throws GameException {
        return getGameboardByLobbyId(lobbyId).rollDice();
    }
  
    public Uni<MessageDTO> broadcastWin(WebSocketConnection connection, String lobbyId, String winnerPlayerId) {
        ObjectNode message = JsonNodeFactory.instance.objectNode();
        Player winner = playerService.getPlayerById(winnerPlayerId);
        message.put("winner", winner.getUsername());

        ArrayNode leaderboard = message.putArray("leaderboard");
        try {
            lobbyService.getLobbyById(lobbyId).getPlayers().stream()
                    .map(playerService::getPlayerById)
                    .filter(p -> p != null)
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

    public void clearGameBoardsForTesting() {
        lobbyToGameboardMap.clear();
        logger.info("All game boards have been cleared for testing.");
    }
}
