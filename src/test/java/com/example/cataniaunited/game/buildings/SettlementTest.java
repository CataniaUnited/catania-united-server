package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@QuarkusTest
public class SettlementTest {
    @InjectMock
    PlayerService mockPlayerService;

    private Settlement settlement;

    private Player mockPlayer;
    private final String testPlayerId = "playerSettle1";
    private final PlayerColor testColor = PlayerColor.BLUE;


    @BeforeEach
    void setUp() throws GameException {
        mockPlayer = Mockito.mock(Player.class);
        settlement = new Settlement(testPlayerId, testColor);
        settlement.playerService = mockPlayerService;
    }

    @Test
    void constructorShouldSetFieldsCorrectly() {
        assertNotNull(settlement);
        assertEquals(testPlayerId, settlement.getOwnerPlayerId());
    }

    @Test
    void toJsonReturnsCorrectTypeAndValues() {
        ObjectNode json = settlement.toJson();
        assertEquals(testPlayerId, json.get("owner").asText());
        assertEquals(testColor.getHexCode(), json.get("color").asText());
        assertEquals("Settlement", json.get("type").asText(), "Type should be 'Settlement'");
    }

    @Test
    void distributeResourcesToPlayerShouldCallPlayerServiceAndGiveOneResource() {
        TileType resourceType = TileType.WHEAT;
        int expectedAmount = 1;

        when(mockPlayerService.getPlayerById(testPlayerId)).thenReturn(mockPlayer);

        settlement.distributeResourcesToPlayer(resourceType);

        verify(mockPlayerService, times(1)).getPlayerById(testPlayerId);
        verify(mockPlayer, times(1)).getResource(resourceType, expectedAmount);
    }

    @Test
    void distributeResourcesToPlayerHandlesDifferentResourceTypes() {
        TileType resourceType = TileType.WHEAT;
        int expectedAmount = 1;

        when(mockPlayerService.getPlayerById(testPlayerId)).thenReturn(mockPlayer);

        settlement.distributeResourcesToPlayer(resourceType);

        verify(mockPlayerService, times(1)).getPlayerById(testPlayerId);
        verify(mockPlayer, times(1)).getResource(resourceType, expectedAmount);
    }

    @Test
    void distributeResourcesToPlayerWhenPlayerServiceReturnsNullPlayer() {
        TileType resourceType = TileType.ORE;
        when(mockPlayerService.getPlayerById(testPlayerId)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> settlement.distributeResourcesToPlayer(resourceType), "Should throw NullPointerException if player is not found by service.");

        verify(mockPlayerService, times(1)).getPlayerById(testPlayerId);
        verify(mockPlayer, never()).getResource(any(TileType.class), anyInt());
    }
}