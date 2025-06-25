package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.dto.PlayerInfo;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.InvalidTurnException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.ReportOutcome;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.trade.TradeRequest;
import com.example.cataniaunited.game.trade.TradingService;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.mapper.PlayerMapper;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;


@QuarkusTest
class GameMessageHandlerTest {

    @Inject
    GameMessageHandler gameMessageHandler;

    @InjectSpy
    PlayerService playerService;

    @InjectSpy
    LobbyService lobbyService;

    @InjectSpy
    PlayerMapper playerMapper;

    @InjectSpy
    TradingService tradingService;

    @InjectSpy
    GameService gameService;


    @Test
    void getLobbyPlayerInfoShouldNotMapNullValues() throws GameException {
        String lobbyId = "lobbyId";
        Lobby lobbyMock = mock(Lobby.class);
        when(lobbyMock.getPlayers()).thenReturn(Set.of("1", "2", "3", "4"));
        doReturn(lobbyMock).when(lobbyService).getLobbyById(lobbyId);
        when(playerService.getPlayerById(anyString())).thenReturn(null);

        var playerInfo = gameMessageHandler.getLobbyPlayerInformation(lobbyId);

        assertTrue(playerInfo.isEmpty());
    }

    @Test
    void getLobbyPlayerInfoShouldThrowExceptionForInvalidLobby() {
        GameException ge = assertThrows(GameException.class, () -> gameMessageHandler.getLobbyPlayerInformation("invalid"));
        assertEquals("Lobby with id %s not found".formatted("invalid"), ge.getMessage());
    }

    @Test
    void testGetLobbyPlayerInfo() throws GameException {
        WebSocketConnection connection = mock(WebSocketConnection.class);
        WebSocketConnection connection2 = mock(WebSocketConnection.class);
        when(connection.id()).thenReturn("1234");
        when(connection2.id()).thenReturn("5678");
        Player player = playerService.addPlayer(connection);
        Player player2 = playerService.addPlayer(connection2);
        String player1Username = "Player 1";
        playerService.setUsername(player.getUniqueId(), player1Username);
        String player2Username = "Player 2";
        playerService.setUsername(player2.getUniqueId(), player2Username);

        player.addVictoryPoints(6);
        player2.addVictoryPoints(8);
        player2.receiveResource(TileType.ORE, 3);
        player2.receiveResource(TileType.WOOD, 4);
        String lobbyId = lobbyService.createLobby(player.getUniqueId());
        lobbyService.joinLobbyByCode(lobbyId, player2.getUniqueId());
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        PlayerColor playerColor = lobby.getPlayerColor(player.getUniqueId());
        PlayerColor player2Color = lobby.getPlayerColor(player2.getUniqueId());

        var playerInfo = gameMessageHandler.getLobbyPlayerInformation(lobbyId);

        assertTrue(playerInfo.containsKey(player.getUniqueId()));
        assertTrue(playerInfo.containsKey(player2.getUniqueId()));

        var player1Info = playerInfo.get(player.getUniqueId());
        var player2Info = playerInfo.get(player2.getUniqueId());

        assertEquals(player1Username, player1Info.username());
        assertEquals(player.getResources(), player1Info.resources());
        assertEquals(playerColor.getHexCode(), player1Info.color());
        assertEquals(6, player1Info.victoryPoints());
        assertTrue(player1Info.isHost());

        assertEquals(player2Username, player2Info.username());
        assertEquals(player2.getResources(), player2Info.resources());
        assertEquals(player2Color.getHexCode(), player2Info.color());
        assertEquals(8, player2Info.victoryPoints());
        assertFalse(player2Info.isHost());
    }


