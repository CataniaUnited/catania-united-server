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
        String lobbyId;

        do {
            lobbyId = generateLobbyId();
        } while (lobbies.containsKey(lobbyId));

        lobbies.put(lobbyId, new Lobby(lobbyId, hostPlayer));

        logger.infof("Lobby created: ID=%s, Host=%s", lobbyId, hostPlayer);

        return lobbyId;
    }

    @Override
    public String generateLobbyId() {
        String letters = getRandomCharacters("abcdefghijklmnopqrstuvwxyz", 3);
        String numbers = getRandomCharacters("0123456789", 3);

        return Math.random() < 0.5 ? letters + numbers : numbers + letters;
    }

    private String getRandomCharacters(String characters, int length) {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            stringBuilder.append(characters.charAt(random.nextInt(characters.length())));
        }
        return stringBuilder.toString();
    }

    @Override
    public List<String> getOpenLobbies() {
        List<String> openLobbies = new ArrayList<>(lobbies.keySet());

        logger.infof("Current open lobbies: %s", openLobbies);

        return openLobbies;
    }

    @Override
    public void clearLobbies() {
        lobbies.clear();
        logger.info("All lobbies have been cleared.");
    }
}