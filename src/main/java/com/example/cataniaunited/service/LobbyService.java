package com.example.cataniaunited.service;

import java.util.List;

public interface LobbyService {
    String createLobby(String hostPlayer);
    String generateLobbyId();
    List<String> getOpenLobbies();
    boolean joinLobbyByCode(String lobbyId, String player);
    void clearLobbies();
}