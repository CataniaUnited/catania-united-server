package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GameService {

    private static final Logger logger = Logger.getLogger(GameService.class);
    private static final ConcurrentHashMap<String, GameBoard> lobbyToGameboardMap = new ConcurrentHashMap<>();

    @Inject
    LobbyService lobbyService;

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
        gameboard.placeSettlement(playerId, color, settlementPositionId);
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
        GameBoard gameboard = getGameboardByLobbyId(lobbyId);
        return gameboard.rollDice();
    }
}
