package com.example.cataniaunited.player;

import io.quarkus.websockets.next.WebSocketConnection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class Player {

    private String username;
    private final String uniqueId;
    private final String connectionId;
    private static final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();

    public Player() {
        this.username = "RandomPlayer_4";
        this.uniqueId = UUID.randomUUID().toString();
        this.connectionId = null;
    }

    public Player(String username) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connectionId = null;
    }

    public Player(WebSocketConnection connection) {
        this("RandomPlayer_4", connection);
    }

    public Player(String username, WebSocketConnection connection) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connectionId = connection.id();
        players.put(this.connectionId, this);
    }

    public static Player getPlayerByConnection(WebSocketConnection connection) {
        return players.get(connection.id());
    }

    public static void removePlayer(WebSocketConnection connection) {
        players.remove(connection.id());
    }

    // NEW: Returns a list of all connected players.
    public static List<Player> getAllPlayers() {
        return new ArrayList<>(players.values());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='" + connectionId + '\'' +
                '}';
    }
}
