package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CityTest {
    private City city;
    private final String testPlayerId = "playerCity1";
    private final PlayerColor testColor = PlayerColor.RED;
    Player mockPlayer;

    @BeforeEach
    void setUp() throws GameException {
        mockPlayer = Mockito.mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(testPlayerId);

        city = new City(mockPlayer, testColor);
    }

    @Test
    void constructorShouldSetFieldsCorrectly() {
        assertNotNull(city);
        assertEquals(testPlayerId, city.getPlayer().getUniqueId());
    }

    @Test
    void toJsonReturnsCorrectTypeAndValues() {
        ObjectNode json = city.toJson();
        assertEquals(testPlayerId, json.get("owner").asText());
        assertEquals(testColor.getHexCode(), json.get("color").asText());
        assertEquals("City", json.get("type").asText(), "Type should be 'City'");
    }

    @Test
    void distributeResourcesToPlayerShouldCallPlayerGetResourceWithTwoUnits() {
        TileType testResource = TileType.WHEAT;

        city.distributeResourcesToPlayer(testResource);

        verify(mockPlayer).getResource(testResource, 2);
    }

    @Test
    void constructorNullPlayerThrowsGameException() {
        assertThrows(GameException.class, () -> {
            new City(null, testColor);
        }, "Constructor should throw GameException for null player.");
    }

    @Test
    void constructorPlayerWithNullIdThrowsGameException() {
        Player playerWithNullId = Mockito.mock(Player.class);
        when(playerWithNullId.getUniqueId()).thenReturn(null);
        assertThrows(GameException.class, () -> {
            new City(playerWithNullId, testColor);
        }, "Constructor should throw GameException for player with null ID.");
    }

    @Test
    void constructorPlayerWithEmptyIdThrowsGameException() {
        Player playerWithEmptyId = Mockito.mock(Player.class);
        when(playerWithEmptyId.getUniqueId()).thenReturn("");
        assertThrows(GameException.class, () -> {
            new City(playerWithEmptyId, testColor);
        }, "Constructor should throw GameException for player with empty ID.");
    }

    @Test
    void constructorNullColorThrowsGameException() {
        assertThrows(GameException.class, () -> {
            new City(mockPlayer, null);
        }, "Constructor should throw GameException for null color.");
    }

}