package com.example.cataniaunited.lobby;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.PlayerColor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        if (lobby != null) {
            PlayerColor assignedColor = lobby.assignAvailableColor();
            if (assignedColor == null) {
                logger.warnf("No colors available for new players in lobby %s.", lobbyId);
                return false;
            }
            lobby.addPlayer(player);
            lobby.setPlayerColor(player, assignedColor);

            logger.infof("Player %s joined lobby %s with color %s", player, lobbyId, assignedColor);
            return true;
        }

        logger.warnf("Invalid or expired lobby ID: %s", lobbyId);
        return false;
    }

    public void removePlayerFromLobby(String lobbyId, String player) {
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby != null) {
            PlayerColor color = lobby.getPlayerColor(player);

            if (color != null) {
                lobby.restoreColor(color);
                logger.infof("Color %s returned to pool from player %s", color, player);
            }

            lobby.removePlayer(player);
            lobby.removePlayerColor(player);

            logger.infof("Player %s removed from lobby %s", player, lobbyId);
        }
        logger.warnf("Attempted to remove player from non-existing lobby: %s", lobbyId);
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
    public boolean isPlayerTurn(String lobbyId, String playerId) throws GameException {
        Lobby lobby = getLobbyById(lobbyId);
        return lobby.isPlayerTurn(playerId);
    }

    @Override
    public PlayerColor getPlayerColor(String lobbyId, String playerId) throws GameException {
        Lobby lobby = getLobbyById(lobbyId);
        PlayerColor playerColor = lobby.getPlayerColor(playerId);
        if (playerColor == null) {
            throw new GameException("No color for player found: playerId=%s, lobbyId=%s", playerId, lobbyId);
        }
        return playerColor;
    }

    @Override
    public void clearLobbies() {
        lobbies.clear();
        logger.info("All lobbies have been cleared.");
    }
}