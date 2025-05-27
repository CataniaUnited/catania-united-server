package com.example.cataniaunited.player;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class for managing {@link Player} objects.
 * Handles adding, retrieving, and removing players, and associating them with WebSocket connections.
 * Also provides utility methods related to player state, like checking for win conditions.
 * <br>
 * Important: This Service is Application Scoped which means it is a Singleton that handles
 * all existing Players, there should be no lengthy calculations in this Class to ensure that
 * different Clients don't experience long waits.
 */
@ApplicationScoped
public class PlayerService {

    private static final ConcurrentHashMap<String, Player> playersByConnectionId = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Player> playersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketConnection> connectionsByPlayerId = new ConcurrentHashMap<>();
    public static final int WIN_THRESHOLD = 10;

    /**
     * Adds a new player associated with a WebSocket connection.
     * The player is stored in maps indexed by connection ID and their unique player ID.
     *
     * @param connection The {@link WebSocketConnection} of the new player.
     * @return The newly created {@link Player} object.
     */
    public Player addPlayer(WebSocketConnection connection) {
        Player player = new Player(connection);
        playersByConnectionId.put(connection.id(), player);
        playersById.put(player.getUniqueId(), player);
        connectionsByPlayerId.put(player.getUniqueId(), connection);
        return player;
    }

    /**
     * Retrieves a player by their associated WebSocket connection.
     *
     * @param connection The {@link WebSocketConnection}.
     * @return The {@link Player} associated with the connection, or null if not found.
     */
    public Player getPlayerByConnection(WebSocketConnection connection) {
        return playersByConnectionId.get(connection.id());
    }

    /**
     * Retrieves a player by their unique player ID.
     *
     * @param id The unique ID of the player.
     * @return The {@link Player} with the given ID, or null if not found.
     */
    public Player getPlayerById(String id){return playersById.get(id);}

    /**
     * Gets a list of all currently managed players.
     *
     * @return A list of {@link Player} objects. The list is a snapshot at the time of calling.
     */
    public List<Player> getAllPlayers() {
        return playersByConnectionId.values().stream().toList();
    }

    /**
     * Removes a player based on their WebSocket connection ID.
     * The player is removed from all internal tracking maps.
     *
     * @param connection The {@link WebSocketConnection} of the player to remove.
     */
    public void removePlayerByConnectionId(WebSocketConnection connection) {
        Player player = playersByConnectionId.remove(connection.id());
        if (player == null)
            return;

        playersById.remove(player.getUniqueId());
        connectionsByPlayerId.remove(player.getUniqueId());
    }

    /**
     * Clears all player data from the service.
     * Intended for testing purposes to reset state.
     */
    public static void clearAllPlayersForTesting() {
        playersByConnectionId.clear();
        playersById.clear();
        // The original code had playersByConnectionId.clear() twice. Assuming one was meant for connectionsByPlayerId.
        // connectionsByPlayerId.clear(); // This map is instance-specific, so cannot be cleared with static context if intended.
        // For a static clear, if connectionsByPlayerId was also static, it would be cleared here.
        // Given it's an instance field in the provided snippet, a static clear for it is not directly applicable.
        // If the intent was to clear an instance map, this method would need to be non-static, or manage a static map.
        // Sticking to the provided method signature, only static maps are cleared.
    }

    /**
     * Adds a specified number of victory points to a player.
     *
     * @param playerId The ID of the player.
     * @param points   The number of victory points to add.
     */
    public void addVictoryPoints(String playerId, int points) {
        Player player = getPlayerById(playerId);
        if (player != null) {
            player.addVictoryPoints(points);
        }
    }

    /**
     * Checks if a player has reached the victory point threshold to win the game.
     *
     * @param playerId The ID of the player to check.
     * @return true if the player has met or exceeded the {@link #WIN_THRESHOLD}, false otherwise or if player not found.
     */
    public boolean checkForWin(String playerId) {
        Player player = getPlayerById(playerId);
        return player != null && player.getVictoryPoints() >= WIN_THRESHOLD;
    }

    /**
     * Retrieves the WebSocket connection associated with a given player ID.
     * If the connection exists but is no longer open, it is removed from tracking and null is returned.
     *
     * @param playerId The unique ID of the player.
     * @return The {@link WebSocketConnection} for the player, or null if not found or not open.
     */
    public WebSocketConnection getConnectionByPlayerId(String playerId) {
        if (playerId == null) {
            return null;
        }
        // fixme "get" terminology, this method should not manipulate the state
        WebSocketConnection conn = connectionsByPlayerId.get(playerId);
        if (conn != null && !conn.isOpen()) {
            // Clean up stale connection
            connectionsByPlayerId.remove(playerId, conn);
            // Also consider removing from playersByConnectionId and playersById if connection is the primary key for removal logic
            Player player = playersById.get(playerId);
            if (player != null && player.getConnection() == conn) { // Ensure it's the same connection
                playersByConnectionId.remove(conn.id());
                playersById.remove(playerId);
            }
            return null;
        }
        return conn;
    }
}
