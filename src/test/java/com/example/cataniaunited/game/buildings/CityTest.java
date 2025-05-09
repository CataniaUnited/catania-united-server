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
public class CityTest {

    @InjectMock
    PlayerService mockPlayerService;

    private City city;

    private Player mockPlayer;
    private final String testPlayerId = "playerCity1";
    private final PlayerColor testColor = PlayerColor.RED;

    @BeforeEach
    void setUp() throws GameException {
        mockPlayer = Mockito.mock(Player.class);

        city = new City(testPlayerId, testColor);

        city.playerService = mockPlayerService;
    }

    @Test
    void constructorShouldSetFieldsCorrectly() {
        assertNotNull(city);
        assertEquals(testPlayerId, city.getOwnerPlayerId());
    }

    @Test
    void toJsonReturnsCorrectTypeAndValues() {
        ObjectNode json = city.toJson();
        assertEquals(testPlayerId, json.get("owner").asText());
        assertEquals(testColor.getHexCode(), json.get("color").asText());
        assertEquals("City", json.get("type").asText(), "Type should be 'City'");
    }

    @Test
    void distributeResourcesToPlayerShouldCallPlayerServiceAndGiveTwoResources() {
        TileType resourceType = TileType.WOOD;
        int expectedAmount = 2;

        when(mockPlayerService.getPlayerById(testPlayerId)).thenReturn(mockPlayer);

        city.distributeResourcesToPlayer(resourceType);

        verify(mockPlayerService, times(1)).getPlayerById(testPlayerId);
        verify(mockPlayer, times(1)).getResource(resourceType, expectedAmount);
    }

    @Test
    void distributeResourcesToPlayerHandlesDifferentResourceTypes() {
        TileType resourceType = TileType.SHEEP;
        int expectedAmount = 2;

        when(mockPlayerService.getPlayerById(testPlayerId)).thenReturn(mockPlayer);

        city.distributeResourcesToPlayer(resourceType);

        verify(mockPlayerService, times(1)).getPlayerById(testPlayerId);
        verify(mockPlayer, times(1)).getResource(resourceType, expectedAmount);
    }

    @Test
    void distributeResourcesToPlayerWhenPlayerServiceReturnsNullPlayer() {
        TileType resourceType = TileType.ORE;
        when(mockPlayerService.getPlayerById(testPlayerId)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> {
            city.distributeResourcesToPlayer(resourceType);
        }, "Should throw NullPointerException if player is not found by service.");

        verify(mockPlayerService, times(1)).getPlayerById(testPlayerId);
        verify(mockPlayer, never()).getResource(any(TileType.class), anyInt());
    }
}