    @Test
    void handleTradeWithBankInvocedFromMainSwitchCaseSuccess() throws GameException {
        WebSocketConnection connection = mock(WebSocketConnection.class);
        WebSocketConnection connection2 = mock(WebSocketConnection.class);
        when(connection.id()).thenReturn("1234");
        when(connection2.id()).thenReturn("5678");
        Player player = playerService.addPlayer(connection);
        Player player2 = playerService.addPlayer(connection2);
        String player1Id = player.getUniqueId();
        String lobbyId = lobbyService.createLobby(player1Id);
        lobbyService.joinLobbyByCode(lobbyId, player2.getUniqueId());

        // Initial state for the player
        player.getResources().clear();
        player.getResources().put(TileType.WOOD, 4);
        player.getResources().put(TileType.SHEEP, 0);


        // Prepare trade request: 4 WOOD for 1 SHEEP
        ObjectNode tradePayload = JsonNodeFactory.instance.objectNode();
        tradePayload.putObject("offeredResources").put(TileType.WOOD.name(), 4);
        tradePayload.putObject("targetResources").put(TileType.SHEEP.name(), 1);

        MessageDTO inputMessage = new MessageDTO(MessageType.TRADE_WITH_BANK, player1Id, lobbyId, tradePayload);

        // Mock LobbyService to pass checkTurn
        doNothing().when(lobbyService).checkPlayerTurn(lobbyId, player1Id);
        Lobby mockLobby = mock(Lobby.class);
        when(lobbyService.getLobbyById(lobbyId)).thenReturn(mockLobby);

        gameMessageHandler.handleGameMessage(inputMessage);

        TradeRequest expectedRequest = new TradeRequest(
                Map.of(TileType.WOOD, 4),
                Map.of(TileType.SHEEP, 1)
        );

        verify(lobbyService).checkPlayerTurn(lobbyId, player1Id);
        verify(tradingService).handleBankTradeRequest(player1Id, expectedRequest);

        assertEquals(0, player.getResourceCount(TileType.WOOD));
        assertEquals(1, player.getResourceCount(TileType.SHEEP));
    }

    @Test
    void handleTradeWithBankPlayerNotActiveThrowsInvalidTurnException() throws GameException {
        WebSocketConnection connection = mock(WebSocketConnection.class);
        when(connection.id()).thenReturn("1234");
        Player player = playerService.addPlayer(connection);
        String player1Id = player.getUniqueId();
        String lobbyId = lobbyService.createLobby(player1Id);
        // The active player is the host (player1Id) by default, so we set it to someone else
        lobbyService.getLobbyById(lobbyId).setActivePlayer("anotherPlayer");

        MessageDTO inputMessage = new MessageDTO(MessageType.TRADE_WITH_BANK, player1Id, lobbyId, JsonNodeFactory.instance.objectNode());

        // We check the error from the handler, as it's the one catching the exception
        Uni<MessageDTO> resultUni = gameMessageHandler.handleGameMessage(inputMessage);
        MessageDTO resultDto = resultUni.await().indefinitely();

        assertEquals(MessageType.ERROR, resultDto.getType());
        assertEquals(new InvalidTurnException().getMessage(), resultDto.getMessageNode("error").asText());

        verify(tradingService, never()).handleBankTradeRequest(any(String.class), any(TradeRequest.class));
        verify(lobbyService, never()).notifyPlayers(anyString(), any(MessageDTO.class));
    }

    @Test
    void handleTradeWithBankTradeServiceFailsThrowsGameException() throws GameException {
        WebSocketConnection connection = mock(WebSocketConnection.class);
        when(connection.id()).thenReturn("1234");
        Player player = playerService.addPlayer(connection);
        String player1Id = player.getUniqueId();
        String lobbyId = lobbyService.createLobby(player1Id);
        lobbyService.getLobbyById(lobbyId).setActivePlayer(player1Id);

        // Initial state for the player
        player.getResources().clear();
        player.getResources().put(TileType.WOOD, 1); // Not enough for a 4:1 trade
        player.getResources().put(TileType.SHEEP, 0);


        // Prepare invalid trade request (1 WOOD for 1 SHEEP)
        ObjectNode tradePayload = JsonNodeFactory.instance.objectNode();
        tradePayload.putObject("offeredResources").put(TileType.WOOD.name(), 1);
        tradePayload.putObject("targetResources").put(TileType.SHEEP.name(), 1);

        MessageDTO inputMessage = new MessageDTO(MessageType.TRADE_WITH_BANK, player1Id, lobbyId, tradePayload);

        // Mock LobbyService to pass checkTurn
        doNothing().when(lobbyService).checkPlayerTurn(lobbyId, player1Id);

        // The handler will catch the exception and create an error DTO
        Uni<MessageDTO> resultUni = gameMessageHandler.handleGameMessage(inputMessage);
        MessageDTO resultDto = resultUni.await().indefinitely();

        assertEquals(MessageType.ERROR, resultDto.getType());
        assertEquals("Trade ratio is invalid", resultDto.getMessageNode("error").asText());

        verify(lobbyService, never()).notifyPlayers(anyString(), any(MessageDTO.class));
    }


