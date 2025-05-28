package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.game.board.BuildingSite;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TileTest {
    private Tile tile;
    private final TileType testType = TileType.WHEAT;

    @BeforeEach
    void setUp() {
        tile = new Tile(testType);
    }

    @Test
    void testConstructorSetsTypeCorrectly() {
        assertEquals(testType, tile.getType(), "Tile type should match the one passed in constructor");
    }

    @Test
    void testConstructorInitializesCoordinatesAT00SoTheyCanBeChangedLater() {
        assertArrayEquals(new double[]{0.0, 0.0}, tile.getCoordinates(), "Initial coordinates should be [0.0, 0.0]");
    }

    @Test
    void testTypeGetter() {
        assertSame(testType, tile.getType(), "getType() should return the initially set type instance");
    }

    @Test
    void testValueGetter() {
        assertEquals(tile.value, tile.getValue(), "getValue() return value");
    }
    @Test
    void testIdGetter() {
        assertEquals(tile.id, tile.getId(), "getId() doesn't return id");
    }


    @Test
    void testSetValueInitializesValueCorrectly(){
        assertEquals(0, tile.value, "value should be initialized at 0");
        tile.setValue(5);
        assertEquals(5, tile.getValue(), "value should update the first time");
        assertEquals(5, tile.value, "internal value should match");
    }

    @Test
    void testSetValueDoesNotUpdateIfAlreadySet(){
        tile.setValue(8);
        assertEquals(8, tile.getValue(), "Value should be set initially");

        tile.setValue(10);
        assertEquals(8, tile.getValue(), "Value should not update the second time");
        assertEquals(8, tile.value, "Internal value should remain unchanged");
    }
    @Test
    void testIdSetterInitializesValueCorrectly(){
        assertEquals(0, tile.id, "id should be initialized at 0");
        tile.setId(12);
        assertEquals(12, tile.id, "id should update the first time");
        tile.setId(100);
        assertEquals(12, tile.id, "id should not update the second time");
    }

    @Test
    void testSetCoordinatesFirstTime() {
        double expectedX = 10.5;
        double expectedY = -20.1;
        tile.setCoordinates(expectedX, expectedY);
        assertArrayEquals(new double[]{expectedX, expectedY}, tile.getCoordinates(), 0.0001,
                "Coordinates should be updated when set for the first time");
    }

    @Test
    void testSetCoordinatesSecondTime() {
        double initialX = 5.0;
        double initialY = 8.0;
        // Set coordinates for the first time
        tile.setCoordinates(initialX, initialY);

        // Attempt to set coordinates again
        tile.setCoordinates(100.0, 200.0);

        // Verify coordinates did NOT change
        assertArrayEquals(new double[]{initialX, initialY}, tile.getCoordinates(), 0.0001,
                "Coordinates should not be updated if already set to non-zero values");
    }


    @Test
    void testGetCoordinates() {
        assertArrayEquals(new double[]{0.0, 0.0}, tile.getCoordinates(), "Should return initial coordinates");

        tile.setCoordinates(15.0, 25.0);
        assertArrayEquals(new double[]{15.0, 25.0}, tile.getCoordinates(), 0.0001, "Should return updated coordinates");

        double[] internalCoords = tile.getCoordinates();
        internalCoords[0] = 99.0; // Modify returned array
        assertArrayEquals(new double[]{15.0, 25.0}, tile.getCoordinates(), 0.0001, "Can't modify coordinates without resetting");
    }

    @Test
    void testToStringAfterSetCoordinates() {
        double x = 7.8;
        double y = -9.1;
        int id = 12;
        tile.setCoordinates(x, y);
        tile.setId(id);
        String expected = String.format("Tile{id=%d,coordinates=(%f, %f)}", id, x, y);
        assertEquals(expected, tile.toString(), "toString() format should be correct");
    }


    @Test
    void testToJson() {
        ObjectNode jsonNode = tile.toJson();

        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        // Check ID
        assertTrue(jsonNode.has("id"), "JSON should contain 'id' field");
        assertEquals(0, jsonNode.get("id").asInt(), "Initial ID should be 0 in JSON");

        // Check Type
        assertTrue(jsonNode.has("type"), "JSON should contain 'type' field");
        assertEquals(testType.name(), jsonNode.get("type").asText(), "Type should match the initial type name in JSON");

        // Check Value
        assertTrue(jsonNode.has("value"), "JSON should contain 'value' field");
        assertEquals(0, jsonNode.get("value").asInt(), "Initial value should be 0 in JSON");

        // Check Coordinates
        assertTrue(jsonNode.has("coordinates"), "JSON should contain 'coordinates' field");
        assertTrue(jsonNode.get("coordinates").isArray(), "Coordinates should be a JSON array");


        ArrayNode coordsArray = (ArrayNode) jsonNode.get("coordinates");
        assertEquals(2, coordsArray.size(), "Coordinates array should have 2 elements");
        assertEquals(0.0, coordsArray.get(0).asDouble(), 0.0001, "Initial X coordinate should be 0.0 in JSON");
        assertEquals(0.0, coordsArray.get(1).asDouble(), 0.0001, "Initial Y coordinate should be 0.0 in JSON");
    }

    @Test
    void testToJsonAfterSetup() {
        int expectedId = 5;
        int expectedValue = 8;
        double expectedX = 12.3;
        double expectedY = -4.5;

        tile.setId(expectedId);
        tile.setValue(expectedValue);
        tile.setCoordinates(expectedX, expectedY);

        ObjectNode jsonNode = tile.toJson();

        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        // Check ID
        assertTrue(jsonNode.has("id"), "JSON should contain 'id' field");
        assertEquals(expectedId, jsonNode.get("id").asInt(), "ID should match the set ID in JSON");

        // Check Type
        assertTrue(jsonNode.has("type"), "JSON should contain 'type' field");
        assertEquals(testType.name(), jsonNode.get("type").asText(), "Type should match the initial type name in JSON");

        // Check Value
        assertTrue(jsonNode.has("value"), "JSON should contain 'value' field");
        assertEquals(expectedValue, jsonNode.get("value").asInt(), "Value should match the set value in JSON");

        // Check Coordinates
        assertTrue(jsonNode.has("coordinates"), "JSON should contain 'coordinates' field");
        assertTrue(jsonNode.get("coordinates").isArray(), "Coordinates should be a JSON array");
        ArrayNode coordsArray = (ArrayNode) jsonNode.get("coordinates");

        assertEquals(2, coordsArray.size(), "Coordinates array should have 2 elements");
        assertEquals(expectedX, coordsArray.get(0).asDouble(), 0.0001, "X coordinate should match the set value in JSON");
        assertEquals(expectedY, coordsArray.get(1).asDouble(), 0.0001, "Y coordinate should match the set value in JSON");
    }

    @Test
    void addSubscriberAddsSettlementPositionToList() {
        BuildingSite mockSettlement1 = mock(BuildingSite.class);
        BuildingSite mockSettlement2 = mock(BuildingSite.class);

        tile.addSubscriber(mockSettlement1);
        assertTrue(tile.buildingSitesOfTile.contains(mockSettlement1), "Subscriber list should contain added settlement 1.");
        assertEquals(1, tile.buildingSitesOfTile.size(), "Subscriber list size should be 1.");

        tile.addSubscriber(mockSettlement2);
        assertTrue(tile.buildingSitesOfTile.contains(mockSettlement2), "Subscriber list should contain added settlement 2.");
        assertEquals(2, tile.buildingSitesOfTile.size(), "Subscriber list size should be 2.");
    }

    @Test
    void removeSubscriberRemovesSettlementPositionFromList() {
        BuildingSite mockSettlement1 = mock(BuildingSite.class);
        BuildingSite mockSettlement2 = mock(BuildingSite.class);

        tile.addSubscriber(mockSettlement1);
        tile.addSubscriber(mockSettlement2);
        assertEquals(2, tile.buildingSitesOfTile.size(), "Subscriber list size should be 2.");

        tile.removeSubscriber(mockSettlement1);
        assertFalse(tile.buildingSitesOfTile.contains(mockSettlement1), "Subscriber list should not contain removed settlement 1.");
        assertTrue(tile.buildingSitesOfTile.contains(mockSettlement2), "Subscriber list should still contain settlement 2.");
        assertEquals(1, tile.buildingSitesOfTile.size(), "Subscriber list size should be 1 after removal.");

        tile.removeSubscriber(mockSettlement2);
        assertFalse(tile.buildingSitesOfTile.contains(mockSettlement2), "Subscriber list should not contain removed settlement 2.");
        assertTrue(tile.buildingSitesOfTile.isEmpty(), "Subscriber list should be empty after removing all subscribers.");
    }

    @Test
    void removeSubscriberDoesNothingIfSubscriberNotInList() {
        BuildingSite mockSettlement1 = mock(BuildingSite.class);
        BuildingSite mockSettlementNotInList = mock(BuildingSite.class);

        tile.addSubscriber(mockSettlement1);
        assertEquals(1, tile.buildingSitesOfTile.size());

        assertDoesNotThrow(() -> tile.removeSubscriber(mockSettlementNotInList));
        assertEquals(1, tile.buildingSitesOfTile.size(), "List size should not change when removing a non-existent subscriber.");
        assertTrue(tile.buildingSitesOfTile.contains(mockSettlement1), "Original subscriber should still be in the list.");
    }


    @Test
    void notifySubscribersCallsUpdateOnAllSubscribedSettlements() {
        BuildingSite mockSettlement1 = mock(BuildingSite.class);
        BuildingSite mockSettlement2 = mock(BuildingSite.class);

        tile.addSubscriber(mockSettlement1);
        tile.addSubscriber(mockSettlement2);

        TileType notificationType = TileType.WOOD;
        tile.notifySubscribers(notificationType);

        verify(mockSettlement1, times(1)).update(notificationType);
        verify(mockSettlement2, times(1)).update(notificationType);
    }

    @Test
    void notifySubscribersDoesNothingIfNoSubscribers() {
        TileType notificationType = TileType.WHEAT;

        assertDoesNotThrow(() -> tile.notifySubscribers(notificationType),
                "notifySubscribers should not throw an error if there are no subscribers.");
    }

    @Test
    void updateTileWithValueMatchingNotificationNotifiesSubscribers() {
        int tileValue = 9;
        tile.setValue(tileValue);

        BuildingSite mockSettlement = mock(BuildingSite.class);
        tile.addSubscriber(mockSettlement);

        tile.update(tileValue);


        verify(mockSettlement, times(1)).update(testType); // testType is TileType.WHEAT (tile.getType())
    }

    @Test
    void updateTileWithValueNotMatchingNotificationDoesNotNotifySubscribers() {
        int tileValue = 9;
        int nonMatchingNotificationValue = 8;
        tile.setValue(tileValue);

        BuildingSite mockSettlement = mock(BuildingSite.class);
        tile.addSubscriber(mockSettlement);

        tile.update(nonMatchingNotificationValue);

        verify(mockSettlement, never()).update(any(TileType.class));
    }

    @Test
    void updateTileWithNoSubscribersAndMatchingValueDoesNotThrowError() {
        int tileValue = 9;
        tile.setValue(tileValue);

        assertDoesNotThrow(() -> tile.update(tileValue),
                "Updating tile with matching value and no subscribers should not throw an error.");
    }

}
