package com.example.cataniaunited.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.quarkus.websockets.next.WebSocketConnection;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerServiceTest {

    PlayerService playerService;

    WebSocketConnection connection;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService();
        connection = mock(WebSocketConnection.class);
        when(connection.id()).thenReturn("conn123");
    }

    @Test
    void addPlayerRegistersPlayerByConnectionAndId() {
        Player player = playerService.addPlayer(connection);
        assertEquals("conn123", player.toString().contains("conn123") ? "conn123" : null);
        assertSame(player, playerService.getPlayerByConnection(connection));
        assertSame(player, playerService.getPlayerById(player.getUniqueId()));
    }

    @Test
    void removePlayerRemovesFromBothMaps() {
        Player player = playerService.addPlayer(connection);
        playerService.removePlayer(connection);
        assertNull(playerService.getPlayerByConnection(connection));
        assertNull(playerService.getPlayerById(player.getUniqueId()));
    }

    @Test
    void removePlayerNonExistingConnection(){
        Player player = playerService.addPlayer(connection);
        playerService.removePlayer(connection);

        assertDoesNotThrow(() -> playerService.removePlayer(connection));

        assertNull(playerService.getPlayerByConnection(connection));
        assertNull(playerService.getPlayerById(player.getUniqueId()));
    }

    @Test
    void addVictoryPointsIncreasesPointsCorrectly() {
        Player player = playerService.addPlayer(connection);
        playerService.addVictoryPoints(player.getUniqueId(), 2);
        playerService.addVictoryPoints(player.getUniqueId(), 3);
        assertEquals(5, player.getVictoryPoints());
    }

    @Test
    void checkForWinReturnsFalseIfLessThanTenPoints() {
        Player player = playerService.addPlayer(connection);
        playerService.addVictoryPoints(player.getUniqueId(), 9);
        assertFalse(playerService.checkForWin(player.getUniqueId()));
    }

    @Test
    void checkForWinReturnsTrueIfTenPoints() {
        Player player = playerService.addPlayer(connection);
        playerService.addVictoryPoints(player.getUniqueId(), 10);
        assertTrue(playerService.checkForWin(player.getUniqueId()));
    }

    @Test
    void getAllPlayersReturnsListOfAllPlayers() {
        playerService.addPlayer(connection);
        List<Player> players = playerService.getAllPlayers();
        assertEquals(1, players.size());
    }


}

