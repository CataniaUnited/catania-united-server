package com.example.cataniaunited.game.board;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class RoadTest {
    SettlementPosition mockPositionA;
    SettlementPosition mockPositionB;
    Road road;

    @BeforeEach
    void setUp() {
        //Ensure that "." is used instead of "," to separate decimals
        Locale.setDefault(Locale.ENGLISH);

        mockPositionA = mock(SettlementPosition.class);
        mockPositionB = mock(SettlementPosition.class);


        road = new Road(mockPositionA, mockPositionB);
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
        assertEquals(0.0, road.getRationAngle(), 0.001, "Initial angle should be 0.0");
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

        assertArrayEquals(expectedCoords, road.getCoordinates(), 0.001, "Calculated coordinates are incorrect");
        assertEquals(expectedAngle, road.getRationAngle(), 0.001, "Calculated angle is incorrect");
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
        double angleAfterFirstCall = road.getRationAngle();

        // Setup done, test
        // Change given Coords
        when(mockPositionA.getCoordinates()).thenReturn(new double[]{99.0, 100.0});
        when(mockPositionB.getCoordinates()).thenReturn(new double[]{101.0, 100.0});

        // Act: Call the method again
        road.setCoordinatesAndRotationAngle();

        // Assert: State should be unchanged from after the first call
        assertArrayEquals(coordsAfterFirstCall, road.getCoordinates(), 0.001, "Coordinates should not change on second call");
        assertEquals(angleAfterFirstCall, road.getRationAngle(), 0.001, "Angle should not change on second call");

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
        assertEquals(0.0, road.getRationAngle(), 0.001, "Angle should remain zero");

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
        assertEquals(0.0, road.getRationAngle(), 0.001, "Angle should remain zero");

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
}
