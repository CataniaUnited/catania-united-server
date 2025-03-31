package com.example.cataniaunited.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

@QuarkusTest
class LobbyServiceImplTest {

    private static final Logger logger = Logger.getLogger(LobbyServiceImplTest.class);

    @Inject
    LobbyServiceImpl lobbyService;

    @Test
    void testCreateLobby() {
        String lobbyId = lobbyService.createLobby("Alice");

        assertNotNull(lobbyId, "Lobby ID should not be null");

        logger.infof("Created Lobby ID: %s", lobbyId);
    }

    @Test
    void testGetOpenLobbies() {
        String lobbyId1 = lobbyService.createLobby("Alice");
        String lobbyId2 = lobbyService.createLobby("Bob");

        List<String> openLobbies = lobbyService.getOpenLobbies();

        assertEquals(2, openLobbies.size(), "There should be 2 open lobbies");
        assertTrue(openLobbies.contains(lobbyId1));
        assertTrue(openLobbies.contains(lobbyId2));

        logger.infof("Open lobbies: %s", openLobbies);
    }
}