package com.example.cataniaunited.service;

import java.util.List;

public interface LobbyService {
    String createLobby(String hostPlayer);
    List<String> getOpenLobbies();
}