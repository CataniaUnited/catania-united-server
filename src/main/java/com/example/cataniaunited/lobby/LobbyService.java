package com.example.cataniaunited.lobby;

import com.example.cataniaunited.exception.GameException;

import java.util.List;

public interface LobbyService {
    String createLobby(String hostPlayer);
    String generateLobbyId();
    List<String> getOpenLobbies();
    boolean joinLobbyByCode(String lobbyId, String player);
    Lobby getLobbyById(String lobbyId) throws GameException;
    void clearLobbies();
}