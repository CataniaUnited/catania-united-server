package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.buildings.Settlement;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            settlementPosition.addTile(mockTile4);
        }, "Should throw IllegalStateException when adding 4th tile");

        assertTrue(exception.getMessage().contains("Cannot assign more than 3 Tiles"), "Exception message should be correct");
        assertEquals(3, settlementPosition.getTiles().size(), "Tile count should remain 3 after exception");
    }

    @Test
    void cantChangeTileListWhenRecivedFromGetterBecauseImmutable() {
        settlementPosition.addTile(mockTile1);
        List<Tile> tiles = settlementPosition.getTiles();

        assertThrows(UnsupportedOperationException.class, () -> {
            tiles.add(mockTile2);
        }, "Returned tile list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> {
            tiles.remove(mockTile1);
        }, "Returned tile list should be immutable");
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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            settlementPosition.addRoad(mockRoad4);
        }, "Should throw IllegalStateException when adding 4th road");

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

        assertThrows(UnsupportedOperationException.class, () -> {
            roads.add(mockRoad2);
        }, "Returned road list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> {
            roads.remove(mockRoad1);
        }, "Returned road list should be immutable");
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
        Settlement settlement = new Settlement(playerId);
        settlementPosition.setBuilding(settlement);
        assertEquals(settlement, settlementPosition.building);
    }

    @Test
    void setBuildingShouldThrowErrorOnPlayerMismatch() throws GameException {
        String playerId = "Player1";
        Settlement settlement1 = new Settlement(playerId);
        settlementPosition.setBuilding(settlement1);
        assertEquals(settlement1, settlementPosition.building);

        String secondPlayerId = "Player2";
        Settlement settlement2 = new Settlement(secondPlayerId);
        GameException ge = assertThrows(GameException.class, () -> settlementPosition.setBuilding(settlement2));
        assertEquals("Player mismatch when placing building: positionId = %s, playerId = %s".formatted(settlementPosition.id, secondPlayerId), ge.getMessage());
    }
}
