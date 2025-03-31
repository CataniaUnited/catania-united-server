package com.example.cataniaunited.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class LobbyServiceImpl implements LobbyService {

    private static final Logger logger = Logger.getLogger(LobbyServiceImpl.class);
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();

    @Override
    public String createLobby(String hostPlayer) {
        String lobbyId = UUID.randomUUID().toString().substring(0, 6);
        lobbies.put(lobbyId, new Lobby(lobbyId, hostPlayer));

        logger.infof("Lobby created: ID=%s, Host=%s", lobbyId, hostPlayer);

        return lobbyId;
    }

    @Override
    public List<String> getOpenLobbies() {
        List<String> openLobbies = new ArrayList<>(lobbies.keySet());

        logger.infof("Current open lobbies: %s", openLobbies);

        return openLobbies;
    }
}