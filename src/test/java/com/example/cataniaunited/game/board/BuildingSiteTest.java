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
class BuildingSiteTest {
    private static final int STANDARD_ID = 42;


    private Road mockRoad1;
    private Road mockRoad2;
    private Road mockRoad3;

    private Tile mockTile1;
    private Tile mockTile2;
    private Tile mockTile3;


    private BuildingSite mockNeighbour1;
    private BuildingSite mockNeighbour2;
    private BuildingSite mockNeighbour3;


    private BuildingSite buildingSite;

    @BeforeEach
    void setUp() {
        //Mock all frequently used classes
        mockRoad1 = mock(Road.class);
        mockRoad2 = mock(Road.class);
        mockRoad3 = mock(Road.class);
        mockTile1 = mock(Tile.class);
        mockTile2 = mock(Tile.class);
        mockTile3 = mock(Tile.class);
        mockNeighbour1 = mock(BuildingSite.class);
        mockNeighbour2 = mock(BuildingSite.class);
        mockNeighbour3 = mock(BuildingSite.class);

        buildingSite = new BuildingSite(STANDARD_ID);
    }

    @Test
    void testEqualsAndHashCodeContract() {
        BuildingSite site1a = new BuildingSite(1);
        BuildingSite site1b = new BuildingSite(1);
        BuildingSite site2 = new BuildingSite(2);

        assertEquals(site1a, site1a, "A site should always be equal to itself.");
        assertEquals(site1a, site1b, "Sites with the same ID should be equal.");

        assertNotEquals(site1a, site2, "Sites with different IDs should not be equal.");
        assertNotEquals(null, site1a, "A site should not be equal to null.");
        assertNotEquals(site1a, new Object(), "A site should not be equal to an object of a different type.");
        assertEquals(site1a.hashCode(), site1b.hashCode(), "Hash codes must be the same for equal objects.");
        assertNotEquals(site1a.hashCode(), site2.hashCode(), "Hash codes should ideally be different for unequal objects.");
    }

    @Test
    void testConstructorInitialization() {
        assertEquals(STANDARD_ID, buildingSite.getId(), "ID should be set by constructor");
        assertArrayEquals(new double[]{0.0, 0.0}, buildingSite.getCoordinates(), 0.001, "Initial coordinates should be [0.0, 0.0]");
        assertTrue(buildingSite.getTiles().isEmpty(), "Initial tiles list should be empty");
        assertTrue(buildingSite.getRoads().isEmpty(), "Initial roads list should be empty");
    }

    @Test
    void getIdReturnsCorrectId() {
        assertEquals(STANDARD_ID, buildingSite.getId());
    }

    @Test
    void addTileSuccessfullyAddsUpToThreeTiles() {
        buildingSite.addTile(mockTile1);
        buildingSite.addTile(mockTile2);
        buildingSite.addTile(mockTile3);
        List<Tile> tiles = buildingSite.getTiles();
        assertEquals(3, tiles.size(), "Should have 3 tiles after adding three");
        assertTrue(tiles.containsAll(List.of(mockTile1, mockTile2, mockTile3)), "List should contain all added tiles");
    }

    @Test
    void addTileDoesNotAddDuplicateTileInstance() {
        buildingSite.addTile(mockTile1);
        assertEquals(1, buildingSite.getTiles().size(), "Size should be 1 after first add");
        buildingSite.addTile(mockTile1);

        assertEquals(1, buildingSite.getTiles().size(), "List should not have changed");
        assertTrue(buildingSite.getTiles().contains(mockTile1), "List should still contain the original tile");
    }

    @Test
    void addTileThrowsExceptionWhenAddingFourthTile() {
        Tile mockTile4 = mock(Tile.class);

        buildingSite.addTile(mockTile1);
        buildingSite.addTile(mockTile2);
        buildingSite.addTile(mockTile3);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> buildingSite.addTile(mockTile4), "Should throw IllegalStateException when adding 4th tile");

