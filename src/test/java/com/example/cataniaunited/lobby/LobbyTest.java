package com.example.cataniaunited.lobby;

import com.example.cataniaunited.player.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        Lobby lobby = new Lobby("555xyz", playerId);
        lobby.setActivePlayer(playerId);

        assertTrue(lobby.isPlayerTurn(playerId));
    }

    @Test
    void isPlayerTurnShouldReturnFalseForNotPlayerTurn() {
        String playerId = "player1";
        Lobby lobby = new Lobby("555xyz", playerId);
        lobby.setActivePlayer("anotherPlayer");

        assertFalse(lobby.isPlayerTurn(playerId));
    }

    @Test
    void isPlayerTurnShouldReturnFalseIfPlayerIsNull() {
        Lobby lobby = new Lobby("555xyz", "player1");
        lobby.setActivePlayer("player1");

        assertFalse(lobby.isPlayerTurn(null));
    }

    @Test
    void getActivePlayerShouldReturnPlayer() {
        String playerId = "player1";
        Lobby lobby = new Lobby("555xyz", playerId);
        lobby.setActivePlayer(playerId);

        assertEquals(playerId, lobby.getActivePlayer());
    }


    private Lobby sut;

    @BeforeEach
    void setUpExtraLobby() {
        sut = new Lobby("L-extra", "host");
        sut.addPlayer("p2");
        sut.addPlayer("p3");
    }

    @Test
    void nextPlayerTurn_wrapsAroundToFirstPlayer() {
        sut.setActivePlayer("host");

        sut.nextPlayerTurn();
        assertEquals("p2", sut.getActivePlayer());

        sut.nextPlayerTurn();
        assertEquals("p3", sut.getActivePlayer());

        sut.nextPlayerTurn();
        assertEquals("host", sut.getActivePlayer());
    }

    @Test
    void randomizePlayerOrderKeepsSameElements() {
        List<String> before = new ArrayList<>(sut.getPlayers());

        sut.randomizePlayerOrder();

        List<String> after = new ArrayList<>(sut.getPlayers());
        assertEquals(before.size(), after.size());
        assertTrue(after.containsAll(before));
    }


    @Test
    void canStartGame_requiresTwoPlayersAndGameNotYetStarted() {
        // host can start when ≥2 players
        assertTrue(sut.canStartGame("host"));

        // once started, cannot start again
        sut.setGameStarted(true);
        assertFalse(sut.canStartGame("host"));
    }

    @Test
    void canStartGame_returnsFalseForNonHostEvenWithEnoughPlayers() {
        assertFalse(sut.canStartGame("p2"), "only the host may start the game");
        assertFalse(sut.canStartGame("p3"), "only the host may start the game");
    }

    @Test
    void testResetForNewGame_resetsActivePlayerAndStartedFlag() {
        sut.setActivePlayer("p2");
        sut.setGameStarted(true);

        sut.resetForNewGame();

        assertFalse(sut.isGameStarted(), "gameStarted should be reset to false");
        assertNull(sut.getActivePlayer(), "activePlayer should be reset to null");
    }

    @Test
    void testSetPlayerOrder_overwritesExistingOrder() {
        List<String> customOrder = List.of("alice", "bob", "carol");

        sut.setPlayerOrder(customOrder);

        List<String> playersList = new ArrayList<>(sut.getPlayers());
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
        assertTrue(fLobby.isGameStarted(), "should now be true");

        fLobby.setGameStarted(false);
        assertFalse(fLobby.isGameStarted(), "can turn back off");
    }

    @Test
    void assignAvailableColor_exhaustsToNullThenRestores() {
        Lobby colorLobby = new Lobby("L-col", "host");

        int totalColors = colorLobby.getAvailableColors().size();
        for (int i = 0; i < totalColors; i++) {
            assertNotNull(colorLobby.assignAvailableColor(), "should still have colors");
        }
        // now exhausted
        assertNull(colorLobby.assignAvailableColor(), "no colors left → should be null");

        // put one back
        PlayerColor comeback = PlayerColor.RED;
        colorLobby.restoreColor(comeback);

        PlayerColor got = colorLobby.assignAvailableColor();
        assertEquals(comeback, got, "restored color must come back immediately");
    }
}
//