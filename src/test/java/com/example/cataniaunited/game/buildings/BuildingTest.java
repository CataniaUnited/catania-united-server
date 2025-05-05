package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class BuildingTest {

    @Test
    void testToJson() throws GameException {
        String playerId = "player1";
        PlayerColor color = PlayerColor.LIGHT_ORANGE;
        var building = new TestBuilding(playerId, color);

        ObjectNode buildingJson = building.toJson();

        assertEquals(playerId, buildingJson.get("owner").asText());
        assertEquals(color.getHexCode(), buildingJson.get("color").asText());
        assertEquals("TestBuilding", buildingJson.get("type").asText());
    }

    @Test
    void constructorShouldThrowExceptionWhenPlayerIdIsNull() throws GameException {
        GameException ge = assertThrows(GameException.class, () -> new TestBuilding(null, PlayerColor.LAVENDER));
        assertEquals("Owner Id of building must not be empty", ge.getMessage());
    }

    @Test
    void constructorShouldThrowExceptionWhenPlayerIdIsEmpty() throws GameException {
        GameException ge = assertThrows(GameException.class, () -> new TestBuilding("", PlayerColor.LAVENDER));
        assertEquals("Owner Id of building must not be empty", ge.getMessage());
    }

    @Test
    void constructorShouldThrowExceptionWhenColorIsNull() throws GameException {
        GameException ge = assertThrows(GameException.class, () -> new TestBuilding("player1", null));
        assertEquals("Color of building must not be null", ge.getMessage());
    }
}

class TestBuilding extends Building {
    protected TestBuilding(String playerId, PlayerColor color) throws GameException {
        super(playerId, color);
    }
}
