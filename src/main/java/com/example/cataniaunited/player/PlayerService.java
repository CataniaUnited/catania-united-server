package com.example.cataniaunited.player;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PlayerService {

    private static final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Player> playersById = new ConcurrentHashMap<>();

    public Player addPlayer(WebSocketConnection connection) {
        Player player = new Player(connection);
        players.put(connection.id(), player);
        playersById.put(player.getUniqueId(), player);
        return player;
    }

    public Player getPlayerByConnection(WebSocketConnection connection) {
        return players.get(connection.id());
    }

    public Player getPlayerById(String playerId) {
        return playersById.get(playerId);
    }

    public List<Player> getAllPlayers() {
        return players.values().stream().toList();
    }

    public void removePlayer(WebSocketConnection connection) {
        Player removed = players.remove(connection.id());
        if (removed != null) {
            playersById.remove(removed.getUniqueId());
        }
    }

    public void addVictoryPoints(String playerId, int points) {
        Player player = getPlayerById(playerId);
        if (player != null) {
            player.addVictoryPoints(points);
        }
    }

    public boolean checkForWin(String playerId) {
        Player player = getPlayerById(playerId);
        return player != null && player.getVictoryPoints() >= 10;
    }


}
