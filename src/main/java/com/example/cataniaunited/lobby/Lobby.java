package com.example.cataniaunited.lobby;

import com.example.cataniaunited.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Lobby {
    private final String lobbyId;
    private final Map<String, Player> players = new ConcurrentHashMap<>();

    public Lobby(String lobbyId, String hostPlayer) {
        this.lobbyId = lobbyId;
        Player host = new Player(hostPlayer);
        players.put(host.getUsername(), host);
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public Set<String> getPlayers() {
        return players.keySet();
    }

    public Collection<Player> getPlayerObjects(){
        return players.values();
    }

    public void addPlayer(Player player){
       players.put(player.getUsername(), player);
    }

    public Player removePlayer(String username){
        return players.remove(username);
    }
}