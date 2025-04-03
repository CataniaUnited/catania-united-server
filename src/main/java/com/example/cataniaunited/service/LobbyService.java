package com.example.cataniaunited.service;

import java.util.List;

public interface LobbyService {
    String createLobby(String hostPlayer);
    String generateLobbyID();
    List<String> getOpenLobbies();
    void clearLobbies();
}