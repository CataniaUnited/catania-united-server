package com.example.cataniaunited.lobby;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.InvalidTurnException;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@QuarkusTest
class LobbyServiceImplTest {
    private static final Logger logger = Logger.getLogger(LobbyServiceImplTest.class);
    @InjectMock
    PlayerService playerService;
    @InjectSpy
    LobbyServiceImpl lobbyService;

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
                    "Lobby Id should be in the format abc123 or 123abc");
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
        String invalidLobbyCode = "InvalidCode";
        boolean joined = assertDoesNotThrow(() -> lobbyService.joinLobbyByCode(invalidLobbyCode, "New Player"));
        assertFalse(joined);
    }

    @Test
    void testJoinLobbyAssignsUniqueColor() throws GameException {
        String lobbyId = lobbyService.createLobby("HostPlayer");
        lobbyService.joinLobbyByCode(lobbyId, "Player1");

        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        assertTrue(lobby.getPlayers().contains("Player1"));

        PlayerColor color = lobby.getPlayerColor("Player1");

        assertNotNull(color);
        assertFalse(lobbyService.getOpenLobbies().isEmpty());
    }

    @Test
    void testJoinLobbyFailsWhenNoColorsAvailable() {
        String lobbyId = lobbyService.createLobby("HostPlayer");

        for (int i = 0; i < PlayerColor.values().length; i++) {
            lobbyService.joinLobbyByCode(lobbyId, "Player" + i);
        }

        boolean joined = lobbyService.joinLobbyByCode(lobbyId, "ExtraPlayer");
        assertFalse(joined, "Player should not be able to join when no colors are available");
    }

    @Test
    void testRemovePlayerFromLobby() throws GameException {
        String lobbyId = lobbyService.createLobby("HostPlayer");
        lobbyService.joinLobbyByCode(lobbyId, "Player1");

        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        assertTrue(lobby.getPlayers().contains("Player1"));

        lobbyService.removePlayerFromLobby(lobbyId, "Player1");
        assertFalse(lobby.getPlayers().contains("Player1"));
    }

    @Test
    void testRemovePlayerRestoresColor() throws GameException {
        String lobbyId = lobbyService.createLobby("HostPlayer");
        lobbyService.joinLobbyByCode(lobbyId, "Player1");

        int colorPoolSizeBefore = PlayerColor.values().length - 2;
        lobbyService.removePlayerFromLobby(lobbyId, "Player1");

        assertEquals(PlayerColor.values().length - 1, colorPoolSizeBefore + 1);
    }

    @Test
    void testRemovePlayerNotInLobby() throws GameException {
        String lobbyId = lobbyService.createLobby("HostPlayer");

        lobbyService.removePlayerFromLobby(lobbyId, "GhostPlayer");

        Lobby lobby = assertDoesNotThrow(() -> lobbyService.getLobbyById(lobbyId));
        assertFalse(lobby.getPlayers().contains("GhostPlayer"));
    }

    @Test
    void getPlayerColorShouldThrowExceptionIfPlayerHasNoColor() throws GameException {
        String lobbyId = "lobby1";
        String playerId = "Player1";
        Lobby lobbyMock = mock(Lobby.class);
        when(lobbyMock.getPlayerColor(playerId)).thenReturn(null);
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        GameException ge = assertThrows(GameException.class, () -> lobbyService.getPlayerColor(lobbyId, playerId));
        assertEquals("No color for player found: playerId=%s, lobbyId=%s".formatted(playerId, lobbyId), ge.getMessage());
    }

    @Test
    void checkPlayerTurnShouldThrowExceptionForNonExistingLobby() {
        String lobbyId = "NonExistingLobby";
        GameException ge = assertThrows(GameException.class, () -> {
            lobbyService.checkPlayerTurn(lobbyId, "NonExistingPlayer");
        });
        assertEquals("Lobby with id %s not found".formatted(lobbyId), ge.getMessage());
    }

    @Test
    void checkPlayerTurnShouldNotThrowExceptionForPlayerTurn() throws GameException {
        String playerId = "player1";
        String lobbyId = lobbyService.createLobby(playerId);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setActivePlayer(playerId);

        assertDoesNotThrow(() -> lobbyService.checkPlayerTurn(lobbyId, playerId));
    }

    @Test
    void checkPlayerTurnShouldThrowExceptionForNotPlayerTurn() throws GameException {
        String playerId = "player1";
        String lobbyId = lobbyService.createLobby(playerId);
        Lobby lobby = spy(lobbyService.getLobbyById(lobbyId));
        lobby.setActivePlayer("anotherPlayer");

        InvalidTurnException ite = assertThrows(InvalidTurnException.class, () -> {lobbyService.checkPlayerTurn(lobbyId, playerId);});
        assertEquals("It is not your turn!", ite.getMessage());
    }

    @Test
    void nextTurnShouldThrowExceptionForNonExistingLobby() {
        String invalidLobbyId = "invalidLobbyId";
        GameException ge = assertThrows(GameException.class, () -> lobbyService.nextTurn(invalidLobbyId, "Player1"));
        assertEquals("Lobby with id %s not found".formatted(invalidLobbyId), ge.getMessage());
    }

    @Test
    void nextTurnShouldThrowExceptionIfPlayerOrderOfLobbyIsNull() throws GameException {
        String playerId = "player1";
        String lobbyId = lobbyService.createLobby(playerId);
        Lobby lobby = spy(lobbyService.getLobbyById(lobbyId));
        lobby.setPlayerOrder(List.of());
        GameException ge = assertThrows(GameException.class, () -> lobbyService.nextTurn(lobbyId, playerId));
        assertEquals("Executing next turn failed", ge.getMessage());
    }

    @Test
    void nextTurnShouldThrowExceptionIfActivePlayerOfLobbyIsNull() throws GameException {
        String playerId = "player1";
        String lobbyId = lobbyService.createLobby(playerId);
        Lobby lobby = spy(lobbyService.getLobbyById(lobbyId));
        lobby.setActivePlayer(null);
        GameException ge = assertThrows(GameException.class, () -> lobbyService.nextTurn(lobbyId, playerId));
        assertEquals("Executing next turn failed", ge.getMessage());
    }

    @Test
    void nextTurnShouldReturnNextActivePlayer() throws GameException {
        String playerId = "player1";
        String playerId2 = "player2";
        String lobbyId = lobbyService.createLobby(playerId);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.addPlayer(playerId2);
        lobby.setPlayerOrder(List.of(playerId, playerId2));
        lobby.setActivePlayer(playerId);
        assertEquals(playerId, lobby.getActivePlayer());

        String nextActivePlayer = assertDoesNotThrow(() -> lobbyService.nextTurn(lobbyId, playerId));

        assertEquals(playerId2, nextActivePlayer);
        assertEquals(playerId2, lobby.getActivePlayer());
    }

    @Test
    void notifyPlayers_sendsMessageToEveryPlayerInTheLobby() {
        String hostId = "host";
        String lobbyId = lobbyService.createLobby(hostId);
        lobbyService.joinLobbyByCode(lobbyId, "p2");

        // mock the Player objects returned by PlayerService
        Player host = mock(Player.class);
        Player p2 = mock(Player.class);

        when(playerService.getPlayerById(hostId)).thenReturn(host);
        when(playerService.getPlayerById("p2")).thenReturn(p2);

        MessageDTO dto = new MessageDTO(MessageType.ERROR, (ObjectNode) null);


        lobbyService.notifyPlayers(lobbyId, dto);


        verify(host).sendMessage(dto);
        verify(p2).sendMessage(dto);
        verifyNoMoreInteractions(host, p2);
    }

    @Test
    void notifyPlayersShouldReturnFailedUniOnException() {
        String invalidLobbyId = "invalidLobbyId";
        Uni<MessageDTO> failedUni = assertDoesNotThrow(() -> lobbyService.notifyPlayers(invalidLobbyId, new MessageDTO(MessageType.JOIN_LOBBY, (ObjectNode) null)));
        failedUni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(GameException.class, "Lobby with id %s not found".formatted(invalidLobbyId))
                .assertSubscribed()
                .assertTerminated();
    }

    @Test
    void notifyPlayersShouldReturnFailedUniOnFailedMessageSend() {
        var exception = new RuntimeException("Test exception");
        String lobbyId = lobbyService.createLobby("HostPlayer");
        Player player = spy(new Player(("Player1")));
        when(player.sendMessage(any(MessageDTO.class))).thenReturn(Uni.createFrom().failure(exception));
        when(playerService.getPlayerById(player.getUniqueId())).thenReturn(player);
        lobbyService.joinLobbyByCode(lobbyId, player.getUniqueId());

        Uni<MessageDTO> failedUni = assertDoesNotThrow(() -> lobbyService.notifyPlayers(lobbyId, new MessageDTO(MessageType.JOIN_LOBBY, null)));
        failedUni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, exception.getMessage())
                .assertSubscribed()
                .assertTerminated();
    }
}
