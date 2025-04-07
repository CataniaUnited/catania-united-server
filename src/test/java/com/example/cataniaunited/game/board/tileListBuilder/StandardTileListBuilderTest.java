package com.example.cataniaunited.game.board.tileListBuilder;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class StandardTileListBuilderTest {

    // Standard test inputs
    private static final int TEST_BOARD_SIZE_1 = 1; // Center only -> 1
    private static final int TEST_BOARD_SIZE_2 = 2; // 1 + 6 tiles
    private static final int TEST_BOARD_SIZE_3 = 3; // 1 + 6 + 12 tiles
    private static final int TEST_HEX_SIZE = 10; // standard hex size

    StandardTileListBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new StandardTileListBuilder();
        builder.setConfiguration(TEST_BOARD_SIZE_3, TEST_HEX_SIZE, true); // standard config
    }
    @Test
    void resetClearsInternalStateAndResetsTileListToEmptyList(){
        builder.reset();
        assertEquals(0, builder.sizeOfBoard, "sizeOfBoard should be 0 after reset");
        assertEquals(0, builder.sizeOfHex, "sizeOfHex should be 0 after reset");
        assertTrue(builder.flipYAxis, "flipYAxis should be true after reset");
        assertNotNull(builder.tileList, "tileList should be non-null (empty list) after reset");
        assertTrue(builder.tileList.isEmpty(), "tileList should be empty after reset");
        assertEquals(0, builder.amountOfTilesOnBoard, "amountOfTilesOnBoard should be 0 after reset");
        assertEquals(0.0, builder.distanceBetweenTiles, 0.001, "distanceBetweenTiles should be 0 after reset");
        assertNull(builder.northWestAddition, "northWestAddition should be null after reset");
        assertNull(builder.southEastAddition, "southEastAddition should be null after reset");
    }


    @Test
    void SetConfigurationWithValidInputsInitializesState() {
        double expectedDistance = Math.sqrt(3.0) * TEST_HEX_SIZE;
        double[] expectedNW = new double[]{-8.660254, -15};
        double[] expectedSE = new double[]{8.660254, 15};

        // Assert: Directly check internal fields
        assertEquals(TEST_BOARD_SIZE_3, builder.sizeOfBoard);
        assertEquals(TEST_HEX_SIZE, builder.sizeOfHex);
        assertTrue(builder.flipYAxis);
        assertEquals(19, builder.amountOfTilesOnBoard);
        assertEquals(expectedDistance, builder.distanceBetweenTiles, 0.001);
        assertArrayEquals(expectedNW, builder.northWestAddition, 0.001);
        assertArrayEquals(expectedSE, builder.southEastAddition, 0.001);
        assertNotNull(builder.tileList);
        assertTrue(builder.tileList.isEmpty(), "tileList should be empty immediately after configuration");
    }

    @Test
    void setConfigurationThrowsExceptionForInvalidBoardSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setConfiguration(0, TEST_HEX_SIZE, true),
                "Board size and hex size must be positive."
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setConfiguration(-1, TEST_HEX_SIZE, true),
                "Board size and hex size must be positive."
        );
    }

    @Test
    void setConfigurationThrowsExceptionForInvalidHexSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setConfiguration(TEST_BOARD_SIZE_3, 0, true),
                "Board size and hex size must be positive."
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setConfiguration(TEST_BOARD_SIZE_3, -5, true),
                "Board size and hex size must be positive."
        );
    }

    @ParameterizedTest
    @MethodSource("boardSizeProvider")
    void buildTilesCreatesCorrectNumberOfTilesTest(int boardSize, int expectedCount) {
        builder.setConfiguration(boardSize, TEST_HEX_SIZE, true);
        int internalCount = builder.amountOfTilesOnBoard;
        assertEquals(expectedCount, internalCount, "Tile Calculation is wrong ");

        builder.buildTiles();

        assertEquals(expectedCount, builder.tileList.size(), "Internal list should have correct number of tiles");
    }

    static Stream<Arguments> boardSizeProvider() {
        return Stream.of(
                Arguments.of(TEST_BOARD_SIZE_1, 1),
                Arguments.of(TEST_BOARD_SIZE_2, 7),
                Arguments.of(TEST_BOARD_SIZE_3, 19)
        );
    }

    @Test
    void buildTilesCreatesCorrectTileTypeDistribution() {
        builder.buildTiles();

        // Assert: Analyze internal list directly
        Map<TileType, Long> counts = builder.tileList.stream()
                .collect(Collectors.groupingBy(Tile::getType, Collectors.counting()));

        // Same assertions as before, but acting on builder.tileList
        assertEquals(1L, counts.getOrDefault(TileType.WASTE, 0L));
        assertEquals(4L, counts.getOrDefault(TileType.WHEAT, 0L));
        assertEquals(4L, counts.getOrDefault(TileType.SHEEP, 0L));
        assertEquals(4L, counts.getOrDefault(TileType.WOOD, 0L));
        assertEquals(3L, counts.getOrDefault(TileType.CLAY, 0L));
        assertEquals(3L, counts.getOrDefault(TileType.ORE, 0L));
        assertEquals(19, builder.tileList.size());
    }

    @Test
    void buildTilesThrowsExceptionIfNotConfigured() {
        builder.reset();
        assertThrows(IllegalStateException.class, () -> builder.buildTiles(), "buildTiles should throw Exception if not configured");
    }

    @Test
    void shuffleTilesDoesNotChangeListContentOrSize() {
        builder.buildTiles();
        // Create copy *directly* from internal list
        List<Tile> tilesBeforeShuffle = new ArrayList<>(builder.tileList);

        builder.shuffleTiles();

        // Assert against internal list directly
        assertEquals(tilesBeforeShuffle.size(), builder.tileList.size(), "Shuffle should not change list size");
        assertTrue(builder.tileList.containsAll(tilesBeforeShuffle), "Shuffle should contain all original tiles");
        assertTrue(tilesBeforeShuffle.containsAll(builder.tileList), "Shuffle should not introduce new tiles");
    }

    @Test
    void shuffleTilesThrowsExceptionIfTilesNotBuilt() {
        assertThrows(IllegalStateException.class, () -> builder.shuffleTiles(), "shuffle should throw exception if tiles are not build");
    }

    @Test
    void shuffleTilesThrowsExceptionIfNotConfigured() {
        builder.reset();
        assertThrows(IllegalStateException.class, () -> builder.shuffleTiles(), "shuffle should throw exception if the builder is not configured");
    }

    @Test
    void assignTileIdsAssignsSequentialIds() {
        builder.buildTiles();
        builder.shuffleTiles();
        builder.assignTileIds();

        for (int i = 0; i < builder.tileList.size(); i++) {
            assertEquals(i + 1, builder.tileList.get(i).getId(), "Id's don't match");
        }
    }

    @Test
    void assignTileIdsThrowsExceptionIfTilesNotBuilt() {
        assertThrows(IllegalStateException.class, () -> builder.assignTileIds(), "should throw exception if tiles are not build");
    }

    @Test
    void assignTileIdsThrowsExceptionIfNotConfigured() {
        builder.reset();
        assertThrows(IllegalStateException.class, () -> builder.assignTileIds(), "should throw exception if the builder is not configured");
    }

    @Test
    void verifyCalculateTilePositionCoordinatesForStandardBoard() {
        // Arrange
        builder.buildTiles();
        assertEquals(19, builder.tileList.size());


        builder.calculateTilePositions();

        assertArrayEquals(new double[]{0.0, 0.0}, builder.tileList.get(0).getCoordinates(), 0.001, "coordinates for index 0 are incorrect");
        assertArrayEquals(new double[]{8.660254, 15.0}, builder.tileList.get(1).getCoordinates(), 0.001, "coordinates for index 1 are incorrect");
        assertArrayEquals(new double[]{17.320508, 0.0}, builder.tileList.get(2).getCoordinates(), 0.001, "coordinates for index 2 are incorrect");
        assertArrayEquals(new double[]{8.660254, -15.0}, builder.tileList.get(3).getCoordinates(), 0.001, "coordinates for index 3 are incorrect");
        assertArrayEquals(new double[]{-8.660254, -15.0}, builder.tileList.get(4).getCoordinates(), 0.001, "coordinates for index 4 are incorrect");
        assertArrayEquals(new double[]{-17.320508, 0.0}, builder.tileList.get(5).getCoordinates(), 0.001, "coordinates for index 5 are incorrect");
        assertArrayEquals(new double[]{-8.660254, 15.0}, builder.tileList.get(6).getCoordinates(), 0.001, "coordinates for index 6 are incorrect");
        assertArrayEquals(new double[]{17.320508, 30.0}, builder.tileList.get(7).getCoordinates(), 0.001, "coordinates for index 7 are incorrect");
        assertArrayEquals(new double[]{25.980762, 15.0}, builder.tileList.get(8).getCoordinates(), 0.001, "coordinates for index 8 are incorrect");
        assertArrayEquals(new double[]{34.641016, 0.0}, builder.tileList.get(9).getCoordinates(), 0.001, "coordinates for index 9 are incorrect");
        assertArrayEquals(new double[]{25.980762, -15.0}, builder.tileList.get(10).getCoordinates(), 0.001, "coordinates for index 10 are incorrect");
        assertArrayEquals(new double[]{17.320508, -30.0}, builder.tileList.get(11).getCoordinates(), 0.001, "coordinates for index 11 are incorrect");
        assertArrayEquals(new double[]{0.0, -30.0}, builder.tileList.get(12).getCoordinates(), 0.001, "coordinates for index 12 are incorrect");
        assertArrayEquals(new double[]{-17.320508, -30.0}, builder.tileList.get(13).getCoordinates(), 0.001, "coordinates for index 13 are incorrect");
        assertArrayEquals(new double[]{-25.980762, -15.0}, builder.tileList.get(14).getCoordinates(), 0.001, "coordinates for index 14 are incorrect");
        assertArrayEquals(new double[]{-34.641016, 0.0}, builder.tileList.get(15).getCoordinates(), 0.001, "coordinates for index 15 are incorrect");
        assertArrayEquals(new double[]{-25.980762, 15.0}, builder.tileList.get(16).getCoordinates(), 0.001, "coordinates for index 16 are incorrect");
        assertArrayEquals(new double[]{-17.320508, 30.0}, builder.tileList.get(17).getCoordinates(), 0.001, "coordinates for index 17 are incorrect");
        assertArrayEquals(new double[]{0.0, 30.0}, builder.tileList.get(18).getCoordinates(), 0.001, "coordinates for index 18 are incorrect");
    }


    @Test
    void calculateTilePositionsThrowsExceptionIfTilesNotBuilt() {
        assertThrows(IllegalStateException.class, () -> builder.calculateTilePositions(), "should throw exception if tiles are not build");
    }

    @Test
    void calculateTilePositionsThrowsExceptionIfNotConfigured() {
        builder.reset();
        assertThrows(IllegalStateException.class, () -> builder.calculateTilePositions(), "should throw exception if the builder is not configured");
    }

    @Test
    void getTileListReturnsInternalListInstance() {
        builder.buildTiles();
        builder.shuffleTiles();
        builder.assignTileIds();
        builder.calculateTilePositions();

        List<Tile> listFromGetter = builder.getTileList();

        assertNotNull(listFromGetter);
        assertEquals(builder.amountOfTilesOnBoard, listFromGetter.size());
        assertSame(builder.tileList, listFromGetter, "getTileList should return the same internal list instance");
    }

    @Test
    void getTileListThrowsExceptionIfNotProperlyBuilt() {
        builder.reset();
        assertThrows(IllegalStateException.class, () -> builder.getTileList(), "Should fail if not configured");
        builder.setConfiguration(TEST_BOARD_SIZE_2, TEST_HEX_SIZE, true);
        assertThrows(IllegalStateException.class, () -> builder.getTileList(), "Should fail if tiles not built");
        builder.buildTiles();
        assertDoesNotThrow(() -> builder.getTileList(), "Should succeed after buildTiles");
    }


    @Test
    void calculateAmountOfTilesForLayerKReturnsCorrectValues() {
        assertEquals(0, StandardTileListBuilder.calculateAmountOfTilesForLayerK(0));
        assertEquals(1, StandardTileListBuilder.calculateAmountOfTilesForLayerK(1));
        assertEquals(7, StandardTileListBuilder.calculateAmountOfTilesForLayerK(2));
        assertEquals(19, StandardTileListBuilder.calculateAmountOfTilesForLayerK(3));
        assertEquals(37, StandardTileListBuilder.calculateAmountOfTilesForLayerK(4));
    }


    @Test
    void polarToCartesianConvertsCorrectly() {
        double r = 10.0;
        assertArrayEquals(new double[]{10.0, 0.0}, StandardTileListBuilder.polarToCartesian(r, 0, false), 0.001);
        assertArrayEquals(new double[]{10.0, -0.0}, StandardTileListBuilder.polarToCartesian(r, 0, true), 0.001);
        assertArrayEquals(new double[]{0.0, 10.0}, StandardTileListBuilder.polarToCartesian(r, Math.PI/2, false), 0.001);
        assertArrayEquals(new double[]{0.0, -10.0}, StandardTileListBuilder.polarToCartesian(r, Math.PI/2, true), 0.001);
        assertArrayEquals(new double[]{-10.0, 0.0}, StandardTileListBuilder.polarToCartesian(r, Math.PI, false), 0.001);
        assertArrayEquals(new double[]{-10.0, -0.0}, StandardTileListBuilder.polarToCartesian(r, Math.PI, true), 0.001);
        assertArrayEquals(new double[]{0.0, -10.0}, StandardTileListBuilder.polarToCartesian(r, 3*Math.PI/2, false), 0.001);
        assertArrayEquals(new double[]{0.0, 10.0}, StandardTileListBuilder.polarToCartesian(r, 3*Math.PI/2, true), 0.001);
    }



}
