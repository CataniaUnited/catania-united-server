package com.example.cataniaunited.lobby;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the {@link LobbyService} interface.
 * Manages game lobbies using a concurrent map for storage.
 * <br>
 * Important: This Service is Application Scoped which means it is a Singleton that handles
 * all existing Lobbies, there should be no lengthy calculations in this Class to ensure that
 * different Clients don't experience long waits.
 * */
@ApplicationScoped
public class LobbyServiceImpl implements LobbyService {

    private static final Logger logger = Logger.getLogger(LobbyServiceImpl.class);
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();

    @Inject
    PlayerService playerService;

    /**
     * {@inheritDoc}
     * Creates a new lobby, assigns a color to the host, and stores the lobby.
     */
    @Override
    public String createLobby(String hostPlayer) {
        String lobbyId;
        do {
            lobbyId = generateLobbyId();
        } while (lobbies.containsKey(lobbyId));
        Lobby lobby = new Lobby(lobbyId, hostPlayer);
        setPlayerColor(lobby, hostPlayer); // Assign a color to the host
        lobbies.put(lobbyId, lobby);
        logger.infof("Lobby created: ID=%s, Host=%s", lobbyId, hostPlayer);
        return lobbyId;
    }

    /**
     * {@inheritDoc}
     * Generates a 6-character ID consisting of 3 random letters and 3 random numbers, in a random order.
     */
    @Override
    public String generateLobbyId() {
        String letters = getRandomCharacters("abcdefghijklmnopqrstuvwxyz", 3);
        String numbers = getRandomCharacters("0123456789", 3);
        return secureRandom.nextBoolean() ? letters + numbers : numbers + letters;
    }

    /**
     * Generates a random string of a specified length from a given set of characters.
     *
     * @param characters The string of characters to choose from.
     * @param length     The desired length of the random string.
     * @return A randomly generated string.
     */
    private String getRandomCharacters(String characters, int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(characters.length());
            stringBuilder.append(characters.charAt(index));
        }
        return stringBuilder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getOpenLobbies() {
        List<String> openLobbies = new ArrayList<>(lobbies.keySet());
        logger.infof("Current open lobbies: %s", openLobbies);
        return openLobbies;
    }

    /**
     * {@inheritDoc}
     * If joining is successful, a color is assigned to the player.
     * @throws GameException if the lobby is not found.
     */
    @Override
    public boolean joinLobbyByCode(String lobbyId, String player) throws GameException {
        Lobby lobby = getLobbyById(lobbyId);
        if (lobby != null) {
            PlayerColor assignedColor = setPlayerColor(lobby, player);
            if (assignedColor == null) {
                return false;
            }
            lobby.addPlayer(player);
            logger.infof("Player %s joined lobby %s with color %s", player, lobbyId, assignedColor);
            return true;
        }
        logger.warnf("Invalid or expired lobby ID: %s", lobbyId);
        return false;
    }

    /**
     * Assigns an available color to a player within a specific lobby.
     *
     * @param lobby  The {@link Lobby} where the player is.
     * @param player The ID of the player to assign a color to.
     * @return The assigned {@link PlayerColor}, or null if no colors are available.
     */
    protected PlayerColor setPlayerColor(Lobby lobby, String player) {
        PlayerColor assignedColor = lobby.assignAvailableColor();
        if (assignedColor == null) {
            logger.warnf("No colors available for new players in lobby %s.", lobby.getLobbyId());
            return null;
        }
        lobby.setPlayerColor(player, assignedColor);
        return assignedColor;
    }

    /**
     * {@inheritDoc}
     * Restores the player's color to the available pool if they had one.
     */
    @Override
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
        } else {
            logger.warnf("Attempted to remove player from non-existing lobby: %s", lobbyId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lobby getLobbyById(String lobbyId) throws GameException {
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) {
            logger.errorf("Lobby not found: id = %s", lobbyId);
            throw new GameException("Lobby with id %s not found", lobbyId);
        }
        return lobby;
    }

    /**
     * {@inheritDoc}
     * @throws GameException if the lobby is not found.
     */
    @Override
    public boolean isPlayerTurn(String lobbyId, String playerId) throws GameException {
        Lobby lobby = getLobbyById(lobbyId);
        return lobby.isPlayerTurn(playerId);
    }

    /**
     * {@inheritDoc}
     * @throws GameException if the lobby is not found or the player has no assigned color.
     */
    @Override
    public PlayerColor getPlayerColor(String lobbyId, String playerId) throws GameException {
        Lobby lobby = getLobbyById(lobbyId);
        PlayerColor playerColor = lobby.getPlayerColor(playerId);
        if (playerColor == null) {
            throw new GameException("No color for player found: playerId=%s, lobbyId=%s", playerId, lobbyId);
        }
        return playerColor;
    }


    /**
     * {@inheritDoc}
     * Uses {@link PlayerService} to get actual Player objects to send messages.
     * @throws GameException if the lobby is not found.
     */
    @Override
    public void notifyPlayers(String lobbyId, MessageDTO dto) throws GameException {
        Lobby lob = getLobbyById(lobbyId);
        for (String pid : lob.getPlayers()) {
            Player p = playerService.getPlayerById(pid);
            if (p != null) p.sendMessage(dto);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void clearLobbies() {
        lobbies.clear();
        logger.info("All lobbies have been cleared.");
    }
}