    @Test
    void handleTradeWithBankWhenPayloadIsMalformedReturnsErrorDto() throws GameException {
        WebSocketConnection connection = mock(WebSocketConnection.class);
        when(connection.id()).thenReturn("test-connection-id");
        Player player = playerService.addPlayer(connection);
        String playerId = player.getUniqueId();
        String lobbyId = lobbyService.createLobby(playerId);
        lobbyService.getLobbyById(lobbyId).setActivePlayer(playerId);


        ObjectNode malformedPayload = JsonNodeFactory.instance.objectNode();
        malformedPayload.put("offeredResources", "this-is-not-a-map"); // Invalid type
        malformedPayload.putObject("targetResources").put(TileType.SHEEP.name(), 1);

        MessageDTO inputMessage = new MessageDTO(MessageType.TRADE_WITH_BANK, playerId, lobbyId, malformedPayload);

        Uni<MessageDTO> resultUni = gameMessageHandler.handleGameMessage(inputMessage);

        MessageDTO resultDto = resultUni.await().indefinitely();

        assertNotNull(resultDto);
        assertEquals(MessageType.ERROR, resultDto.getType());
        assertEquals("Invalid trade request format.", resultDto.getMessageNode("error").asText());

        verify(tradingService, never()).handleBankTradeRequest(anyString(), any(TradeRequest.class));
        verify(lobbyService, never()).notifyPlayers(anyString(), any(MessageDTO.class));
    }

    @Test
    void handleCheatAttempt_validResource_shouldBroadcastUpdate() throws GameException {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.id()).thenReturn("playerY");
        Player player = playerService.addPlayer(conn);
        String playerId = player.getUniqueId();
        String lobbyId = lobbyService.createLobby(playerId);

        TileType resource = TileType.WOOD;

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("resource", resource.name());
        MessageDTO inputMessage = new MessageDTO(MessageType.CHEAT_ATTEMPT, playerId, lobbyId, payload);

        doNothing().when(gameService).handleCheat(lobbyId, playerId, resource);

        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        PlayerInfo playerInfo = mock(PlayerInfo.class);
        when(playerMapper.toDto(player, lobby)).thenReturn(playerInfo);

        MessageDTO expected = new MessageDTO(MessageType.PLAYER_RESOURCE_UPDATE, playerId, lobbyId, Map.of(playerId, playerInfo));
        when(lobbyService.notifyPlayers(eq(lobbyId), any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().item(expected));

        MessageDTO result = gameMessageHandler.handleCheatAttempt(inputMessage).await().indefinitely();

        assertNotNull(result);
        assertEquals(MessageType.PLAYER_RESOURCE_UPDATE, result.getType());
        assertEquals(playerId, result.getPlayer());
        verify(gameService, times(1)).handleCheat(lobbyId, playerId, resource);
        verify(lobbyService, times(1)).notifyPlayers(eq(lobbyId), any(MessageDTO.class));
    }


    @Test
    void handleCheatAttempt_gameException_shouldReturnErrorDto() throws GameException {
        String lobbyId = "lobbyX";
        String playerId = "playerY";
        TileType resource = TileType.ORE;

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("resource", resource.name());
        MessageDTO inputMessage = new MessageDTO(MessageType.CHEAT_ATTEMPT, playerId, lobbyId, payload);

        doThrow(new GameException("cheating not allowed")).when(gameService)
                .handleCheat(lobbyId, playerId, resource);

        MessageDTO result = gameMessageHandler.handleCheatAttempt(inputMessage).await().indefinitely();

        assertNotNull(result);
        assertEquals(MessageType.ERROR, result.getType());
        assertEquals("cheating not allowed", result.getMessageNode("error").asText());
        verify(gameService, times(1)).handleCheat(lobbyId, playerId, resource);
        verify(lobbyService, never()).notifyPlayers(any(), any());
    }

    @Test
    void handleCheatAttempt_invalidResource_shouldReturnInvalidResourceTypeError() throws GameException {
        String lobbyId = "lobbyX";
        String playerId = "playerY";
        String invalidResource = "BANANA";

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("resource", invalidResource);
        MessageDTO inputMessage = new MessageDTO(MessageType.CHEAT_ATTEMPT, playerId, lobbyId, payload);

        MessageDTO result = gameMessageHandler.handleCheatAttempt(inputMessage).await().indefinitely();

        assertNotNull(result);
        assertEquals(MessageType.ERROR, result.getType());
        assertEquals("Invalid resource type", result.getMessageNode("error").asText());
        verify(gameService, never()).handleCheat(any(), any(), any());
        verify(lobbyService, never()).notifyPlayers(any(), any());
    }

