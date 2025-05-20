package com.example.cataniaunited.lobby;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.PlayerColor;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Interface for lobby management services.
 * Defines operations for creating, joining, and managing game lobbies.
 */
public interface LobbyService {
    /**
     * Creates a new lobby with the specified player as the host.
     *
     * @param hostPlayer The ID of the player creating the lobby.
     * @return The unique ID of the newly created lobby.
     */
    String createLobby(String hostPlayer);

    /**
     * Generates a unique ID for a new lobby.
     *
     * @return A unique lobby ID string.
     */
    String generateLobbyId();

    /**
     * Gets a list of IDs of all currently open (active) lobbies.
     *
     * @return A list of lobby ID strings.
     */
    List<String> getOpenLobbies();

    /**
     * Allows a player to join an existing lobby using its ID (code).
     *
     * @param lobbyId The ID of the lobby to join.
     * @param player  The ID of the player wishing to join.
     * @return true if the player successfully joined the lobby, false otherwise.
     */
    boolean joinLobbyByCode(String lobbyId, String player);

    /**
     * Removes a player from a specified lobby.
     *
     * @param lobbyId The ID of the lobby.
     * @param player  The ID of the player to remove.
     */
    void removePlayerFromLobby(String lobbyId, String player);

    /**
     * Retrieves a lobby by its unique ID.
     *
     * @param lobbyId The ID of the lobby to retrieve.
     * @return The {@link Lobby} object.
     * @throws GameException if the lobby with the specified ID is not found.
     */
    Lobby getLobbyById(String lobbyId) throws GameException;

    /**
     * Clears all existing lobbies. Intended primarily for testing purposes.
     */
    void clearLobbies();

    /**
     * Checks if it is currently the specified player's turn in the given lobby.
     *
     * @param lobbyId  The ID of the lobby.
     * @param playerId The ID of the player.
     * @return true if it is the player's turn, false otherwise.
     * @throws GameException if the lobby or player is not found.
     */
    boolean isPlayerTurn(String lobbyId, String playerId) throws GameException;

    /**
     * Gets the color assigned to a specific player in a given lobby.
     *
     * @param lobbyId  The ID of the lobby.
     * @param playerId The ID of the player.
     * @return The {@link PlayerColor} assigned to the player.
     * @throws GameException if the lobby, player, or color assignment is not found.
     */
    PlayerColor getPlayerColor(String lobbyId, String playerId) throws GameException;

    /**
     * Notifies all players in a specified lobby by sending them a {@link MessageDTO}.
     *
     * @param lobbyId The ID of the lobby whose players should be notified.
     * @param dto     The {@link MessageDTO} to send.
     * @return The {@link Uni} containing the sent message
     */
    Uni<MessageDTO> notifyPlayers(String lobbyId, MessageDTO dto);

    String nextTurn(String lobbyId, String playerId) throws GameException;

}