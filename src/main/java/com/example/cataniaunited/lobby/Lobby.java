package com.example.cataniaunited.lobby;

import com.example.cataniaunited.player.PlayerColor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class Lobby {
    private final String lobbyId;
    private final Set<String> players = new CopyOnWriteArraySet<>();
    private final Map<String, PlayerColor> playerColors = new ConcurrentHashMap<>();
    private final List<PlayerColor> availableColors = new CopyOnWriteArrayList<>();
    private volatile String activePlayer;

    public Lobby(String lobbyId, String hostPlayer) {
        this.lobbyId = lobbyId;
        this.players.add(hostPlayer);
        Collections.addAll(availableColors, PlayerColor.values());
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public Set<String> getPlayers() {
        return players;
    }

    public void addPlayer(String player) {
        players.add(player);
    }

    public boolean removePlayer(String player) {
        return players.remove(player);
    }

    public String getActivePlayer() {
        return activePlayer;
    }

    //TODO: Remove after implementation of player turn order
    public void setActivePlayer(String activePlayer) {
        this.activePlayer = activePlayer;
    }

    public boolean isPlayerTurn(String player) {
        return player != null && player.equals(activePlayer);
    }

    public List<PlayerColor> getAvailableColors() {
        return availableColors;
    }

    public void setPlayerColor(String player, PlayerColor color) {
        playerColors.put(player, color);
    }

    public PlayerColor getPlayerColor(String player) {
        return playerColors.get(player);
    }

    public void removePlayerColor(String player) {
        playerColors.remove(player);
    }

    public PlayerColor assignAvailableColor() {
        if (availableColors.isEmpty()) return null;
        Collections.shuffle(availableColors);
        return availableColors.remove(0);
    }

    public void restoreColor(PlayerColor color) {
        if (color != null && !availableColors.contains(color)) {
            availableColors.add(color);
        }
    }
}