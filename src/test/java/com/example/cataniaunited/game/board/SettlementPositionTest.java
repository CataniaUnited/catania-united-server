package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.IntersectionOccupiedException;
import com.example.cataniaunited.exception.ui.NoAdjacentRoadException;
import com.example.cataniaunited.exception.ui.SpacingRuleViolationException;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.buildings.Building;
import com.example.cataniaunited.game.buildings.Settlement;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class SettlementPositionTest {
    private static final int STANDARD_ID = 42;


    private Road mockRoad1;
    private Road mockRoad2;
    private Road mockRoad3;

    private Tile mockTile1;
    private Tile mockTile2;
    private Tile mockTile3;


    private SettlementPosition mockNeighbour1;
    private SettlementPosition mockNeighbour2;
    private SettlementPosition mockNeighbour3;


    private SettlementPosition settlementPosition;

    @BeforeEach
    void setUp() {
        //Mock all frequently used classes
        mockRoad1 = mock(Road.class);
        mockRoad2 = mock(Road.class);
        mockRoad3 = mock(Road.class);
        mockTile1 = mock(Tile.class);
        mockTile2 = mock(Tile.class);
        mockTile3 = mock(Tile.class);
        mockNeighbour1 = mock(SettlementPosition.class);
        mockNeighbour2 = mock(SettlementPosition.class);
        mockNeighbour3 = mock(SettlementPosition.class);

        settlementPosition = new SettlementPosition(STANDARD_ID);
    }

    @Test
    void testConstructorInitialization() {
        assertEquals(STANDARD_ID, settlementPosition.getId(), "ID should be set by constructor");
        assertArrayEquals(new double[]{0.0, 0.0}, settlementPosition.getCoordinates(), 0.001, "Initial coordinates should be [0.0, 0.0]");
        assertTrue(settlementPosition.getTiles().isEmpty(), "Initial tiles list should be empty");
        assertTrue(settlementPosition.getRoads().isEmpty(), "Initial roads list should be empty");
    }

    @Test
    void getIdReturnsCorrectId() {
        assertEquals(STANDARD_ID, settlementPosition.getId());
    }

    @Test
    void addTileSuccessfullyAddsUpToThreeTiles() {
        settlementPosition.addTile(mockTile1);
        settlementPosition.addTile(mockTile2);
        settlementPosition.addTile(mockTile3);
        List<Tile> tiles = settlementPosition.getTiles();
        assertEquals(3, tiles.size(), "Should have 3 tiles after adding three");
        assertTrue(tiles.containsAll(List.of(mockTile1, mockTile2, mockTile3)), "List should contain all added tiles");
    }

    @Test
    void addTileDoesNotAddDuplicateTileInstance() {
        settlementPosition.addTile(mockTile1);
        assertEquals(1, settlementPosition.getTiles().size(), "Size should be 1 after first add");
        settlementPosition.addTile(mockTile1);

        assertEquals(1, settlementPosition.getTiles().size(), "List should not have changed");
        assertTrue(settlementPosition.getTiles().contains(mockTile1), "List should still contain the original tile");
    }

    @Test
    void addTileThrowsExceptionWhenAddingFourthTile() {
        Tile mockTile4 = mock(Tile.class);

        settlementPosition.addTile(mockTile1);
        settlementPosition.addTile(mockTile2);
        settlementPosition.addTile(mockTile3);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> settlementPosition.addTile(mockTile4), "Should throw IllegalStateException when adding 4th tile");

        assertTrue(exception.getMessage().contains("Cannot assign more than 3 Tiles"), "Exception message should be correct");
        assertEquals(3, settlementPosition.getTiles().size(), "Tile count should remain 3 after exception");
    }

    @Test
    void cantChangeTileListWhenReceivedFromGetterBecauseImmutable() {
        settlementPosition.addTile(mockTile1);
        List<Tile> tiles = settlementPosition.getTiles();

        assertThrows(UnsupportedOperationException.class, () -> tiles.add(mockTile2), "Returned tile list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> tiles.remove(mockTile1), "Returned tile list should be immutable");
    }

    @Test
    void addRoadSuccessfullyAddsUpToThreeRoads() {
        settlementPosition.addRoad(mockRoad1);
        settlementPosition.addRoad(mockRoad2);
        settlementPosition.addRoad(mockRoad3);
        List<Road> roads = settlementPosition.getRoads();
        assertEquals(3, roads.size(), "Should have 3 roads after adding three");
        assertTrue(roads.containsAll(List.of(mockRoad1, mockRoad2, mockRoad3)), "List should contain all added roads");
    }

    @Test
    void addRoadThrowsExceptionWhenAddingFourthRoad() {
        Road mockRoad4 = mock(Road.class);

        settlementPosition.addRoad(mockRoad1);
        settlementPosition.addRoad(mockRoad2);
        settlementPosition.addRoad(mockRoad3);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> settlementPosition.addRoad(mockRoad4), "Should throw IllegalStateException when adding 4th road");

        assertTrue(exception.getMessage().contains("Cannot connect more than 3 Roads"), "Exception message should be correct");
        assertEquals(3, settlementPosition.getRoads().size(), "Road count should remain 3 after exception");
    }

    @Test
    void addRoadDoesNotAddDuplicateRoadInstance() {
        settlementPosition.addRoad(mockRoad1);
        assertEquals(1, settlementPosition.getRoads().size(), "Size should be 1 after first add");
        settlementPosition.addRoad(mockRoad1);

        assertEquals(1, settlementPosition.getRoads().size(), "List should not have changed");
        assertTrue(settlementPosition.getRoads().contains(mockRoad1), "List should still contain the original road");
    }

    @Test
    void cantChangeRoadListWhenRecievedFromGetterBecauseImmutable() {
        settlementPosition.addRoad(mockRoad1);
        List<Road> roads = settlementPosition.getRoads();

        assertThrows(UnsupportedOperationException.class, () -> roads.add(mockRoad2), "Returned road list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> roads.remove(mockRoad1), "Returned road list should be immutable");
    }

    @Test
    void setCoordinatesSetsValuesCorrectlyWhenCalledForTheFirstTime() {
        settlementPosition.setCoordinates(10, 20);
        assertArrayEquals(new double[]{10, 20}, settlementPosition.getCoordinates(), 0.001, "Coordinates should be set");
    }

    @Test
    void setCoordinatesDoesNotChangeValuesWhenCalledAgain() {
        settlementPosition.setCoordinates(10, 20);
        settlementPosition.setCoordinates(100, 200);
        assertArrayEquals(new double[]{10, 20}, settlementPosition.getCoordinates(), 0.001, "Coordinates should remain the values from the first call\"");
    }


    @Test
    void getCoordinatesReturnsCloneNotSameArray() {
        settlementPosition.setCoordinates(10, 20);
        double[] coords1 = settlementPosition.getCoordinates();
        double[] coords2 = settlementPosition.getCoordinates();

        // coords1 != coords2, cords1.equals(cords2)
        assertNotSame(coords1, coords2, "getCoordinates should return a clone");
        assertArrayEquals(coords1, coords2, 0.001, "Cloned arrays should have same content");

        // Modify the returned array should not change internal position
        coords1[0] = 999.9;
        coords2 = settlementPosition.getCoordinates();

        assertNotEquals(coords1[0], coords2[0], "Modifying clone should not affect internal state");
    }


    @Test
    void getNeighboursReturnsCorrectNeighboursFromRoads() {
        when(mockRoad1.getNeighbour(settlementPosition)).thenReturn(mockNeighbour1);
        when(mockRoad2.getNeighbour(settlementPosition)).thenReturn(mockNeighbour2);
        when(mockRoad3.getNeighbour(settlementPosition)).thenReturn(mockNeighbour3);

        settlementPosition.addRoad(mockRoad1);
        settlementPosition.addRoad(mockRoad2);
        settlementPosition.addRoad(mockRoad3);


        List<SettlementPosition> neighbours = settlementPosition.getNeighbours();


        assertEquals(3, neighbours.size(), "Should return 3 neighbours");
        assertTrue(neighbours.containsAll(List.of(mockNeighbour1, mockNeighbour2, mockNeighbour3)), "List should contain all expected mock neighbours");

        // Verify that getNeighbour was called on each road exactly once with the correct argument
        verify(mockRoad1, times(1)).getNeighbour(settlementPosition);
        verify(mockRoad2, times(1)).getNeighbour(settlementPosition);
        verify(mockRoad3, times(1)).getNeighbour(settlementPosition);
    }

    @Test
    void toStringOutputsCorrectString() {
        settlementPosition.setCoordinates(10, 20);
        settlementPosition.addTile(mockTile1);
        settlementPosition.addTile(mockTile2);
        settlementPosition.addTile(mockTile3);
        settlementPosition.addRoad(mockRoad1);
        settlementPosition.addRoad(mockRoad2);
        settlementPosition.addRoad(mockRoad3);


        when(mockTile1.toString()).thenReturn("MockTile1");
        when(mockTile2.toString()).thenReturn("MockTile2");
        when(mockTile3.toString()).thenReturn("MockTile3");
        when(mockRoad1.toString()).thenReturn("MockRoad1");
        when(mockRoad2.toString()).thenReturn("MockRoad2");
        when(mockRoad3.toString()).thenReturn("MockRoad3");

        String toString = settlementPosition.toString();
        String expectedString = "SettlementPosition{ID='42', (10.0; 20.0), tiles=[MockTile1, MockTile2, MockTile3], roads=[MockRoad1, MockRoad2, MockRoad3]}";
        assertEquals(expectedString, toString, "String does not match");
    }

    @Test
    void testSetBuilding() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockRoad1.getOwner()).thenReturn(mockPlayer);
        when(mockRoad1.getNeighbour(any(SettlementPosition.class))).thenReturn(mockNeighbour1);
        settlementPosition.addRoad(mockRoad1);
        Settlement settlement = new Settlement(mockPlayer, PlayerColor.BLUE);
        settlementPosition.setBuilding(settlement);
        assertEquals(settlement, settlementPosition.building);
        assertEquals(playerId, settlementPosition.getBuildingOwner().getUniqueId());
    }

    @Test
    void setBuildingShouldThrowErrorOnPlayerMismatch() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        Settlement settlement1 = new Settlement(mockPlayer, PlayerColor.BLUE);
        when(mockRoad1.getOwner()).thenReturn(mockPlayer);
        when(mockRoad1.getNeighbour(any(SettlementPosition.class))).thenReturn(mockNeighbour1);
        settlementPosition.addRoad(mockRoad1);
        settlementPosition.setBuilding(settlement1);
        assertEquals(settlement1, settlementPosition.building);

        String secondPlayerId = "Player2";
        Player mockPlayer2 = mock(Player.class);
        when(mockPlayer2.getUniqueId()).thenReturn(secondPlayerId);
        Settlement settlement2 = new Settlement(mockPlayer2, PlayerColor.BLUE);
        GameException ge = assertThrows(IntersectionOccupiedException.class, () -> settlementPosition.setBuilding(settlement2));
        assertEquals("Intersection occupied!", ge.getMessage());
    }

    @Test
    void setBuildingShouldThrowErrorWhenSpacingRuleGetsViolated() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockRoad1.getOwner()).thenReturn(mockPlayer);
        when(mockRoad1.getNeighbour(any(SettlementPosition.class))).thenReturn(mockNeighbour1);
        when(mockNeighbour1.getBuildingOwner()).thenReturn(mockPlayer);
        settlementPosition.addRoad(mockRoad1);

        String secondPlayerId = "Player2";
        Player mockPlayer2 = mock(Player.class);
        when(mockPlayer2.getUniqueId()).thenReturn(secondPlayerId);
        Settlement settlement = new Settlement(mockPlayer2, PlayerColor.BLUE);
        GameException ge = assertThrows(SpacingRuleViolationException.class, () -> settlementPosition.setBuilding(settlement));
        assertEquals("Too close to another settlement or city", ge.getMessage());
    }

    @Test
    void setBuildingShouldThrowErrorWhenPlayerHasNoAdjacentRoad() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        Settlement settlement = new Settlement(mockPlayer, PlayerColor.BLUE);
        GameException ge = assertThrows(NoAdjacentRoadException.class, () -> settlementPosition.setBuilding(settlement));
        assertEquals("No adjacent roads found", ge.getMessage());
    }

    @Test
    void setBuildingShouldThrowErrorWhenOnlyAnotherPlayerHasAdjacentRoad() throws GameException {
        var player = new Player("Player1");
        when(mockRoad1.getOwner()).thenReturn(player);
        when(mockRoad1.getNeighbour(any(SettlementPosition.class))).thenReturn(mockNeighbour1);
        settlementPosition.addRoad(mockRoad1);

        String secondPlayerId = "Player2";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(secondPlayerId);
        Settlement settlement = new Settlement(mockPlayer, PlayerColor.BLUE);
        GameException ge = assertThrows(NoAdjacentRoadException.class, () -> settlementPosition.setBuilding(settlement));
        assertEquals("No adjacent roads found", ge.getMessage());
    }

    @Test
    void setBuildingShouldWorkIfTwoPlayersHaveAdjacentRoads() throws GameException {
        var player = new Player("Player1");
        var secondPlayer = new Player("Player2");
        when(mockRoad1.getOwner()).thenReturn(player);
        when(mockRoad1.getNeighbour(any(SettlementPosition.class))).thenReturn(mockNeighbour1);
        when(mockRoad2.getOwner()).thenReturn(secondPlayer);
        when(mockRoad2.getNeighbour(any(SettlementPosition.class))).thenReturn(mockNeighbour2);
        settlementPosition.addRoad(mockRoad1);
        settlementPosition.addRoad(mockRoad2);

        Settlement settlement = new Settlement(secondPlayer, PlayerColor.BLUE);
        settlementPosition.setBuilding(settlement);
        assertEquals(settlement, settlementPosition.building);
        assertEquals(secondPlayer, settlementPosition.getBuildingOwner());
    }

    @Test
    void testToJsonInitialState() {

        ObjectNode jsonNode = settlementPosition.toJson();

        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        // Check ID
        assertTrue(jsonNode.has("id"), "JSON should contain 'id' field");
        assertEquals(STANDARD_ID, jsonNode.get("id").asInt(), "ID should match the initial ID in JSON");

        // Check Building (should be the string "null")
        assertTrue(jsonNode.has("building"), "JSON should contain 'building' field");
        // String.valueOf(null) results in the string "null"
        assertEquals("null", jsonNode.get("building").asText(), "Initial building should be represented as 'null' string in JSON");

        // Check Coordinates
        assertTrue(jsonNode.has("coordinates"), "JSON should contain 'coordinates' field");
        assertTrue(jsonNode.get("coordinates").isArray(), "Coordinates should be a JSON array");
        ArrayNode coordsArray = (ArrayNode) jsonNode.get("coordinates");
        assertEquals(2, coordsArray.size(), "Coordinates array should have 2 elements");
        assertEquals(0.0, coordsArray.get(0).asDouble(), 0.0001, "Initial X coordinate should be 0.0 in JSON");
        assertEquals(0.0, coordsArray.get(1).asDouble(), 0.0001, "Initial Y coordinate should be 0.0 in JSON");

        // Check that tiles and roads are NOT included
        assertFalse(jsonNode.has("tiles"), "JSON should not contain 'tiles' field");
        assertFalse(jsonNode.has("roads"), "JSON should not contain 'roads' field");
    }

    @Test
    void testToJsonWithCoordinatesAndBuildingSet() throws GameException {
        double expectedX = -25.5;
        double expectedY = 100.1;
        String buildingOwner = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(buildingOwner);
        PlayerColor color = PlayerColor.LIGHT_ORANGE;

        // Use a mock Building
        Building mockBuilding = new Settlement(mockPlayer, color);
        when(mockRoad1.getOwner()).thenReturn(mockPlayer);
        when(mockRoad1.getNeighbour(any(SettlementPosition.class))).thenReturn(mockNeighbour1);
        settlementPosition.addRoad(mockRoad1);

        settlementPosition.setCoordinates(expectedX, expectedY);
        settlementPosition.setBuilding(mockBuilding);


        ObjectNode jsonNode = settlementPosition.toJson();

        assertNotNull(jsonNode, "toJson() should return a non-null ObjectNode");

        // Check ID
        assertTrue(jsonNode.has("id"), "JSON should contain 'id' field");
        assertEquals(STANDARD_ID, jsonNode.get("id").asInt(), "ID should match the initial ID in JSON");

        // Check Building (should be the string from building.toString())
        assertTrue(jsonNode.has("building"), "JSON should contain 'building' field");
        assertEquals(mockBuilding.toJson().asText(), jsonNode.get("building").asText(), "Building string should match the building's toString() output");

        // Check Coordinates
        assertTrue(jsonNode.has("coordinates"), "JSON should contain 'coordinates' field");
        assertTrue(jsonNode.get("coordinates").isArray(), "Coordinates should be a JSON array");
        ArrayNode coordsArray = (ArrayNode) jsonNode.get("coordinates");
        assertEquals(2, coordsArray.size(), "Coordinates array should have 2 elements");
        assertEquals(expectedX, coordsArray.get(0).asDouble(), 0.0001, "X coordinate should match the set value in JSON");
        assertEquals(expectedY, coordsArray.get(1).asDouble(), 0.0001, "Y coordinate should match the set value in JSON");

        // Check that tiles and roads are NOT included
        assertFalse(jsonNode.has("tiles"), "JSON should not contain 'tiles' field");
        assertFalse(jsonNode.has("roads"), "JSON should not contain 'roads' field");
    }

    @Test
    void getBuildingOwnerNoBuilding() {
        assertNull(settlementPosition.getBuildingOwner(), "Building owner should be null when no building is present.");
    }

    @Test
    void getNeighboursNoRoads() {
        List<SettlementPosition> neighbours = settlementPosition.getNeighbours();
        assertTrue(neighbours.isEmpty(), "Neighbours list should be empty when there are no roads.");
    }

    @Test
    void getNeighboursWithOneNeighbour() {
        when(mockRoad1.getNeighbour(settlementPosition)).thenReturn(mockNeighbour1);
        settlementPosition.addRoad(mockRoad1);

        List<SettlementPosition> neighbours = settlementPosition.getNeighbours();

        assertEquals(1, neighbours.size(), "Should return 1 neighbour.");
        assertTrue(neighbours.contains(mockNeighbour1), "List should contain the expected mock neighbour.");
        verify(mockRoad1, times(1)).getNeighbour(settlementPosition);
    }

    @Test
    void update_whenBuildingIsNull_doesNothing() {
        assertNull(settlementPosition.building, "Building needs tio be null.");

        settlementPosition.update(TileType.WHEAT);

        assertDoesNotThrow(() -> settlementPosition.update(TileType.WHEAT), "Update should not throw an exception when building is null.");

        assertNull(settlementPosition.building, "Building should still be null after update call.");
    }

    @Test
    void update_whenBuildingExists_distributesResources() throws GameException {
        String playerId = "test";
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        settlementPosition.addRoad(mockRoad1);
        when(mockRoad1.getOwner()).thenReturn(player);

        when(mockRoad1.getNeighbour(settlementPosition)).thenReturn(mockNeighbour1);
        when(mockNeighbour1.getBuildingOwner()).thenReturn(null);

        Building mockBuilding = mock(Building.class);
        when(mockBuilding.getPlayer()).thenReturn(player); // Building's owner

        settlementPosition.setBuilding(mockBuilding);
        assertEquals(mockBuilding, settlementPosition.building, "Building should be set correctly.");

        settlementPosition.update(TileType.WHEAT);

        verify(mockBuilding, times(1)).distributeResourcesToPlayer(TileType.WHEAT);
    }

    @Test
    void getPortInitiallyShouldReturnNull() {
        assertNull(settlementPosition.getPort(), "Initially, the port should be null.");
    }

    @Test
    void setPortWhenCurrentPortIsNullShouldSetThePort() {
        Port mockPort = mock(Port.class);

        assertNull(settlementPosition.getPort(), "Port should be null before setting.");

        settlementPosition.setPort(mockPort);

        assertNotNull(settlementPosition.getPort(), "Port should not be null after setting.");
        assertEquals(mockPort, settlementPosition.getPort(), "The retrieved port should be the one that was set.");
    }

    @Test
    void setPortWhenPortIsAlreadySetShouldNotChangeThePort() {
        Port initialMockPort = mock(Port.class);
        Port newMockPort = mock(Port.class);

        settlementPosition.setPort(initialMockPort);
        assertEquals(initialMockPort, settlementPosition.getPort(), "Initial port should be set correctly.");

        settlementPosition.setPort(newMockPort);

        assertEquals(initialMockPort, settlementPosition.getPort(), "Port should not have changed after attempting to set a new one when one was already present.");
        assertNotEquals(newMockPort, settlementPosition.getPort(), "The port should not be the new port instance.");
    }

    @Test
    void setPortWithNullArgumentShouldNotThrowErrorAndPortRemainsUnchanged() {
        assertNull(settlementPosition.getPort(), "Port is initially null.");
        settlementPosition.setPort(null);
        assertNull(settlementPosition.getPort(), "Port should remain null if set to null when already null.");

        Port mockPort = mock(Port.class);
        settlementPosition.setPort(mockPort);
        assertNotNull(settlementPosition.getPort());

        settlementPosition.setPort(null);
        assertNotNull(settlementPosition.getPort(), "Port should remain the originally set port, not become null.");
        assertEquals(mockPort, settlementPosition.getPort(), "Port should be the original mockPort.");
    }
}
