package com.example.cataniaunited.lobby;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Lobby {
    private final String lobbyId;
    private final Set<String> players = new CopyOnWriteArraySet<>();
    private List<String> turnOrder;

    public Lobby(String lobbyId, String hostPlayer) {
        this.lobbyId = lobbyId;
        this.players.add(hostPlayer);
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public Set<String> getPlayers() {
        return players;
    }

    /**
     * Adds a new player to this lobby.
     */
    public void addPlayer(String player) {
        this.players.add(player);
    }

    /**
     * Gets the current turn order list, set when the game starts.
     */
    public List<String> getTurnOrder() {
        return turnOrder;
    }

    /**
     * Sets the shuffled turn order for the game.
     */
    public void setTurnOrder(List<String> turnOrder) {
        this.turnOrder = turnOrder;
    }
}
