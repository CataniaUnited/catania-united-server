package com.example.cataniaunited.cleanup;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class CleanupServiceTest {


    @InjectSpy
    CleanupService cleanupService;

    @InjectSpy
    LobbyService lobbyService;

    @InjectSpy
    GameService gameService;

    @Test
    public void testCleanup() throws GameException {
        Player player = new Player("Player1");
        Player player2 = new Player("Player2");
        String lobbyId = "lob001";
        Lobby lobby = spy(new Lobby(lobbyId, player.getUniqueId()));
        doReturn(Instant.now().minus(49, ChronoUnit.HOURS)).when(lobby).getCreatedAt();
        doReturn(List.of(lobby)).when(lobbyService).getOpenLobbies();
        doReturn(lobby).when(lobbyService).getLobbyById(lobbyId);
        lobbyService.joinLobbyByCode(lobbyId, player2.getUniqueId());
        gameService.createGameboard(lobbyId);

        assertNotNull(gameService.getGameboardByLobbyId(lobbyId));
        assertEquals(2, lobby.getPlayers().size());

        cleanupService.cleanupOldLobbies();

        assertThrows(GameException.class, () -> gameService.getGameboardByLobbyId(lobbyId));
        assertEquals(0, lobby.getPlayers().size());

        verify(cleanupService).cleanupLobby(lobby);
        verify(lobbyService).getOpenLobbies();
        verify(lobbyService).removePlayerFromLobby(lobbyId, player.getUniqueId());
        verify(gameService).removeGameBoardForLobby(lobbyId);
    }

    @Test
    void cleanupShouldWorkIfNoLobbiesExist() {
        assertDoesNotThrow(() -> cleanupService.cleanupOldLobbies());
    }

    @Test
    void cleanupShouldNotRemoveNewLobby() throws GameException {
        Player player = new Player("Player1");
        Player player2 = new Player("Player2");
        String lobbyId = lobbyService.createLobby(player.getUniqueId());
        lobbyService.joinLobbyByCode(lobbyId, player2.getUniqueId());
        gameService.createGameboard(lobbyId);

        assertNotNull(gameService.getGameboardByLobbyId(lobbyId));
        assertEquals(2, lobbyService.getLobbyById(lobbyId).getPlayers().size());

        cleanupService.cleanupOldLobbies();

        assertNotNull(gameService.getGameboardByLobbyId(lobbyId));
        assertEquals(2, lobbyService.getLobbyById(lobbyId).getPlayers().size());

        verify(cleanupService, never()).cleanupLobby(any(Lobby.class));
    }

}
