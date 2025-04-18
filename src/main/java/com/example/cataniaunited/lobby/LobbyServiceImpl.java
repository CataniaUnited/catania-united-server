package com.example.cataniaunited.lobby;

import com.example.cataniaunited.exception.GameException;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class LobbyServiceImpl implements LobbyService {

    private static final Logger logger = Logger.getLogger(LobbyServiceImpl.class);
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();

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

        return secureRandom.nextBoolean() ? letters + numbers : numbers + letters;
    }

    private String getRandomCharacters(String characters, int length) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(characters.length());
            stringBuilder.append(characters.charAt(index));
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
    public boolean joinLobbyByCode(String lobbyId, String player) {
        Lobby lobby = lobbies.get(lobbyId);
        if(lobby != null){
            lobby.addPlayer(player);
            logger.infof("Player %s joined lobby %s", player, lobbyId);
            return true;
        }

        logger.warnf("Invalid or expired lobby ID: %s", lobbyId);
        return false;
    }

    @Override
    public Lobby getLobbyById(String lobbyId) throws GameException {
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) {
            logger.errorf("Lobby not found: id = %s", lobbyId);
            throw new GameException("Lobby with id %s not found", lobbyId);
        }
        return lobby;
    }

    @Override
    public void clearLobbies() {
        lobbies.clear();
        logger.info("All lobbies have been cleared.");
    }
}