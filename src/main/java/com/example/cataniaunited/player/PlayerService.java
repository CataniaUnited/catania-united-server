package com.example.cataniaunited.player;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PlayerService {

    private static final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();

    public Player addPlayer(WebSocketConnection connection) {
        Player player = new Player(connection);
        players.put(connection.id(), player);
        return player;
    }

    public Player getPlayerByConnection(WebSocketConnection connection) {
        return players.get(connection.id());
    }

    public List<Player> getAllPlayers() {
        return players.values().stream().toList();
    }

    public void removePlayer(WebSocketConnection connection) {
        players.remove(connection.id());
    }

}
