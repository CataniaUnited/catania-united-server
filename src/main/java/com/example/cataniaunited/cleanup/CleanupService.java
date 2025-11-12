package com.example.cataniaunited.cleanup;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class CleanupService {

    @Inject
    LobbyService lobbyService;

    @Inject
    GameService gameService;

    /**
     * Job that removes all lobbies which are older than 2 days
     */
    @Scheduled(every = "24h")
    void cleanupOldLobbies() {
        Log.debugf("Starting cleanup job for old lobbies");
        Instant twoDaysOld = Instant.now().minus(2, ChronoUnit.DAYS);
        lobbyService.getOpenLobbies().stream()
                .filter(lobby -> lobby.getCreatedAt().isBefore(twoDaysOld))
                .forEach(this::cleanupLobby);
    }

    void cleanupLobby(Lobby lobby) {
        String lobbyId = lobby.getLobbyId();
        Log.debugf("Starting cleanup of lobby %s", lobbyId);
        gameService.removeGameBoardForLobby(lobbyId);
        lobby.getPlayers().forEach(playerId -> {
            try {
                lobbyService.removePlayerFromLobby(lobbyId, playerId);
            } catch (GameException e) {
                Log.warnf(e, "Error while removing player %s from lobby %s during cleanup", playerId, lobbyId);
            }
        });
    }

}
