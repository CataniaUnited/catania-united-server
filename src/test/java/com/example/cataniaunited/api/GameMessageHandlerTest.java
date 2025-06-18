package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.dto.PlayerInfo;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.InvalidTurnException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
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

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    /*@Test
    void testPlaceRobber_ValidTileId_NotifiesPlayers() throws Exception {
        String lobbyId = "123abc";
        String playerId = "player1";
        int tileId = 5;

        MessageDTO message = Mockito.mock(MessageDTO.class);
        JsonNode tileIdNode = new ObjectMapper().readTree("{\"tileId\": " + tileId + "}").get("tileId");

        Mockito.when(message.getMessageNode("tileId")).thenReturn(tileIdNode);
        Mockito.when(message.getLobbyId()).thenReturn(lobbyId);
        Mockito.when(message.getPlayer()).thenReturn(playerId);

        ObjectNode gameBoardInfo = new ObjectMapper().createObjectNode();
        ObjectNode playerInfo = new ObjectMapper().createObjectNode();

        GameBoard board = Mockito.mock(GameBoard.class);
        Mockito.when(gameService.getGameboardByLobbyId(lobbyId)).thenReturn(board);
        Mockito.when(gameMessageHandler.getLobbyPlayerInformation(lobbyId)).thenReturn((Map<String, PlayerInfo>) playerInfo);
        Mockito.when(lobbyService.notifyPlayers(Mockito.eq(lobbyId), Mockito.any()))
                .thenReturn(Uni.createFrom().nullItem());

        Uni <MessageDTO> result = gameMessageHandler.placeRobber(message);
        MessageDTO resultDTO = result.await().indefinitely();

        Mockito.verify(gameService).placeRobber(lobbyId, playerId, tileId);
        Mockito.verify(lobbyService).notifyPlayers(Mockito.eq(lobbyId), Mockito.argThat(dto ->
                dto.getType() == MessageType.PLACE_ROBBER &&
                dto.getLobbyId().equals(lobbyId) &&
                dto.getPlayer().equals(playerId)
        ));

        assertEquals(MessageType.PLACE_ROBBER, resultDTO.getType());
    }*/

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
}