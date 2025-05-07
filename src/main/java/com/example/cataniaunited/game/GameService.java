package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;

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
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        gameboard.placeSettlement(playerId, settlementPositionId);
        playerService.addVictoryPoints(playerId, 1);
    }

    public void placeRoad(String lobbyId, String playerId, int roadId) throws GameException {
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        gameboard.placeRoad(playerId, roadId);
    }

    public ObjectNode getGameboardJsonByLobbyId(String lobbyId) throws GameException{
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

    void addGameboardToList(String lobbyId, GameBoard gameboard) {
        lobbyToGameboardMap.put(lobbyId, gameboard);
    }

    public Uni<MessageDTO> broadcastWin(WebSocketConnection connection, String lobbyId, String winnerPlayerId) {
        ObjectNode message = JsonNodeFactory.instance.objectNode();
        message.put("winner", winnerPlayerId);
        MessageDTO messageDTO = new MessageDTO(MessageType.GAME_WON, winnerPlayerId, lobbyId, message);
        logger.infof("Player %s has won the game in lobby %s", winnerPlayerId, lobbyId);
        return connection.broadcast().sendText(messageDTO).chain(i -> Uni.createFrom().item(messageDTO));
    }
}
