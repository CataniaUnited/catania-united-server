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
    private City city;
    private final String testPlayerId = "playerCity1";
    private final PlayerColor testColor = PlayerColor.RED;

    @BeforeEach
    void setUp() throws GameException {
        Player mockPlayer = Mockito.mock(Player.class);
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

}