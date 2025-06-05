package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.InvalidTurnException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.trade.TradingService;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.mapper.PlayerMapper;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

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
        ArrayNode offered = tradePayload.putArray("offeredResources");
        offered.add("WOOD"); offered.add("WOOD"); offered.add("WOOD"); offered.add("WOOD");
        ArrayNode target = tradePayload.putArray("targetResources");
        target.add("SHEEP");
        tradePayload.put("target", "bank");

        MessageDTO inputMessage = new MessageDTO(MessageType.TRADE_WITH_BANK, player1Id, lobbyId, tradePayload);

        // Mock LobbyService to pass checkTurn
        doNothing().when(lobbyService).checkPlayerTurn(lobbyId, player1Id);
        Lobby mockLobby = mock(Lobby.class);
        when(lobbyService.getLobbyById(lobbyId)).thenReturn(mockLobby);

        gameMessageHandler.handleGameMessage(inputMessage);

        verify(lobbyService).checkPlayerTurn(lobbyId, player1Id);
        verify(tradingService).handleBankTradeRequest(inputMessage);

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

        MessageDTO inputMessage = new MessageDTO(MessageType.TRADE_WITH_BANK, player1Id, lobbyId, JsonNodeFactory.instance.objectNode());

        assertThrows(InvalidTurnException.class, () -> gameMessageHandler.handleTradeWithBank(inputMessage));

        verify(tradingService, never()).handleBankTradeRequest(any(MessageDTO.class));
        verify(lobbyService, never()).notifyPlayers(anyString(), any(MessageDTO.class));
    }

    @Test
    void handleTradeWithBankTradeServiceFailsThrowsGameException() throws GameException {
        WebSocketConnection connection = mock(WebSocketConnection.class);
        when(connection.id()).thenReturn("1234");
        Player player = playerService.addPlayer(connection);
        String player1Username = "Player 1";
        String player1Id = player.getUniqueId();
        playerService.setUsername(player.getUniqueId(), player1Username);
        String lobbyId = lobbyService.createLobby(player1Id);

        // Initial state for the player
        player.getResources().clear();
        player.getResources().put(TileType.WOOD, 4);
        player.getResources().put(TileType.SHEEP, 0);


        // Prepare trade request: 1 WOOD for 1 SHEEP -> FAILS -> Game Exception
        ObjectNode tradePayload = JsonNodeFactory.instance.objectNode();
        ArrayNode offered = tradePayload.putArray("offeredResources");
        offered.add("WOOD");
        ArrayNode target = tradePayload.putArray("targetResources");
        target.add("SHEEP");
        tradePayload.put("target", "bank");

        MessageDTO inputMessage = new MessageDTO(MessageType.TRADE_WITH_BANK, player1Id, lobbyId, tradePayload);

        // Mock LobbyService to pass checkTurn
        doNothing().when(lobbyService).checkPlayerTurn(lobbyId, player1Id);
        Lobby mockLobby = mock(Lobby.class);
        when(lobbyService.getLobbyById(lobbyId)).thenReturn(mockLobby);

        assertThrows(GameException.class, () -> gameMessageHandler.handleTradeWithBank(inputMessage));

        verify(lobbyService, never()).notifyPlayers(anyString(), any(MessageDTO.class));
    }
}
