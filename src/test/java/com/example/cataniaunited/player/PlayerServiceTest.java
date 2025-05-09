package com.example.cataniaunited.player;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class PlayerServiceTest {

    @Inject
    PlayerService playerService;

    private WebSocketConnection mockConnection1;
    private WebSocketConnection mockConnection2;
    private final String mockConnId1 = "connId_1";
    private final String mockConnId2 = "connId_2";

    @BeforeEach
    void setUp() {
        PlayerService.clearAllPlayersForTesting();

        mockConnection1 = mock(WebSocketConnection.class);
        when(mockConnection1.id()).thenReturn(mockConnId1);

        mockConnection2 = mock(WebSocketConnection.class);
        when(mockConnection2.id()).thenReturn(mockConnId2);
    }

    @Test
    void addPlayerShouldCreateAndStorePlayer() {
        Player addedPlayer = playerService.addPlayer(mockConnection1);

        assertNotNull(addedPlayer, "Added player should not be null.");
        assertEquals(mockConnId1, addedPlayer.connectionId, "Player's connection ID should match.");
        assertTrue(addedPlayer.getUsername().startsWith("RandomPlayer_"), "Player should have a random default username.");
        assertNotNull(addedPlayer.getUniqueId(), "Player should have a unique ID.");

        Player retrievedByConn = playerService.getPlayerByConnection(mockConnection1);
        Player retrievedById = playerService.getPlayerById(addedPlayer.getUniqueId());

        assertSame(addedPlayer, retrievedByConn, "Player retrieved by connection should be the same instance.");
        assertSame(addedPlayer, retrievedById, "Player retrieved by ID should be the same instance.");
        assertEquals(1, playerService.getAllPlayers().size(), "Should be one player in the service.");
    }

    @Test
    void addPlayerWithSameConnectionShouldOverwritePlayerForConnectionIdButCreateNewPlayerObject() {
        Player player1 = playerService.addPlayer(mockConnection1);
        String player1UniqueId = player1.getUniqueId();

        Player player2 = playerService.addPlayer(mockConnection1);
        String player2UniqueId = player2.getUniqueId();

        assertNotSame(player1, player2, "Adding with same connection should create a new Player object.");
        assertNotEquals(player1UniqueId, player2UniqueId, "The new player object should have a different unique ID.");

        Player retrievedByConn = playerService.getPlayerByConnection(mockConnection1);
        assertSame(player2, retrievedByConn, "Player retrieved by connection should be the latest one added.");

        assertNotNull(playerService.getPlayerById(player1UniqueId), "Original player1 should still be in playersById map.");
        assertSame(player1, playerService.getPlayerById(player1UniqueId));
        assertNotNull(playerService.getPlayerById(player2UniqueId), "New player2 should be in playersById map.");
        assertSame(player2, playerService.getPlayerById(player2UniqueId));

        assertEquals(1, playerService.getAllPlayers().size(), "getAllPlayers should return 1 player (the one associated with mockConnId1).");
        assertTrue(playerService.getAllPlayers().contains(player2));
    }


    @Test
    void getPlayerByConnectionShouldReturnCorrectPlayerOrNull() {
        assertNull(playerService.getPlayerByConnection(mockConnection1), "Should return null if player not found.");

        Player addedPlayer = playerService.addPlayer(mockConnection1);
        Player retrievedPlayer = playerService.getPlayerByConnection(mockConnection1);
        assertSame(addedPlayer, retrievedPlayer, "Should return the added player.");
    }

    @Test
    void getPlayerByIdShouldReturnCorrectPlayerOrNull() {
        assertNull(playerService.getPlayerById("nonExistentId"), "Should return null if player ID not found.");

        Player addedPlayer = playerService.addPlayer(mockConnection1);
        Player retrievedPlayer = playerService.getPlayerById(addedPlayer.getUniqueId());
        assertSame(addedPlayer, retrievedPlayer, "Should return the added player by its unique ID.");
    }

    @Test
    void getAllPlayersShouldReturnAllPlayersFromConnectionMap() {
        assertTrue(playerService.getAllPlayers().isEmpty(), "Initially, getAllPlayers should return an empty list.");

        Player player1 = playerService.addPlayer(mockConnection1);
        Player player2 = playerService.addPlayer(mockConnection2);

        List<Player> allPlayers = playerService.getAllPlayers();
        assertEquals(2, allPlayers.size(), "Should return two players.");
        assertTrue(allPlayers.contains(player1), "List should contain player1.");
        assertTrue(allPlayers.contains(player2), "List should contain player2.");
    }

    @Test
    void removePlayerByConnectionIdShouldRemovePlayerFromBothMaps() {
        Player addedPlayer = playerService.addPlayer(mockConnection1);
        String uniqueId = addedPlayer.getUniqueId();

        assertNotNull(playerService.getPlayerByConnection(mockConnection1));
        assertNotNull(playerService.getPlayerById(uniqueId));
        assertEquals(1, playerService.getAllPlayers().size());

        playerService.removePlayerByConnectionId(mockConnection1);

        assertNull(playerService.getPlayerByConnection(mockConnection1), "Player should be removed from connection map.");
        assertNull(playerService.getPlayerById(uniqueId), "Player should be removed from ID map.");
        assertTrue(playerService.getAllPlayers().isEmpty(), "getAllPlayers should return an empty list after removal.");
    }

    @Test
    void removePlayerByConnectionIdForNonExistentPlayerShouldDoNothing() {
        playerService.addPlayer(mockConnection1);

        playerService.removePlayerByConnectionId(mockConnection2);

        assertEquals(1, playerService.getAllPlayers().size(), "Size should remain 1 if removing a non-existent player.");
        assertNotNull(playerService.getPlayerByConnection(mockConnection1), "Existing player should not be affected.");
    }

}