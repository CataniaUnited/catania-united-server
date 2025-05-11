// src/main/java/com/example/cataniaunited/lobby/Lobby.java
package com.example.cataniaunited.lobby;

import com.example.cataniaunited.player.PlayerColor;
import java.util.*;
import java.util.concurrent.*;


public class Lobby {
    private final String lobbyId;
    private final String hostPlayer;
    private final Set<String> players = new CopyOnWriteArraySet<>();
    private final Map<String, PlayerColor> playerColors = new ConcurrentHashMap<>();
    private final List<PlayerColor> availableColors = new CopyOnWriteArrayList<>();
    private volatile String activePlayer;
    private volatile boolean gameStarted = false;

    public Lobby(String lobbyId, String hostPlayer) {
        this.lobbyId = lobbyId;
        this.hostPlayer = hostPlayer;
        players.add(hostPlayer);
        Collections.addAll(availableColors, PlayerColor.values());
    }

    public String getLobbyId() { return lobbyId; }
    public Set<String> getPlayers() { return players; }

    public void addPlayer(String player) {
        players.add(player);
    }

    public boolean removePlayer(String player) {
        playerColors.remove(player);
        return players.remove(player);
    }

    public String getActivePlayer() { return activePlayer; }
    public void setActivePlayer(String activePlayer) { this.activePlayer = activePlayer; }

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

    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean started) { this.gameStarted = started; }


    public boolean canStartGame(String requestingPlayer) {
        return hostPlayer.equals(requestingPlayer)
                && players.size() >= 2
                && !gameStarted;
    }


    public void randomizePlayerOrder() {
        List<String> order = new ArrayList<>(players);
        Collections.shuffle(order);
        players.clear();
        players.addAll(order);
        activePlayer = order.get(0);
        gameStarted = true;
    }


    public void nextPlayerTurn() {
        if (players.isEmpty() || activePlayer == null) return;
        List<String> order = new ArrayList<>(players);
        int idx = order.indexOf(activePlayer);
        activePlayer = order.get((idx + 1) % order.size());
    }

    public void resetForNewGame() {
        this.gameStarted = false;
        this.activePlayer = null;
    }


    public void setPlayerOrder(List<String> order) {
        players.clear();
        players.addAll(order);
    }

}
