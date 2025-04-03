package com.example.cataniaunited.service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Lobby {
    private final String lobbyId;
    private final Set<String> players = new CopyOnWriteArraySet<>();

    public Lobby(String lobbyId, String hostPlayer) {
        this.lobbyId = lobbyId;
        this.players.add(hostPlayer);

        // Hard-code a couple of test names:
        this.players.add("Alice (test)");
        this.players.add("Bob (test)");
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public Set<String> getPlayers() {
        return players;
    }
}