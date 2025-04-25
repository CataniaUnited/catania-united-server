package com.example.cataniaunited.lobby;

import com.example.cataniaunited.player.PlayerColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;

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
    void testRestoreColor_shouldOnlyAddColorIfNotPresent(){
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
}
