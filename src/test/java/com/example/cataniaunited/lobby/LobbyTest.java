package com.example.cataniaunited.lobby;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyTest {

    Lobby testLobby;

    @BeforeEach
    void setUpExtraLobby() {
        testLobby = new Lobby("L-extra", "host");
        testLobby.addPlayer("p2");
        testLobby.addPlayer("p3");
    }

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

    @Test
    void nextPlayerShouldThrowExceptionIfPlayerOrderIsEmpty() {
        testLobby.setPlayerOrder(List.of());
        GameException ge = assertThrows(GameException.class, () -> testLobby.nextPlayerTurn());
        assertEquals("Executing next turn failed", ge.getMessage());
    }

    @Test
    void nextPlayerShouldThrowExceptionIfActivePlayerIsNull() {
        testLobby.setActivePlayer(null);
        GameException ge = assertThrows(GameException.class, () -> testLobby.nextPlayerTurn());
        assertEquals("Executing next turn failed", ge.getMessage());
    }

    @Test
    void testNextPlayerTurnForThreeOrMorePlayers() throws GameException {
        List<String> playerOrder = List.of("p2", "host", "p3");
        testLobby.setPlayerOrder(playerOrder);
        testLobby.setActivePlayer("p2");
        assertEquals(0, testLobby.getRoundsPlayed());

        //First round in order
        testLobby.nextPlayerTurn();
        assertEquals(0, testLobby.getRoundsPlayed());
        assertEquals("host", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(0, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        //Second round in reverse order
        testLobby.nextPlayerTurn();
        assertEquals(1, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(1, testLobby.getRoundsPlayed());
        assertEquals("host", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(1, testLobby.getRoundsPlayed());
        assertEquals("p2", testLobby.getActivePlayer());

        //Third and subsequent rounds in order again
        testLobby.nextPlayerTurn();
        assertEquals(2, testLobby.getRoundsPlayed());
        assertEquals("host", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(2, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(3, testLobby.getRoundsPlayed());
        assertEquals("p2", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(3, testLobby.getRoundsPlayed());
        assertEquals("host", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(3, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(4, testLobby.getRoundsPlayed());
        assertEquals("p2", testLobby.getActivePlayer());
    }

    @Test
    void testNextPlayerTurnForTwoPlayers() throws GameException {
        List<String> playerOrder = List.of("p2", "p3");
        testLobby.setPlayerOrder(playerOrder);
        testLobby.setActivePlayer("p2");
        assertEquals(0, testLobby.getRoundsPlayed());

        //First round in order
        testLobby.nextPlayerTurn();
        assertEquals(0, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        //Second round reversed
        testLobby.nextPlayerTurn();
        assertEquals(1, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(1, testLobby.getRoundsPlayed());
        assertEquals("p2", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(2, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals("p2", testLobby.getActivePlayer());
        assertEquals(3, testLobby.getRoundsPlayed());

        testLobby.nextPlayerTurn();
        assertEquals(3, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(4, testLobby.getRoundsPlayed());
        assertEquals("p2", testLobby.getActivePlayer());

        testLobby.nextPlayerTurn();
        assertEquals(4, testLobby.getRoundsPlayed());
        assertEquals("p3", testLobby.getActivePlayer());

    }

    @Test
    void startGameChangesOrderButKeepsSameElements() {
        assertTrue(testLobby.getPlayerOrder().isEmpty());
        assertEquals(3, testLobby.getPlayers().size());
        testLobby.startGame();
        assertEquals(testLobby.getPlayers().size(), testLobby.getPlayerOrder().size());
        assertTrue(testLobby.getPlayerOrder().containsAll(testLobby.getPlayers()));
    }

    @Test
    void canStartGame_requiresTwoPlayersAndGameNotYetStarted() {
        // host can start when ≥2 players
        testLobby.toggleReady("host");
        testLobby.toggleReady("p2");
        testLobby.toggleReady("p3");
        assertTrue(testLobby.canStartGame("host"));

        // once started, cannot start again
        testLobby.setGameStarted(true);
        assertFalse(testLobby.canStartGame("host"));
    }

    @Test
    void canStartGame_doesRequireTwoPlayers() {
        testLobby.removePlayer("p2");
        testLobby.removePlayer("p3");
        testLobby.toggleReady("host");

        assertFalse(testLobby.canStartGame("host"));
    }

    @Test
    void canStartGame_returnsFalseForNonHostEvenWithEnoughPlayers() {
        assertFalse(testLobby.canStartGame("p2"), "only the host may start the game");
        assertFalse(testLobby.canStartGame("p3"), "only the host may start the game");
    }

    @Test
    void testResetForNewGame_resetsActivePlayerAndStartedFlag() {
        testLobby.setActivePlayer("p2");
        testLobby.setGameStarted(true);

        testLobby.resetForNewGame();

        assertFalse(testLobby.isGameStarted(), "gameStarted should be reset to false");
        assertNull(testLobby.getActivePlayer(), "activePlayer should be reset to null");
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