    @Test
    void handleReportPlayer_gameException_shouldReturnErrorDto() throws GameException {
        String lobbyId = "lobbyX";
        String reporterId = "reporterY";
        String reportedId = "reportedY";

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("reportedId", reportedId);
        MessageDTO inputMessage = new MessageDTO(MessageType.REPORT_PLAYER, reporterId, lobbyId, payload);

        doThrow(new GameException("reporting not allowed"))
                .when(gameService).handleReportPlayer(lobbyId, reporterId, reportedId);

        MessageDTO result = gameMessageHandler.handleReportPlayer(inputMessage).await().indefinitely();

        assertNotNull(result);
        assertEquals(MessageType.ERROR, result.getType());
        assertEquals("reporting not allowed", result.getMessageNode("error").asText());
        verify(gameService, times(1)).handleReportPlayer(lobbyId, reporterId, reportedId);
        verify(lobbyService, never()).notifyPlayers(any(), any());
    }

    @Test
    void handleReportPlayer_invalidPlayer_shouldReturnInvalidPlayerError() throws GameException {
        String lobbyId = "lobbyZ";
        String reporterId = "reporterZ";
        String invalidReportedId = "not-a-real-player";

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("reportedId", invalidReportedId);
        MessageDTO inputMessage = new MessageDTO(MessageType.REPORT_PLAYER, reporterId, lobbyId, payload);

        doThrow(new IllegalArgumentException("No such player"))
                .when(gameService).handleReportPlayer(lobbyId, reporterId, invalidReportedId);

        MessageDTO result = gameMessageHandler.handleReportPlayer(inputMessage).await().indefinitely();

        assertNotNull(result);
        assertEquals(MessageType.ERROR, result.getType());
        assertEquals("Invalid player to report.", result.getMessageNode("error").asText());
        verify(gameService, times(1)).handleReportPlayer(lobbyId, reporterId, invalidReportedId);
        verify(lobbyService, never()).notifyPlayers(any(), any());
    }

    @Test
    void handleReportPlayer_correctReportNew_shouldSendSuccessAlertAndUpdate() throws GameException {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.id()).thenReturn("reporter");
        Player reporter = playerService.addPlayer(conn);

        Player reported = mock(Player.class);
        String reportedId = "mock-reported-id";
        when(reported.getUniqueId()).thenReturn(reportedId); // FIX
        when(reported.getUsername()).thenReturn("cheater");

        String reporterId = reporter.getUniqueId();
        String lobbyId = lobbyService.createLobby(reporterId);
        lobbyService.joinLobbyByCode(lobbyId, reportedId);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("reportedId", reportedId);
        MessageDTO inputMessage = new MessageDTO(MessageType.REPORT_PLAYER, reporterId, lobbyId, payload);

        when(gameService.handleReportPlayer(lobbyId, reporterId, reportedId))
                .thenReturn(ReportOutcome.CORRECT_REPORT_NEW);
        when(playerService.getPlayerById(reportedId)).thenReturn(reported);
        when(playerService.sendMessageToPlayer(eq(reporterId), any()))
                .thenReturn(Uni.createFrom().voidItem());

        MessageDTO updateMessage = new MessageDTO(MessageType.PLAYER_RESOURCE_UPDATE, reporterId, lobbyId, Map.of());
        when(lobbyService.notifyPlayers(eq(lobbyId), any()))
                .thenReturn(Uni.createFrom().item(updateMessage));

        MessageDTO result = gameMessageHandler.handleReportPlayer(inputMessage).await().indefinitely();