        assertTrue(exception.getMessage().contains("Cannot assign more than 3 Tiles"), "Exception message should be correct");
        assertEquals(3, buildingSite.getTiles().size(), "Tile count should remain 3 after exception");
    }

    @Test
    void cantChangeTileListWhenReceivedFromGetterBecauseImmutable() {
        buildingSite.addTile(mockTile1);
        List<Tile> tiles = buildingSite.getTiles();

        assertThrows(UnsupportedOperationException.class, () -> tiles.add(mockTile2), "Returned tile list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> tiles.remove(mockTile1), "Returned tile list should be immutable");
    }

    @Test
    void addRoadSuccessfullyAddsUpToThreeRoads() {
        buildingSite.addRoad(mockRoad1);
        buildingSite.addRoad(mockRoad2);
        buildingSite.addRoad(mockRoad3);
        List<Road> roads = buildingSite.getRoads();
        assertEquals(3, roads.size(), "Should have 3 roads after adding three");
        assertTrue(roads.containsAll(List.of(mockRoad1, mockRoad2, mockRoad3)), "List should contain all added roads");
    }

    @Test
    void addRoadThrowsExceptionWhenAddingFourthRoad() {
        Road mockRoad4 = mock(Road.class);

        buildingSite.addRoad(mockRoad1);
        buildingSite.addRoad(mockRoad2);
        buildingSite.addRoad(mockRoad3);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> buildingSite.addRoad(mockRoad4), "Should throw IllegalStateException when adding 4th road");

        assertTrue(exception.getMessage().contains("Cannot connect more than 3 Roads"), "Exception message should be correct");
        assertEquals(3, buildingSite.getRoads().size(), "Road count should remain 3 after exception");
    }

    @Test
    void addRoadDoesNotAddDuplicateRoadInstance() {
        buildingSite.addRoad(mockRoad1);
        assertEquals(1, buildingSite.getRoads().size(), "Size should be 1 after first add");
        buildingSite.addRoad(mockRoad1);

        assertEquals(1, buildingSite.getRoads().size(), "List should not have changed");
        assertTrue(buildingSite.getRoads().contains(mockRoad1), "List should still contain the original road");
    }

    @Test
    void cantChangeRoadListWhenRecievedFromGetterBecauseImmutable() {
        buildingSite.addRoad(mockRoad1);
        List<Road> roads = buildingSite.getRoads();

        assertThrows(UnsupportedOperationException.class, () -> roads.add(mockRoad2), "Returned road list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> roads.remove(mockRoad1), "Returned road list should be immutable");
    }

    @Test
    void setCoordinatesSetsValuesCorrectlyWhenCalledForTheFirstTime() {
        buildingSite.setCoordinates(10, 20);
        assertArrayEquals(new double[]{10, 20}, buildingSite.getCoordinates(), 0.001, "Coordinates should be set");
    }

    @Test
    void setCoordinatesDoesNotChangeValuesWhenCalledAgain() {
        buildingSite.setCoordinates(10, 20);
        buildingSite.setCoordinates(100, 200);
        assertArrayEquals(new double[]{10, 20}, buildingSite.getCoordinates(), 0.001, "Coordinates should remain the values from the first call\"");
    }


    @Test
    void getCoordinatesReturnsCloneNotSameArray() {
        buildingSite.setCoordinates(10, 20);
        double[] coords1 = buildingSite.getCoordinates();
        double[] coords2 = buildingSite.getCoordinates();

        // coords1 != coords2, cords1.equals(cords2)
        assertNotSame(coords1, coords2, "getCoordinates should return a clone");
        assertArrayEquals(coords1, coords2, 0.001, "Cloned arrays should have same content");

        // Modify the returned array should not change internal position
        coords1[0] = 999.9;
        coords2 = buildingSite.getCoordinates();

        assertNotEquals(coords1[0], coords2[0], "Modifying clone should not affect internal state");
    }


    @Test
    void getNeighboursReturnsCorrectNeighboursFromRoads() {
        when(mockRoad1.getNeighbour(buildingSite)).thenReturn(mockNeighbour1);
        when(mockRoad2.getNeighbour(buildingSite)).thenReturn(mockNeighbour2);
        when(mockRoad3.getNeighbour(buildingSite)).thenReturn(mockNeighbour3);

        buildingSite.addRoad(mockRoad1);
        buildingSite.addRoad(mockRoad2);
        buildingSite.addRoad(mockRoad3);


        List<BuildingSite> neighbours = buildingSite.getNeighbours();


        assertEquals(3, neighbours.size(), "Should return 3 neighbours");
        assertTrue(neighbours.containsAll(List.of(mockNeighbour1, mockNeighbour2, mockNeighbour3)), "List should contain all expected mock neighbours");

        // Verify that getNeighbour was called on each road exactly once with the correct argument
        verify(mockRoad1, times(1)).getNeighbour(buildingSite);
        verify(mockRoad2, times(1)).getNeighbour(buildingSite);
        verify(mockRoad3, times(1)).getNeighbour(buildingSite);
    }

    @Test
    void toStringOutputsCorrectString() {
        buildingSite.setCoordinates(10, 20);
        buildingSite.addTile(mockTile1);
        buildingSite.addTile(mockTile2);
        buildingSite.addTile(mockTile3);
        buildingSite.addRoad(mockRoad1);
        buildingSite.addRoad(mockRoad2);
        buildingSite.addRoad(mockRoad3);


        when(mockTile1.toString()).thenReturn("MockTile1");
        when(mockTile2.toString()).thenReturn("MockTile2");
        when(mockTile3.toString()).thenReturn("MockTile3");
        when(mockRoad1.toString()).thenReturn("MockRoad1");
        when(mockRoad2.toString()).thenReturn("MockRoad2");
        when(mockRoad3.toString()).thenReturn("MockRoad3");

        String toString = buildingSite.toString();
        String expectedString = "BuildingSite{ID='42', (10.0; 20.0), tiles=[MockTile1, MockTile2, MockTile3], roads=[MockRoad1, MockRoad2, MockRoad3]}";
        assertEquals(expectedString, toString, "String does not match");
    }

    @Test
    void testSetBuilding() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockRoad1.getOwner()).thenReturn(mockPlayer);
        when(mockRoad1.getNeighbour(any(BuildingSite.class))).thenReturn(mockNeighbour1);
        buildingSite.addRoad(mockRoad1);
        Settlement settlement = new Settlement(mockPlayer, PlayerColor.BLUE);
        buildingSite.setBuilding(settlement);
        assertEquals(settlement, buildingSite.building);
        assertEquals(playerId, buildingSite.getBuildingOwner().getUniqueId());
    }

    @Test
    void setBuildingShouldThrowErrorOnPlayerMismatch() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        Settlement settlement1 = new Settlement(mockPlayer, PlayerColor.BLUE);
        when(mockRoad1.getOwner()).thenReturn(mockPlayer);
        when(mockRoad1.getNeighbour(any(BuildingSite.class))).thenReturn(mockNeighbour1);
        buildingSite.addRoad(mockRoad1);
        buildingSite.setBuilding(settlement1);
        assertEquals(settlement1, buildingSite.building);

        String secondPlayerId = "Player2";
        Player mockPlayer2 = mock(Player.class);
        when(mockPlayer2.getUniqueId()).thenReturn(secondPlayerId);
        Settlement settlement2 = new Settlement(mockPlayer2, PlayerColor.BLUE);
        GameException ge = assertThrows(IntersectionOccupiedException.class, () -> buildingSite.setBuilding(settlement2));
        assertEquals("Intersection occupied!", ge.getMessage());
    }

    @Test
    void setBuildingShouldThrowErrorWhenSpacingRuleGetsViolated() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockRoad1.getOwner()).thenReturn(mockPlayer);
        when(mockRoad1.getNeighbour(any(BuildingSite.class))).thenReturn(mockNeighbour1);
        when(mockNeighbour1.getBuildingOwner()).thenReturn(mockPlayer);
        buildingSite.addRoad(mockRoad1);

        String secondPlayerId = "Player2";
        Player mockPlayer2 = mock(Player.class);
        when(mockPlayer2.getUniqueId()).thenReturn(secondPlayerId);
        Settlement settlement = new Settlement(mockPlayer2, PlayerColor.BLUE);
        GameException ge = assertThrows(SpacingRuleViolationException.class, () -> buildingSite.setBuilding(settlement));
        assertEquals("Too close to another settlement or city", ge.getMessage());
    }

    @Test
    void setBuildingShouldThrowErrorWhenPlayerHasNoAdjacentRoad() throws GameException {
        String playerId = "Player1";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        Settlement settlement = new Settlement(mockPlayer, PlayerColor.BLUE);
        GameException ge = assertThrows(NoAdjacentRoadException.class, () -> buildingSite.setBuilding(settlement));
        assertEquals("No adjacent roads found", ge.getMessage());
    }

    @Test
    void setBuildingShouldThrowErrorWhenOnlyAnotherPlayerHasAdjacentRoad() throws GameException {
        var player = new Player("Player1");
        when(mockRoad1.getOwner()).thenReturn(player);
        when(mockRoad1.getNeighbour(any(BuildingSite.class))).thenReturn(mockNeighbour1);
        buildingSite.addRoad(mockRoad1);

        String secondPlayerId = "Player2";
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(secondPlayerId);
        Settlement settlement = new Settlement(mockPlayer, PlayerColor.BLUE);
        GameException ge = assertThrows(NoAdjacentRoadException.class, () -> buildingSite.setBuilding(settlement));
        assertEquals("No adjacent roads found", ge.getMessage());
    }

    @Test
    void setBuildingShouldWorkIfTwoPlayersHaveAdjacentRoads() throws GameException {
        var player = new Player("Player1");
        var secondPlayer = new Player("Player2");
        when(mockRoad1.getOwner()).thenReturn(player);
        when(mockRoad1.getNeighbour(any(BuildingSite.class))).thenReturn(mockNeighbour1);
        when(mockRoad2.getOwner()).thenReturn(secondPlayer);
        when(mockRoad2.getNeighbour(any(BuildingSite.class))).thenReturn(mockNeighbour2);
        buildingSite.addRoad(mockRoad1);
        buildingSite.addRoad(mockRoad2);

        Settlement settlement = new Settlement(secondPlayer, PlayerColor.BLUE);
        buildingSite.setBuilding(settlement);
        assertEquals(settlement, buildingSite.building);
        assertEquals(secondPlayer, buildingSite.getBuildingOwner());
    }

    @Test
    void testToJsonInitialState() {

        ObjectNode jsonNode = buildingSite.toJson();

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
        when(mockRoad1.getNeighbour(any(BuildingSite.class))).thenReturn(mockNeighbour1);
        buildingSite.addRoad(mockRoad1);

        buildingSite.setCoordinates(expectedX, expectedY);
        buildingSite.setBuilding(mockBuilding);


        ObjectNode jsonNode = buildingSite.toJson();

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
        assertNull(buildingSite.getBuildingOwner(), "Building owner should be null when no building is present.");
    }

    @Test
    void getNeighboursNoRoads() {
        List<BuildingSite> neighbours = buildingSite.getNeighbours();
        assertTrue(neighbours.isEmpty(), "Neighbours list should be empty when there are no roads.");
    }

    @Test
    void getNeighboursWithOneNeighbour() {
        when(mockRoad1.getNeighbour(buildingSite)).thenReturn(mockNeighbour1);
        buildingSite.addRoad(mockRoad1);

        List<BuildingSite> neighbours = buildingSite.getNeighbours();

        assertEquals(1, neighbours.size(), "Should return 1 neighbour.");
        assertTrue(neighbours.contains(mockNeighbour1), "List should contain the expected mock neighbour.");
        verify(mockRoad1, times(1)).getNeighbour(buildingSite);
    }

    @Test
    void update_whenBuildingIsNull_doesNothing() {
        assertNull(buildingSite.building, "Building needs tio be null.");

        buildingSite.update(TileType.WHEAT);

        assertDoesNotThrow(() -> buildingSite.update(TileType.WHEAT), "Update should not throw an exception when building is null.");

        assertNull(buildingSite.building, "Building should still be null after update call.");
    }

    @Test
    void update_whenBuildingExists_distributesResources() throws GameException {
        String playerId = "test";
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        buildingSite.addRoad(mockRoad1);
        when(mockRoad1.getOwner()).thenReturn(player);

        when(mockRoad1.getNeighbour(buildingSite)).thenReturn(mockNeighbour1);
        when(mockNeighbour1.getBuildingOwner()).thenReturn(null);

        Building mockBuilding = mock(Building.class);
        when(mockBuilding.getPlayer()).thenReturn(player); // Building's owner

        buildingSite.setBuilding(mockBuilding);
        assertEquals(mockBuilding, buildingSite.building, "Building should be set correctly.");

        buildingSite.update(TileType.WHEAT);

        verify(mockBuilding, times(1)).distributeResourcesToPlayer(TileType.WHEAT);
    }

    @Test
    void getPortInitiallyShouldReturnNull() {
        assertNull(buildingSite.getPort(), "Initially, the port should be null.");
    }

    @Test
    void setPortWhenCurrentPortIsNullShouldSetThePort() {
        Port mockPort = mock(Port.class);

        assertNull(buildingSite.getPort(), "Port should be null before setting.");

        buildingSite.setPort(mockPort);

        assertNotNull(buildingSite.getPort(), "Port should not be null after setting.");
        assertEquals(mockPort, buildingSite.getPort(), "The retrieved port should be the one that was set.");
    }

    @Test
    void setPortWhenPortIsAlreadySetShouldNotChangeThePort() {
        Port initialMockPort = mock(Port.class);
        Port newMockPort = mock(Port.class);

        buildingSite.setPort(initialMockPort);
        assertEquals(initialMockPort, buildingSite.getPort(), "Initial port should be set correctly.");

        buildingSite.setPort(newMockPort);

        assertEquals(initialMockPort, buildingSite.getPort(), "Port should not have changed after attempting to set a new one when one was already present.");
        assertNotEquals(newMockPort, buildingSite.getPort(), "The port should not be the new port instance.");
    }

    @Test
    void setPortWithNullArgumentShouldNotThrowErrorAndPortRemainsUnchanged() {
        assertNull(buildingSite.getPort(), "Port is initially null.");
        buildingSite.setPort(null);
        assertNull(buildingSite.getPort(), "Port should remain null if set to null when already null.");

        Port mockPort = mock(Port.class);
        buildingSite.setPort(mockPort);
        assertNotNull(buildingSite.getPort());

        buildingSite.setPort(null);
        assertNotNull(buildingSite.getPort(), "Port should remain the originally set port, not become null.");
        assertEquals(mockPort, buildingSite.getPort(), "Port should be the original mockPort.");
    }
}
