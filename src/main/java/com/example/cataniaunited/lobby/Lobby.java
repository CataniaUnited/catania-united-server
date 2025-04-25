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
    private List<String> turnOrder;
    private final Map<String, PlayerColor> playerColors = new ConcurrentHashMap<>();
    private final List<PlayerColor> availableColors = new CopyOnWriteArrayList<>();

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

    public boolean removePlayer(String player){
        return players.remove(player);
    }

    public List<PlayerColor> getAvailableColors(){
        return availableColors;
    }

    public void setPlayerColor(String player, PlayerColor color){
        playerColors.put(player, color);
    }

    public PlayerColor getPlayerColor (String player){
        return playerColors.get(player);
    }

    public void removePlayerColor(String player){
        playerColors.remove(player);
    }

    public PlayerColor assignAvailableColor(){
        if(availableColors.isEmpty()) return null;
        Collections.shuffle(availableColors);
        return availableColors.remove(0);
    }

    public void restoreColor(PlayerColor color){
        if(color != null && !availableColors.contains(color)){
            availableColors.add(color);
        }
    }

    /**
     * Sets the shuffled turn order for the game.
     */
    public void setTurnOrder(List<String> turnOrder) {
        this.turnOrder = turnOrder;
    }
}
