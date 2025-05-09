package com.example.cataniaunited.lobby;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.PlayerColor;

import java.util.List;

public interface LobbyService {
    String createLobby(String hostPlayer);

    String generateLobbyId();

    List<String> getOpenLobbies();

    boolean joinLobbyByCode(String lobbyId, String player) throws GameException;

    void removePlayerFromLobby(String lobbyId, String player);

    Lobby getLobbyById(String lobbyId) throws GameException;

    void clearLobbies();

    boolean isPlayerTurn(String lobbyId, String playerId) throws GameException;

    PlayerColor getPlayerColor(String lobbyId, String playerId) throws GameException;
}