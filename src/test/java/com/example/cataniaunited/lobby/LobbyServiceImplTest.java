package com.example.cataniaunited.lobby;

import com.example.cataniaunited.exception.GameException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LobbyServiceImplTest {

    @Inject
    LobbyServiceImpl lobbyService;

    private static final Logger logger = Logger.getLogger(LobbyServiceImplTest.class);

    @BeforeEach
    void setUp() {
        lobbyService.clearLobbies();
    }

    @Test
    void testCreateLobby() {
        String lobbyId = lobbyService.createLobby("Player 1");
        assertNotNull(lobbyId, "Lobby ID should not be null");
        logger.infof("Created Lobby ID: %s", lobbyId);
    }

    @Test
    void testGetOpenLobbies() {
        String lobbyId1 = lobbyService.createLobby("Player 1");
        String lobbyId2 = lobbyService.createLobby("Player 2");

        List<String> openLobbies = lobbyService.getOpenLobbies();

        assertEquals(2, openLobbies.size(), "There should be 2 open lobbies");
        assertTrue(openLobbies.contains(lobbyId1));
        assertTrue(openLobbies.contains(lobbyId2));

        logger.infof("Open lobbies: %s", openLobbies);
    }

    @Test
    void testGeneratedLobbyIdFormat() {
        for (int i = 0; i < 100; i++) {
            String lobbyId = lobbyService.createLobby("Player" + i);
            assertEquals(6, lobbyId.length(), "Lobby ID should be exactly 6 characters long");
            assertTrue(lobbyId.matches("[a-z]{3}\\d{3}") || lobbyId.matches("\\d{3}[a-z]{3}"),
                    "Lobby ID should be in the format abc123 or 123abc");
        }
    }

    @Test
    void testNoDuplicateLobbyIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String id = lobbyService.createLobby("Player" + i);
            assertFalse(ids.contains(id), "Duplicate Lobby ID found: " + id);
            ids.add(id);
        }
    }

    @Test
    void testJoinLobbyByValidCode() throws GameException {
        String hostPlayer = "HostPlayer";
        String joiningPlayer = "NewPlayer";

        String lobbyId = lobbyService.createLobby(hostPlayer);
        boolean joined = lobbyService.joinLobbyByCode(lobbyId, joiningPlayer);

        assertTrue(joined, "Player should be able to join the lobby with a valid code");

        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        assertNotNull(lobby, "Lobby should exist");
        assertTrue(lobby.getPlayers().contains(joiningPlayer),
                "The joining player should be in the lobby's player list.");

        lobbyService.clearLobbies();
        List<String> openLobbies = lobbyService.getOpenLobbies();
        assertTrue(openLobbies.isEmpty(), "All lobbies should be cleared after the test");
    }

    @Test
    void testJoinLobbyByInvalidCode() {
        boolean joined = lobbyService.joinLobbyByCode("InvalidCode", "New Player");
        assertFalse(joined, "Player should not be able to join the lobby with an invalid code");
    }

    // New tests for startGame logic

    @Test
    void testStartGameShouldShufflePlayersAndStoreTurnOrder() throws GameException {
        // Arrange: create a lobby and add three players
        String lobbyId = lobbyService.createLobby("Alice");
        lobbyService.joinLobbyByCode(lobbyId, "Bob");
        lobbyService.joinLobbyByCode(lobbyId, "Carol");

        // Precondition: turnOrder is not set yet
        Lobby preGameLobby = lobbyService.getLobbyById(lobbyId);
        assertNull(preGameLobby.getTurnOrder(), "Turn order should be null before starting the game");

        // Act: start the game (shuffle and store)
        lobbyService.startGame(lobbyId);

        // Assert: turnOrder is set and contains exactly the same players
        Lobby postGameLobby = lobbyService.getLobbyById(lobbyId);
        List<String> turnOrder = postGameLobby.getTurnOrder();
        assertNotNull(turnOrder, "Turn order should not be null after starting the game");
        assertEquals(3, turnOrder.size(), "Turn order should contain all three players");
        assertTrue(turnOrder.containsAll(Set.of("Alice", "Bob", "Carol")),
                "Turn order must include Alice, Bob, and Carol");
    }

    @Test
    void testStartGameOnNonExistentLobbyShouldThrow() {
        String invalidId = "nonexistent";
        GameException ex = assertThrows(GameException.class,
                () -> lobbyService.startGame(invalidId),
                "Starting a game on a non-existent lobby should throw GameException"
        );
        assertTrue(ex.getMessage().contains(invalidId),
                "Exception message should mention the invalid lobby ID");
    }
}
