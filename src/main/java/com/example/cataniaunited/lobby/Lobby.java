package com.example.cataniaunited.lobby;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class Lobby {
    private final String lobbyId;
    private final Set<String> players = new CopyOnWriteArraySet<>();

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
    /*
    public List<String> getPlayerNames() {
        return players.values().stream()
                .map(Player::getUsername)
                .toList();
    }

    public Collection<Player> getPlayerObjects(){
        return players.values();
    }*/

    public void addPlayer(String player){
       players.add(player);
    }

    public boolean removePlayer(String player){
        return players.remove(player);
    }
}