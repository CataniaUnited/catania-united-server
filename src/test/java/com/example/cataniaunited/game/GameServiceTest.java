package com.example.cataniaunited.game;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.PlayerService;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class GameServiceTest {

    @InjectSpy
    GameService gameService;

    @InjectSpy
    LobbyService lobbyService;

    @InjectSpy
    PlayerService playerService;

    GameBoard gameboardMock;

    Lobby lobbyMock;

    @BeforeEach
    void init() {
        gameboardMock = mock(GameBoard.class);
        lobbyMock = mock(Lobby.class);
        when(lobbyMock.getLobbyId()).thenReturn("12345");
        when(lobbyMock.getPlayers()).thenReturn(Set.of("1", "2"));
    }

    @Test
    void testCreateGameBoard() throws GameException {
        doReturn(lobbyMock).when(lobbyService).getLobbyById(anyString());
        GameBoard gameBoard = gameService.createGameboard(lobbyMock.getLobbyId());
        assertNotNull(gameBoard);
        verify(lobbyService).getLobbyById(anyString());
        verify(gameService).addGameboardToList(lobbyMock.getLobbyId(), gameBoard);
    }

    @Test
    void createGameBoardShouldThrowGameException() {
        GameException ge = assertThrows(GameException.class, () -> gameService.createGameboard(lobbyMock.getLobbyId()));
        assertEquals("Lobby with id %s not found".formatted(lobbyMock.getLobbyId()), ge.getMessage());
    }

    @Test
    void getGameboardByLobbyIdShouldThrowGameExceptionForNonExistingLobby() {
        String invalidLobbyId = "invalidLobbyId";
        GameException ge = assertThrows(GameException.class, () -> gameService.getGameboardByLobbyId(invalidLobbyId));
        assertEquals("Gameboard for Lobby not found: id = %s".formatted(invalidLobbyId), ge.getMessage());
    }

    @Test
    void getGameBoardShouldReturnCorrectGameBoard() throws GameException {
        doReturn(lobbyMock).when(lobbyService).getLobbyById(anyString());
        GameBoard expectedGameBoard = gameService.createGameboard(lobbyMock.getLobbyId());
        assertNotNull(expectedGameBoard);

        GameBoard actualGameBoard = gameService.getGameboardByLobbyId(lobbyMock.getLobbyId());
        assertNotNull(actualGameBoard);
        assertEquals(expectedGameBoard, actualGameBoard);
        verify(lobbyService).getLobbyById(lobbyMock.getLobbyId());
        verify(gameService).addGameboardToList(lobbyMock.getLobbyId(), expectedGameBoard);
    }

    @Test
    void placeSettlementShouldThrowGameExceptionForNonExistingLobby() throws GameException {
        String invalidLobbyId = "invalidLobbyId";
        GameException ge = assertThrows(GameException.class, () -> gameService.placeSettlement(invalidLobbyId, "1", 1));
        assertEquals("Lobby with id %s not found".formatted(invalidLobbyId), ge.getMessage());
        verify(gameService, never()).getGameboardByLobbyId(invalidLobbyId);
    }

    @Test
    void placeRoadShouldThrowGameExceptionForNonExistingLobby() throws GameException {
        String invalidLobbyId = "invalidLobbyId";
        GameException ge = assertThrows(GameException.class, () -> gameService.placeRoad(invalidLobbyId, "1", 1));
        assertEquals("Lobby with id %s not found".formatted(invalidLobbyId), ge.getMessage());
        verify(gameService, never()).getGameboardByLobbyId(invalidLobbyId);
    }

    @Test
    void placeSettlementShouldThrowGameExceptionForNotPlayerTurn() throws GameException {
        String playerId = "playerId1";
        String lobbyId = lobbyMock.getLobbyId();
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(false).when(lobbyMock).isPlayerTurn(playerId);
        GameException ge = assertThrows(GameException.class, () -> gameService.placeSettlement(lobbyId, playerId, 1));
        assertEquals("It is not the players turn: playerId=%s, lobbyId=%s".formatted(playerId, lobbyId), ge.getMessage());
        verify(gameService, never()).getGameboardByLobbyId(lobbyId);
    }

    @Test
    void placeRoadShouldThrowGameExceptionForNotPlayerTurn() throws GameException {
        String playerId = "playerId1";
        String lobbyId = lobbyMock.getLobbyId();
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(false).when(lobbyMock).isPlayerTurn(playerId);
        GameException ge = assertThrows(GameException.class, () -> gameService.placeRoad(lobbyId, playerId, 1));
        assertEquals("It is not the players turn: playerId=%s, lobbyId=%s".formatted(playerId, lobbyId), ge.getMessage());
        verify(gameService, never()).getGameboardByLobbyId(lobbyId);
    }

    @Test
    void setSettlementShouldCallPlaceSettlementOnGameboard() throws GameException {
        String playerId = "playerId1";
        int settlementPositionId = 15;
        String lobbyId = lobbyMock.getLobbyId();
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.BLUE).when(lobbyMock).getPlayerColor(playerId);
        doReturn(gameboardMock).when(gameService).getGameboardByLobbyId(lobbyId);
        gameService.placeSettlement(lobbyId, playerId, settlementPositionId);
        verify(gameboardMock).placeSettlement(playerId, PlayerColor.BLUE, settlementPositionId);
    }

    @Test
    void placeSettlementShouldAddVictoryPointForPlayer() throws GameException {
        String playerId = "player1";
        int settlementPositionId = 5;
        String lobbyId = lobbyMock.getLobbyId();

        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.BLUE).when(lobbyMock).getPlayerColor(playerId);

        gameService.addGameboardToList(lobbyId, gameboardMock);
        doNothing().when(gameboardMock).placeSettlement(playerId, PlayerColor.BLUE, settlementPositionId);
        gameService.placeSettlement(lobbyId, playerId, settlementPositionId);

        verify(gameboardMock).placeSettlement(playerId, PlayerColor.BLUE, settlementPositionId);
        verify(playerService).addVictoryPoints(playerId, 1);
    }

    @Test
    void setRoadShouldCallPlaceRoadOnGameboard() throws GameException {
        String playerId = "playerId1";
        int settlementPositionId = 15;
        String lobbyId = lobbyMock.getLobbyId();
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.BLUE).when(lobbyMock).getPlayerColor(playerId);
        doReturn(gameboardMock).when(gameService).getGameboardByLobbyId(lobbyId);
        gameService.placeRoad(lobbyId, playerId, settlementPositionId);
        verify(gameboardMock).placeRoad(playerId, PlayerColor.BLUE, settlementPositionId);
    }

    @Test
    void testGetJsonByValidLobbyId() throws GameException {
        String lobbyId = lobbyMock.getLobbyId(); // Use the ID from the mock setup
        ObjectNode expectedJson = new ObjectMapper().createObjectNode().put("test", "data"); // Create a dummy JSON node
        when(gameboardMock.getJson()).thenReturn(expectedJson);
        doReturn(gameboardMock).when(gameService).getGameboardByLobbyId(lobbyId);
        ObjectNode actualJson = gameService.getGameboardJsonByLobbyId(lobbyId);
        verify(gameService).getGameboardByLobbyId(lobbyId);
        verify(gameboardMock).getJson();
        assertNotNull(actualJson);
        assertSame(expectedJson, actualJson, "The returned JSON should be the one from the GameBoard");
    }

    @Test
    void testGetJsonByInvalidLobbyId() throws GameException {
        String invalidLobbyId = "nonExistentLobby";
        String expectedErrorMessage = "Gameboard for Lobby not found: id = %s".formatted(invalidLobbyId);
        doThrow(new GameException(expectedErrorMessage))
                .when(gameService).getGameboardByLobbyId(invalidLobbyId);
        GameException exception = assertThrows(GameException.class, () -> {
            gameService.getGameboardJsonByLobbyId(invalidLobbyId);
        }, "Should throw GameException when gameboard is not found");
        assertEquals(expectedErrorMessage, exception.getMessage());
        verify(gameService).getGameboardByLobbyId(invalidLobbyId);
    }

    @Test
    void broadcastWinShouldSendCorrectGameWonMessage() {
        String lobbyId = "lobby123";
        String winnerPlayerId = "playerABC";

        WebSocketConnection mockConnection = mock(WebSocketConnection.class, RETURNS_DEEP_STUBS);
        when(mockConnection.broadcast().sendText(any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<MessageDTO> resultUni = gameService.broadcastWin(mockConnection, lobbyId, winnerPlayerId);
        MessageDTO result = resultUni.await().indefinitely();

        assertNotNull(result);
        assertEquals(MessageType.GAME_WON, result.getType());
        assertEquals(lobbyId, result.getLobbyId());
        assertEquals(winnerPlayerId, result.getPlayer());
        assertEquals(winnerPlayerId, result.getMessageNode("winner").asText());

        verify(mockConnection.broadcast()).sendText(any(MessageDTO.class));
    }

}
