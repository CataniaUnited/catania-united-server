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

    private Settlement settlement;
    private final String testPlayerId = "playerSettle1";
    private final PlayerColor testColor = PlayerColor.BLUE;


    @BeforeEach
    void setUp() throws GameException {
        Player mockPlayer = Mockito.mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(testPlayerId);
        settlement = new Settlement(mockPlayer, testColor);
    }

    @Test
    void constructorShouldSetFieldsCorrectly() {
        assertNotNull(settlement);
        assertEquals(testPlayerId, settlement.getPlayer().getUniqueId());
    }

    @Test
    void toJsonReturnsCorrectTypeAndValues() {
        ObjectNode json = settlement.toJson();
        assertEquals(testPlayerId, json.get("owner").asText());
        assertEquals(testColor.getHexCode(), json.get("color").asText());
        assertEquals("Settlement", json.get("type").asText(), "Type should be 'Settlement'");
    }
}