package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.dto.PlayerInfo;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class GameMessageHandlerTest {

    @Inject
    GameMessageHandler gameMessageHandler;

    @InjectSpy
    PlayerService playerService;

    @InjectSpy
    LobbyService lobbyService;

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

}
