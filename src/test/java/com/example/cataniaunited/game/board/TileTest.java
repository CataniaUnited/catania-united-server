package com.example.cataniaunited.game.board;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TileTest {
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

}
