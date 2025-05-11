package com.example.cataniaunited.lobby;

import com.example.cataniaunited.player.PlayerColor;
import org.junit.jupiter.api.Test;


import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;


class LobbyTest {

    @Test
    void getLobbyId_shouldReturnCorrectId() {
        String expectedLobbyId = "1";
        String hostPlayer = "Player 1";
        Lobby lobby = new Lobby(expectedLobbyId, hostPlayer);

        String actualLobbyId = lobby.getLobbyId();

        assertEquals(expectedLobbyId, actualLobbyId);
    }

    @Test
    void getPlayers_shouldReturnHostPlayer() {
        String lobbyId = "1";
        String hostPlayer = "Player 1";
        Lobby lobby = new Lobby(lobbyId, hostPlayer);

        Set<String> players = lobby.getPlayers();

        assertEquals(1, players.size());
        assertTrue(players.contains(hostPlayer));
    }

    @Test
    void testRestoreColor_shouldOnlyAddColorIfNotPresent() {
        Lobby lobby = new Lobby("555xyz", "HostPlayer");

        PlayerColor color = lobby.assignAvailableColor();
        assertNotNull(color);

        lobby.restoreColor(color);

        int countBefore = (int) lobby.getAvailableColors().stream().filter(c -> c == color).count();
        lobby.restoreColor(color);
        int countAfter = (int) lobby.getAvailableColors().stream().filter(c -> c == color).count();

        assertEquals(1, countAfter);
        assertEquals(countBefore, countAfter);
    }

    @Test
    void isPlayerTurnShouldReturnTrueForPlayerTurn() {
        String playerId = "player1";
        String lobbyId = "555xyz";
        Lobby lobby = new Lobby(lobbyId, playerId);
        lobby.setActivePlayer(playerId);
        assertTrue(lobby.isPlayerTurn(playerId));
    }

    @Test
    void isPlayerTurnShouldReturnFalseForNotPlayerTurn() {
        String playerId = "player1";
        String lobbyId = "555xyz";
        Lobby lobby = new Lobby(lobbyId, playerId);
        lobby.setActivePlayer("anotherPlayer");
        assertFalse(lobby.isPlayerTurn(playerId));
    }

    @Test
    void isPlayerTurnShouldReturnFalseIfPlayerIsNull() {
        String playerId = "player1";
        String lobbyId = "555xyz";
        Lobby lobby = new Lobby(lobbyId, playerId);
        lobby.setActivePlayer(playerId);
        assertFalse(lobby.isPlayerTurn(null));
    }

    @Test
    void getActivePlayerShouldReturnPlayer() {
        String playerId = "player1";
        String lobbyId = "555xyz";
        Lobby lobby = new Lobby(lobbyId, playerId);
        lobby.setActivePlayer(playerId);

        assertEquals(playerId, lobby.getActivePlayer());
    }

    private Lobby lobby;   // reused by the 3 new tests

    @BeforeEach
    void setUpExtraLobby() {
        lobby = new Lobby("L-extra", "host");
        lobby.addPlayer("p2");
        lobby.addPlayer("p3");
    }

    @Test
    void nextPlayerTurn_wrapsAroundToFirstPlayer() {
        lobby.setActivePlayer("host");


        lobby.nextPlayerTurn();
        assertEquals("p2", lobby.getActivePlayer());


        lobby.nextPlayerTurn();
        assertEquals("p3", lobby.getActivePlayer());


        lobby.nextPlayerTurn();
        assertEquals("host", lobby.getActivePlayer());
    }
    @Test
    void randomizePlayerOrder_changesOrderButKeepsSameElements() {
        List<String> before = new ArrayList<>(lobby.getPlayers());   // ← convert

        lobby.randomizePlayerOrder();

        List<String> after  = new ArrayList<>(lobby.getPlayers());   // ← convert
        assertEquals(before.size(), after.size());
        assertTrue(after.containsAll(before));
        assertNotEquals(before, after, "Order should be shuffled");
    }

    @Test
    void canStartGame_requiresTwoPlayersAndGameNotYetStarted() {
        assertTrue(lobby.canStartGame("host"), "should start with ≥2 players");

        lobby.setGameStarted(true);
        assertFalse(lobby.canStartGame("host"), "already started ⇒ false");
    }

    @Test
    void testResetForNewGame_resetsActivePlayerAndStartedFlag() {
        lobby.setActivePlayer("p2");
        lobby.setGameStarted(true);

        lobby.resetForNewGame();

        assertFalse(lobby.isGameStarted(), "gameStarted should be reset to false");
        assertNull(lobby.getActivePlayer(),   "activePlayer should be reset to null");
    }

    @Test
    void testSetPlayerOrder_overwritesExistingOrder() {
        List<String> customOrder = List.of("alice", "bob", "carol");

        lobby.setPlayerOrder(customOrder);

        List<String> playersList = new ArrayList<>(lobby.getPlayers());
        assertEquals(customOrder, playersList, "setPlayerOrder should replace the entire players list");
    }

    @Test
    void testAssignAvailableColor_removesColorAndRestoreAddsBack() {
        Lobby colorLobby = new Lobby("L-color", "host");
        int before = colorLobby.getAvailableColors().size();

        PlayerColor picked = colorLobby.assignAvailableColor();
        assertNotNull(picked);
        assertEquals(before - 1, colorLobby.getAvailableColors().size());

        colorLobby.restoreColor(picked);
        assertTrue(colorLobby.getAvailableColors().contains(picked));
        assertEquals(before, colorLobby.getAvailableColors().size());
    }

    @Test
    void testSetGameStarted_flagToggles() {
        Lobby fLobby = new Lobby("L-flag", "host");
        assertFalse(fLobby.isGameStarted(), "should start false");

        fLobby.setGameStarted(true);
        assertTrue(fLobby.isGameStarted(),  "should now be true");

        fLobby.setGameStarted(false);
        assertFalse(fLobby.isGameStarted(), "can turn back off");
    }

    @Test
    void canStartGame_returnsFalseForNonHostEvenWithEnoughPlayers() {
        assertFalse(lobby.canStartGame("p2"), "only the host may start the game");
        assertFalse(lobby.canStartGame("p3"), "only the host may start the game");
    }

    @Test
    void assignAvailableColor_exhaustsToNullThenRestores() {
        Lobby colorLobby = new Lobby("L-col", "host");

        int totalColors = colorLobby.getAvailableColors().size();
        for (int i = 0; i < totalColors; i++) {
            assertNotNull(colorLobby.assignAvailableColor(), "should still have colors");
        }
        assertNull(colorLobby.assignAvailableColor(), "no colors left → should be null");

        PlayerColor comeback = PlayerColor.RED;
        colorLobby.restoreColor(comeback);

        PlayerColor got = colorLobby.assignAvailableColor();
        assertEquals(comeback, got, "restored color must come back immediately");
    }
}
