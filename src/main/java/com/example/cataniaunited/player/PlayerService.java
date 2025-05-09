package com.example.cataniaunited.player;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PlayerService {

    private static final ConcurrentHashMap<String, Player> playersByConnectionId = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Player> playersById = new ConcurrentHashMap<>();
    public static final int WIN_THRESHOLD = 10;

    public Player addPlayer(WebSocketConnection connection) {
        Player player = new Player(connection);
        playersByConnectionId.put(connection.id(), player);
        playersById.put(player.getUniqueId(), player);
        return player;
    }

    public Player getPlayerByConnection(WebSocketConnection connection) {
        return playersByConnectionId.get(connection.id());
    }

    public Player getPlayerById(String id){return playersById.get(id);}

    public List<Player> getAllPlayers() {
        return playersByConnectionId.values().stream().toList();
    }

    public void removePlayerByConnectionId(WebSocketConnection connection) {
        Player player = playersByConnectionId.remove(connection.id());
        if (player == null)
            return;

        playersById.remove(player.getUniqueId());
    }

    public static void clearAllPlayersForTesting() {
        playersByConnectionId.clear();
        playersById.clear();
    }
    
    public void removePlayer(WebSocketConnection connection) {
        Player removed = playersByConnectionId.remove(connection.id());
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
        return player != null && player.getVictoryPoints() >= WIN_THRESHOLD;
    }


}
