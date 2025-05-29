package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.InvalidTurnException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
        when(lobbyMock.getPlayers()).thenReturn(Set.of("host", "p2"));
    }

    @Test
    void startGame_setsFlags() throws GameException {
        String hostId = "host";
        String lobbyId = lobbyService.createLobby(hostId);

        Lobby lobbySpy = spy(lobbyService.getLobbyById(lobbyId));

        Player host = mock(Player.class);
        Player p2 = mock(Player.class);
        when(playerService.getPlayerById(hostId)).thenReturn(host);
        when(playerService.getPlayerById("p2")).thenReturn(p2);
        lobbyService.joinLobbyByCode(lobbyId, "p2");

        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        assertFalse(lobby.isGameStarted());
        assertTrue(lobby.getPlayerOrder().isEmpty());
        assertNull(lobby.getActivePlayer());

        gameService.startGame(lobbyId, hostId);

        assertTrue(lobby.isGameStarted());
        assertFalse(lobby.getPlayerOrder().isEmpty());
        assertTrue(lobby.getPlayerOrder().containsAll(List.of(hostId, "p2")));
        assertNotNull(lobby.getActivePlayer());
        assertNotNull(gameService.getGameboardByLobbyId(lobbyId));
    }

    @Test
    void startGameShouldThrowExceptionIfGameIsAlreadyStarted() throws GameException {
        String playerId = "player";
        String lobbyId = lobbyService.createLobby(playerId);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setGameStarted(true);
        lobby.addPlayer(playerId);
        lobby.addPlayer("Player2");
        GameException ge = assertThrows(GameException.class, () -> gameService.startGame(lobbyId, playerId));
        assertEquals("Starting of game failed", ge.getMessage());
    }

    @Test
    void startGameShouldThrowExceptionIfPlayerCountIsSmallerThanTwo() {
        String playerId = "player";
        String lobbyId = lobbyService.createLobby(playerId);
        GameException ge = assertThrows(GameException.class, () -> gameService.startGame(lobbyId, "Player1"));
        assertEquals("Starting of game failed", ge.getMessage());
    }

    @Test
    void startGameShouldThrowExceptionIfRequestingPlayerIsNotHost() throws GameException {
        String playerId = "player";
        String notHostPlayerId = "notHostPlayer";
        String lobbyId = lobbyService.createLobby(playerId);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setGameStarted(false);
        lobby.addPlayer(playerId);
        lobby.addPlayer(notHostPlayerId);
        GameException ge = assertThrows(GameException.class, () -> gameService.startGame(lobbyId, notHostPlayerId));
        assertEquals("Starting of game failed", ge.getMessage());
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
        InvalidTurnException ite = assertThrows(InvalidTurnException.class, () -> gameService.placeSettlement(lobbyId, playerId, 1));
        assertEquals("It is not your turn!", ite.getMessage());
        verify(gameService, never()).getGameboardByLobbyId(lobbyId);
    }

    @Test
    void placeRoadShouldThrowGameExceptionForNotPlayerTurn() throws GameException {
        String playerId = "playerId1";
        String lobbyId = lobbyMock.getLobbyId();
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(false).when(lobbyMock).isPlayerTurn(playerId);
        InvalidTurnException ite = assertThrows(InvalidTurnException.class, () -> gameService.placeRoad(lobbyId, playerId, 1));
        assertEquals("It is not your turn!", ite.getMessage());
        verify(gameService, never()).getGameboardByLobbyId(lobbyId);
    }

    @Test
    void setSettlementShouldCallPlaceSettlementOnGameboard() throws GameException {
        String playerId = "playerId1";
        Player player = mock(Player.class);
        int settlementPositionId = 15;
        String lobbyId = lobbyMock.getLobbyId();
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.BLUE).when(lobbyMock).getPlayerColor(playerId);
        doReturn(gameboardMock).when(gameService).getGameboardByLobbyId(lobbyId);
        doReturn(player).when(playerService).getPlayerById(playerId);
        gameService.placeSettlement(lobbyId, playerId, settlementPositionId);
        verify(gameboardMock).placeSettlement(player, PlayerColor.BLUE, settlementPositionId);
    }

    @Test
    void placeSettlementShouldAddVictoryPointForPlayer() throws GameException {
        String playerId = "player1";
        Player player = mock(Player.class);
        int settlementPositionId = 5;
        String lobbyId = lobbyMock.getLobbyId();

        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.BLUE).when(lobbyMock).getPlayerColor(playerId);
        doReturn(player).when(playerService).getPlayerById(playerId);

        gameService.addGameboardToList(lobbyId, gameboardMock);
        doNothing().when(gameboardMock).placeSettlement(player, PlayerColor.BLUE, settlementPositionId);
        gameService.placeSettlement(lobbyId, playerId, settlementPositionId);

        verify(gameboardMock).placeSettlement(player, PlayerColor.BLUE, settlementPositionId);
        verify(playerService).addVictoryPoints(playerId, 1);
    }

    @Test
    void setRoadShouldCallPlaceRoadOnGameboard() throws GameException {
        Player player = new Player("player1");
        String playerId = player.getUniqueId();
        int settlementPositionId = 15;
        String lobbyId = lobbyMock.getLobbyId();
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.BLUE).when(lobbyMock).getPlayerColor(playerId);
        doReturn(gameboardMock).when(gameService).getGameboardByLobbyId(lobbyId);
        doReturn(player).when(playerService).getPlayerById(playerId);
        gameService.placeRoad(lobbyId, playerId, settlementPositionId);
        verify(gameboardMock).placeRoad(player, PlayerColor.BLUE, settlementPositionId);
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
        GameException exception = assertThrows(GameException.class, () -> gameService.getGameboardJsonByLobbyId(invalidLobbyId), "Should throw GameException when gameboard is not found");
        assertEquals(expectedErrorMessage, exception.getMessage());
        verify(gameService).getGameboardByLobbyId(invalidLobbyId);
    }

    @Test
    void placeSettlementWhenPositionHasPortShouldAddPortToPlayer() throws GameException {
        String playerId = "playerWithPort";
        Player mockPlayer = mock(Player.class);
        int settlementPositionId = 10;
        String lobbyId = lobbyMock.getLobbyId();
        Port mockPort = mock(Port.class);

        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.RED).when(lobbyMock).getPlayerColor(playerId);
        doReturn(mockPlayer).when(playerService).getPlayerById(playerId);
        doReturn(gameboardMock).when(gameService).getGameboardByLobbyId(lobbyId);

        when(gameboardMock.getPortOfBuildingSite(settlementPositionId)).thenReturn(mockPort);
        doNothing().when(gameboardMock).placeSettlement(any(Player.class), any(PlayerColor.class), anyInt());
        doNothing().when(playerService).addVictoryPoints(anyString(), anyInt());


        gameService.placeSettlement(lobbyId, playerId, settlementPositionId);

        verify(gameboardMock).placeSettlement(mockPlayer, PlayerColor.RED, settlementPositionId);
        verify(gameboardMock).getPortOfBuildingSite(settlementPositionId);
        verify(mockPlayer).addPort(mockPort);
        verify(playerService).addVictoryPoints(playerId, 1);
    }

    @Test
    void placeSettlementWhenPositionHasNoPortShouldNotAddPortToPlayer() throws GameException {
        String playerId = "playerWithoutPort";
        Player mockPlayer = mock(Player.class);
        int settlementPositionId = 12;
        String lobbyId = lobbyMock.getLobbyId();

        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        doReturn(true).when(lobbyMock).isPlayerTurn(playerId);
        doReturn(PlayerColor.GREEN).when(lobbyMock).getPlayerColor(playerId);
        doReturn(mockPlayer).when(playerService).getPlayerById(playerId);
        doReturn(gameboardMock).when(gameService).getGameboardByLobbyId(lobbyId);

        when(gameboardMock.getPortOfBuildingSite(settlementPositionId)).thenReturn(null);
        doNothing().when(gameboardMock).placeSettlement(any(Player.class), any(PlayerColor.class), anyInt());
        doNothing().when(playerService).addVictoryPoints(anyString(), anyInt());

        gameService.placeSettlement(lobbyId, playerId, settlementPositionId);

        verify(gameboardMock).placeSettlement(mockPlayer, PlayerColor.GREEN, settlementPositionId);
        verify(gameboardMock).getPortOfBuildingSite(settlementPositionId);
        verify(mockPlayer, never()).addPort(any(Port.class));
        verify(playerService).addVictoryPoints(playerId, 1);
    }

}
