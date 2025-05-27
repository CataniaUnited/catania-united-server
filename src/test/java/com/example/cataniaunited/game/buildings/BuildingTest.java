package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class BuildingTest {

    @Test
    void testToJson() throws GameException {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn("myUniqueId");
        PlayerColor color = PlayerColor.LIGHT_ORANGE;
        var building = new TestBuilding(player, color);

        ObjectNode buildingJson = building.toJson();

        assertEquals(player.getUniqueId(), buildingJson.get("owner").asText());
        assertEquals(color.getHexCode(), buildingJson.get("color").asText());
        assertEquals("TestBuilding", buildingJson.get("type").asText());
    }

    @Test
    void constructorShouldThrowExceptionWhenPlayerIdIsNull() {
        GameException ge = assertThrows(GameException.class, () -> new TestBuilding(null, PlayerColor.LAVENDER));
        assertEquals("Owner of building must not be empty", ge.getMessage());
    }

    @Test
    void constructorShouldThrowExceptionWhenColorIsNull() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn("id1");
        GameException ge = assertThrows(GameException.class, () -> new TestBuilding(player, null));
        assertEquals("Color of building must not be null", ge.getMessage());
    }

    @Test
    void testSuccessfulConstructorInitialization() throws GameException {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn("id1");
        PlayerColor expectedColor = PlayerColor.GREEN;
        TestBuilding building = new TestBuilding(player, expectedColor);

        assertNotNull(building, "Building instance should not be null.");
        assertEquals(player, building.getPlayer(), "Owner player ID should be correctly set.");
    }

    @Test
    void testGetOwnerPlayerId() throws GameException {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn("id1");
        PlayerColor color = PlayerColor.YELLOW;
        TestBuilding building = new TestBuilding(player, color);

        assertEquals(player, building.getPlayer(), "getPlayer should return the correct player ID.");
    }

}

// fixme extract to extra file
class TestBuilding extends Building {
    protected TestBuilding(Player player, PlayerColor color) throws GameException {
        super(player, color);
    }

    @Override
    public int getResourceDistributionAmount() {
        return 1;
    }

    @Override
    public Map<TileType, Integer> getRequiredResources() {
        return Map.of();
    }
}