        assertEquals(MessageType.PLAYER_RESOURCE_UPDATE, result.getType());
        verify(playerService).sendMessageToPlayer(eq(reporterId), argThat(alert ->
                alert.getMessageNode("message").asText().contains("cheater got caught cheating!") &&
                        alert.getMessageNode("severity").asText().equals("success")
        ));
    }

    @Test
    void handleReportPlayer_correctReportAlreadyCaught_shouldSendErrorAlertAndUpdate() throws GameException {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.id()).thenReturn("reporter");
        Player reporter = playerService.addPlayer(conn);

        Player reported = mock(Player.class);
        String reportedId = "mock-reported-id";
        when(reported.getUniqueId()).thenReturn(reportedId);
        when(reported.getUsername()).thenReturn("cheater");

        String reporterId = reporter.getUniqueId();
        String lobbyId = lobbyService.createLobby(reporterId);
        lobbyService.joinLobbyByCode(lobbyId, reportedId);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("reportedId", reportedId);
        MessageDTO inputMessage = new MessageDTO(MessageType.REPORT_PLAYER, reporterId, lobbyId, payload);

        when(gameService.handleReportPlayer(lobbyId, reporterId, reportedId)).thenReturn(ReportOutcome.CORRECT_REPORT_ALREADY_CAUGHT);
        when(playerService.getPlayerById(reportedId)).thenReturn(reported);
        when(playerService.sendMessageToPlayer(eq(reporterId), any())).thenReturn(Uni.createFrom().voidItem());

        MessageDTO updateMessage = new MessageDTO(MessageType.PLAYER_RESOURCE_UPDATE, reporterId, lobbyId, Map.of());
        when(lobbyService.notifyPlayers(eq(lobbyId), any())).thenReturn(Uni.createFrom().item(updateMessage));

        MessageDTO result = gameMessageHandler.handleReportPlayer(inputMessage).await().indefinitely();

        assertEquals(MessageType.PLAYER_RESOURCE_UPDATE, result.getType());
        verify(playerService).sendMessageToPlayer(eq(reporterId), argThat(alert ->
                alert.getMessageNode("message").asText().contains("was already caught cheating") &&
                        alert.getMessageNode("severity").asText().equals("error")
        ));
    }

    @Test
    void handleReportPlayer_falseReport_shouldSendErrorAlertAndUpdate() throws GameException {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.id()).thenReturn("reporter");
        Player reporter = playerService.addPlayer(conn);

        Player reported = mock(Player.class);
        String reportedId = "mock-reported-id";
        when(reported.getUniqueId()).thenReturn(reportedId);
        when(reported.getUsername()).thenReturn("cheater");

        String reporterId = reporter.getUniqueId();
        String lobbyId = lobbyService.createLobby(reporterId);
        lobbyService.joinLobbyByCode(lobbyId, reportedId);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("reportedId", reportedId);
        MessageDTO inputMessage = new MessageDTO(MessageType.REPORT_PLAYER, reporterId, lobbyId, payload);

        when(gameService.handleReportPlayer(lobbyId, reporterId, reportedId)).thenReturn(ReportOutcome.FALSE_REPORT);
        when(playerService.getPlayerById(reportedId)).thenReturn(reported);
        when(playerService.sendMessageToPlayer(eq(reporterId), any())).thenReturn(Uni.createFrom().voidItem());

        MessageDTO updateMessage = new MessageDTO(MessageType.PLAYER_RESOURCE_UPDATE, reporterId, lobbyId, Map.of());
        when(lobbyService.notifyPlayers(eq(lobbyId), any())).thenReturn(Uni.createFrom().item(updateMessage));

        MessageDTO result = gameMessageHandler.handleReportPlayer(inputMessage).await().indefinitely();

        assertEquals(MessageType.PLAYER_RESOURCE_UPDATE, result.getType());
        verify(playerService).sendMessageToPlayer(eq(reporterId), argThat(alert ->
                alert.getMessageNode("message").asText().contains("falsely accused") &&
                        alert.getMessageNode("severity").asText().equals("error")
        ));
    }

    @Test
    void handleReportPlayer_notifyPlayersThrows_shouldReturnErrorDto() throws GameException {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.id()).thenReturn("reporter");
        Player reporter = playerService.addPlayer(conn);

        Player reported = mock(Player.class);
        String reportedId = "mock-reported-id";
        when(reported.getUniqueId()).thenReturn(reportedId);
        when(reported.getUsername()).thenReturn("cheater");

        String reporterId = reporter.getUniqueId();
        String lobbyId = lobbyService.createLobby(reporterId);
        lobbyService.joinLobbyByCode(lobbyId, reportedId);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("reportedId", reportedId);
        MessageDTO inputMessage = new MessageDTO(MessageType.REPORT_PLAYER, reporterId, lobbyId, payload);

        when(gameService.handleReportPlayer(lobbyId, reporterId, reportedId)).thenReturn(ReportOutcome.FALSE_REPORT);
        when(playerService.getPlayerById(reportedId)).thenReturn(reported);
        when(playerService.sendMessageToPlayer(eq(reporterId), any())).thenReturn(Uni.createFrom().voidItem());

        when(lobbyService.notifyPlayers(eq(lobbyId), any()))
                .thenReturn(Uni.createFrom().failure(new GameException("notify error")));

        try {
            gameMessageHandler.handleReportPlayer(inputMessage).await().indefinitely();
            fail("Expected CompletionException to be thrown");
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            assertInstanceOf(GameException.class, cause);
            assertEquals("notify error", cause.getMessage());
        }
    }










}