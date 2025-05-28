package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class RoadTest {
    static SettlementPosition mockPositionA;
    static SettlementPosition mockPositionB;
    private static final int ROAD_ID = 12;
    Road road;

    @BeforeEach
    void setUp() {
        //Ensure that "." is used instead of "," to separate decimals
        Locale.setDefault(Locale.ENGLISH);

        mockPositionA = mock(SettlementPosition.class);
        mockPositionB = mock(SettlementPosition.class);


        road = new Road(mockPositionA, mockPositionB, ROAD_ID);
    }

    @Test
    void getNeighbourReturnsCorrectSettlementPositionIfGivenPositionIsValid() {
        assertSame(mockPositionB, road.getNeighbour(mockPositionA), "getNeighbour should return B when given A");
        assertSame(mockPositionA, road.getNeighbour(mockPositionB), "getNeighbour should return A when given B");
    }

    @Test
    void getNeighbourReturnsNullWhenGivenUnknownPosition() {
        SettlementPosition mockPositionC = mock(SettlementPosition.class);
        SettlementPosition neighbour = road.getNeighbour(mockPositionC);
        assertNull(neighbour, "getNeighbour should return null If an invalid Position is given");
    }

    @Test
    void getCoordinatesReturnsInitialZeroArrayBeforeCalculation() {
        double[] expected = {0.0, 0.0};
        assertArrayEquals(expected, road.getCoordinates(), "Initial coordinates should be [0.0, 0.0]");
    }

    @Test
    void getRationAngleReturnsInitialZeroBeforeCalculation() {
        assertEquals(0.0, road.getRotationAngle(), 0.001, "Initial angle should be 0.0");
    }

    @Test
    void setCoordinatesAndRotationAngleCalculatesCorrectly() {
        // Mock getCoordinates from Settlementposition
        double[] coordsA = {10.0, 20.0};
        double[] coordsB = {30.0, 60.0};
        when(mockPositionA.getCoordinates()).thenReturn(coordsA);
        when(mockPositionB.getCoordinates()).thenReturn(coordsB);

        // Run
        road.setCoordinatesAndRotationAngle();

        // Verify
        double expectedX = (coordsA[0] + coordsB[0]) / 2.0; // (10 + 30) / 2 = 20
        double expectedY = (coordsA[1] + coordsB[1]) / 2.0; // (20 + 60) / 2 = 40
        double[] expectedCoords = {expectedX, expectedY};

        double angleX = coordsA[0] - coordsB[0]; // 10 - 30 = -20
        double angleY = coordsA[1] - coordsB[1]; // 20 - 60 = -40
        double expectedAngle = StrictMath.atan2(angleY, angleX); // atan2(-40, -20)

        assertArrayEquals(expectedCoords, road.transform.getCoordinatesArray(), 0.001, "Calculated coordinates are incorrect");
        assertEquals(expectedAngle, road.transform.rotation(), 0.001, "Calculated angle is incorrect");
    }

    @Test
    void setCoordinatesAndRotationAngleDoesNothingIfAlreadySet() {
        // Mock getCoordinates from Settlementposition
        double[] coordsA = {10.0, 20.0};
        double[] coordsB = {30.0, 60.0};
        when(mockPositionA.getCoordinates()).thenReturn(coordsA);
        when(mockPositionB.getCoordinates()).thenReturn(coordsB);

        road.setCoordinatesAndRotationAngle();

        double[] coordsAfterFirstCall = road.getCoordinates().clone();
        double angleAfterFirstCall = road.getRotationAngle();

        // Setup done, test
        // Change given Coords
        when(mockPositionA.getCoordinates()).thenReturn(new double[]{99.0, 100.0});
        when(mockPositionB.getCoordinates()).thenReturn(new double[]{101.0, 100.0});

        // Act: Call the method again
        road.setCoordinatesAndRotationAngle();

        // Assert: State should be unchanged from after the first call
        assertArrayEquals(coordsAfterFirstCall, road.getCoordinates(), 0.001, "Coordinates should not change on second call");
        assertEquals(angleAfterFirstCall, road.getRotationAngle(), 0.001, "Angle should not change on second call");

        // Verify that the method didn't even try to calculate the coords but returned after the first check if position has already been set
        verify(mockPositionA, times(1)).getCoordinates();
        verify(mockPositionB, times(1)).getCoordinates();
    }

    @Test
    void setCoordinatesAndRotationAngleDoesNothingIfPositionAIsNotSet() {
        // Position a is not yes set
        when(mockPositionA.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockPositionB.getCoordinates()).thenReturn(new double[]{1.0, 1.0});


        road.setCoordinatesAndRotationAngle();

        // No change in position
        assertArrayEquals(new double[]{0.0, 0.0}, road.getCoordinates(), 0.001, "Coordinates should remain zero");
        assertEquals(0.0, road.getRotationAngle(), 0.001, "Angle should remain zero");

        // Verify getCoordinates was called on A, but not on B
        verify(mockPositionA, times(1)).getCoordinates();
        verify(mockPositionB, never()).getCoordinates();
    }

    @Test
    void setCoordinatesAndRotationAngleDoesNothingIfPositionBIsNotSet() {
        //Position B is not yet set
        when(mockPositionA.getCoordinates()).thenReturn(new double[]{1.0, 1.0});
        when(mockPositionB.getCoordinates()).thenReturn(new double[]{0.0, 0.0});

        road.setCoordinatesAndRotationAngle();

        // No change in position
        assertArrayEquals(new double[]{0.0, 0.0}, road.getCoordinates(), 0.001, "Coordinates should remain zero");
        assertEquals(0.0, road.getRotationAngle(), 0.001, "Angle should remain zero");

        // Verify getCoordinates was called on both A and B but method returned early.
        verify(mockPositionA, times(1)).getCoordinates();
        verify(mockPositionB, times(1)).getCoordinates();
    }

    @Test
    void verifyGetCoordinatesReturnsNewArrayNotInstance() {
        when(mockPositionA.getCoordinates()).thenReturn(new double[]{10.0, 20.0});
        when(mockPositionB.getCoordinates()).thenReturn(new double[]{30.0, 60.0});
        road.setCoordinatesAndRotationAngle();

        double[] coords1 = road.getCoordinates();
        double[] coords2 = road.getCoordinates();

        // coords1 != coords2, cords1.equals(cords2)
        assertNotSame(coords1, coords2, "getCoordinates should return a clone, not the internal array instance.");
        assertArrayEquals(coords1, coords2, 0.001, "Cloned arrays should have the same content.");

        // changing recieved array should not change internal position
        coords1[0] = 999.9;
        coords2 = road.getCoordinates();
        assertNotEquals(coords1[0], coords2[0], "Modifying the returned array should not affect the road's internal state.");
    }


    @Test
    void toStringOutputsCorrectString() {
        when(mockPositionA.getId()).thenReturn(123);
        when(mockPositionB.getId()).thenReturn(456);

        when(mockPositionA.getCoordinates()).thenReturn(new double[]{2.0, 4.0});
        when(mockPositionB.getCoordinates()).thenReturn(new double[]{6.0, 8.0});
        road.setCoordinatesAndRotationAngle();

        String toString = road.toString();
        String expectedString = "Road:{owner: null; (123, 456); position: ([4.0, 6.0]); angle: -2.356194}";
        assertEquals(expectedString, toString, "String does not match");
    }

    @Test
    void testSetOwnerPlayerId() throws GameException {
        var player = new Player("Player1");
        road.setOwner(player);
        assertEquals(player, road.getOwner());
    }

    @Test
    void setOwnerPlayerShouldThrowErrorIfRoadIsAlreadyPlaced() throws GameException {
        var player = new Player("Player1");
        road.setOwner(player);
        GameException ge = assertThrows(GameException.class, () -> road.setOwner(player));
        assertEquals("Road cannot be placed twice: roadId = %s, playerId = %s".formatted(road.id, player.getUniqueId()), ge.getMessage());
    }

    @Test
    void setOwnerPlayerShouldThrowErrorIfOwnerIsNull() {
        GameException ge = assertThrows(GameException.class, () -> road.setOwner(null));
        assertEquals("Owner of road must not be null: roadId = %s".formatted(road.id), ge.getMessage());
    }

    @Test
    void testSetAndGetColor() throws GameException {
        PlayerColor expectedColor = PlayerColor.BLUE;
        road.setColor(expectedColor);
        assertEquals(expectedColor, road.getColor());
    }

    @Test
    void setColorShouldThrowErrorIfRoadIsAlreadyPlaced() throws GameException {
        var firstColor = PlayerColor.BLUE;
        var secondColor = PlayerColor.RED;
        road.setColor(firstColor);
        GameException ge = assertThrows(GameException.class, () -> road.setColor(secondColor));
        assertEquals("Color of road cannot be changed twice: roadId = %s, color = %s".formatted(road.getId(), secondColor.getHexCode()), ge.getMessage());
    }

    @Test
    void testToJsonInitialState() {
        ObjectNode jsonNode = road.toJson();

        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        // Check ID
        assertTrue(jsonNode.has("id"), "JSON should contain 'id' field");
        assertEquals(ROAD_ID, jsonNode.get("id").asInt(), "ID should match the initial ID in JSON");

        // Check Owner (should be JSON null)
        assertTrue(jsonNode.has("owner"), "JSON should contain 'owner' field");
        assertTrue(jsonNode.get("owner").isNull(), "Initial owner should be null in JSON");

        // Check Coordinates
        assertTrue(jsonNode.has("coordinates"), "JSON should contain 'coordinates' field");
        assertTrue(jsonNode.get("coordinates").isArray(), "Coordinates should be a JSON array");
        ArrayNode coordsArray = (ArrayNode) jsonNode.get("coordinates");
        assertEquals(2, coordsArray.size(), "Coordinates array should have 2 elements");
        assertEquals(0.0, coordsArray.get(0).asDouble(), 0.0001, "Initial X coordinate should be 0.0 in JSON");
        assertEquals(0.0, coordsArray.get(1).asDouble(), 0.0001, "Initial Y coordinate should be 0.0 in JSON");

        // Check Angle
        assertTrue(jsonNode.has("rotationAngle"), "JSON should contain 'rotationAngle' field");
        assertEquals(0.0, jsonNode.get("rotationAngle").asDouble(), 0.0001, "Initial rotationAngle should be 0.0 in JSON");
    }

    @Test
    void testToJsonWithOwner() throws GameException {
        var expectedOwner = new Player("PlayerX");
        road.setOwner(expectedOwner);

        ObjectNode jsonNode = road.toJson();

        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        // Check ID
        assertEquals(ROAD_ID, jsonNode.get("id").asInt());

        // Check Owner (should be the player ID string)
        assertTrue(jsonNode.has("owner"), "JSON should contain 'owner' field");
        assertFalse(jsonNode.get("owner").isNull(), "Owner should not be null after setting");
        assertEquals(expectedOwner.getUniqueId(), jsonNode.get("owner").asText(), "Owner should match the set player ID in JSON");

        assertTrue(jsonNode.get("color").isNull(), "Color should be null");

        // Check Coordinates (should still be initial)
        assertTrue(jsonNode.has("coordinates"), "JSON should contain 'coordinates' field");
        ArrayNode coordsArray = (ArrayNode) jsonNode.get("coordinates");
        assertEquals(0.0, coordsArray.get(0).asDouble(), 0.0001);
        assertEquals(0.0, coordsArray.get(1).asDouble(), 0.0001);

        // Check Angle (should still be initial)
        assertTrue(jsonNode.has("rotationAngle"), "JSON should contain 'rotationAngle' field");
        assertEquals(0.0, jsonNode.get("rotationAngle").asDouble(), 0.0001);
    }

    @Test
    void testToJsonWithColor() throws GameException {
        PlayerColor expectedColor = PlayerColor.LAVENDER;
        road.setColor(expectedColor);

        ObjectNode jsonNode = road.toJson();
        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        assertEquals(ROAD_ID, jsonNode.get("id").asInt());
        assertEquals(expectedColor.getHexCode(), jsonNode.get("color").asText(), "Color should match the hex code of the color in JSON\"");
    }

    @Test
    void testToJsonAfterCoordinateCalculationAndOwner() throws GameException {
        var expectedOwner = new Player("PlayerX");
        PlayerColor expectedColor = PlayerColor.LAVENDER;

        // Mock getCoordinates from SettlementPosition for calculation
        double[] coordsA = {5.0, -10.0};
        double[] coordsB = {15.0, 10.0};
        when(mockPositionA.getCoordinates()).thenReturn(coordsA);
        when(mockPositionB.getCoordinates()).thenReturn(coordsB);

        // Set owner and calculate coordinates/angle
        road.setOwner(expectedOwner);
        road.setColor(expectedColor);
        road.setCoordinatesAndRotationAngle();

        // Expected calculated values
        double expectedX = (coordsA[0] + coordsB[0]) / 2.0; // (5 + 15) / 2 = 10.0
        double expectedY = (coordsA[1] + coordsB[1]) / 2.0; // (-10 + 10) / 2 = 0.0
        double angleX = coordsA[0] - coordsB[0]; // 5 - 15 = -10
        double angleY = coordsA[1] - coordsB[1]; // -10 - 10 = -20
        double expectedAngle = StrictMath.atan2(angleY, angleX); // atan2(-20, -10)

        // Call toJson
        ObjectNode jsonNode = road.toJson();

        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        // Check ID
        assertEquals(ROAD_ID, jsonNode.get("id").asInt());

        // Check Owner
        assertTrue(jsonNode.has("owner"), "JSON should contain 'owner' field");
        assertEquals(expectedOwner.getUniqueId(), jsonNode.get("owner").asText(), "Owner should match the set player ID in JSON");

        assertTrue(jsonNode.has("color"), "JSON should contain 'color' field");
        assertEquals(expectedColor.getHexCode(), jsonNode.get("color").asText(), "Color should match the hex code of the color in JSON");

        // Check Coordinates
        assertTrue(jsonNode.has("coordinates"), "JSON should contain 'coordinates' field");
        assertTrue(jsonNode.get("coordinates").isArray(), "Coordinates should be a JSON array");
        ArrayNode coordsArray = (ArrayNode) jsonNode.get("coordinates");
        assertEquals(2, coordsArray.size(), "Coordinates array should have 2 elements");
        assertEquals(expectedX, coordsArray.get(0).asDouble(), 0.0001, "Calculated X coordinate should be correct in JSON");
        assertEquals(expectedY, coordsArray.get(1).asDouble(), 0.0001, "Calculated Y coordinate should be correct in JSON");

        // Check Angle
        assertTrue(jsonNode.has("rotationAngle"), "JSON should contain 'rotationAngle' field");
        assertEquals(expectedAngle, jsonNode.get("rotationAngle").asDouble(), 0.0001, "Calculated rotationAngle should be correct in JSON");
    }
}
