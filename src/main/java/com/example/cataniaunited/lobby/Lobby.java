package com.example.cataniaunited.lobby;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import com.example.cataniaunited.player.PlayerColor;

public class Lobby {
    private final String lobbyId;
    private final Set<String> players = new CopyOnWriteArraySet<>();
    private final Map<String, PlayerColor> playerColors = new HashMap<>();

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

    public void addPlayer(String player){
       players.add(player);
    }

    public boolean removePlayer(String player){
        return players.remove(player);
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
}