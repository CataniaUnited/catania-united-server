package com.example.cataniaunited.game.board;

import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.TileListDirector;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@QuarkusTest
class GraphBuilderTest {

    private List<Tile> validTileList;
    private int validSizeOfBoard;

    @BeforeEach
    void setUp() {
        validSizeOfBoard = 3;
        int expectedTileCount = StandardTileListBuilder.calculateAmountOfTilesForLayerK(validSizeOfBoard);
        validTileList = new ArrayList<>();
        for (int i = 0; i < expectedTileCount; i++) {
            validTileList.add(new Tile(TileType.WASTE));
        }
    }

    @Test
    void constructorWithNullAsListShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new GraphBuilder(null, validSizeOfBoard),
                "Tile list cannot be null or empty.");
    }

    @Test
    void constructorWithEmptyTileListShouldThrowException() {
        List<Tile> emptyList = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> new GraphBuilder(emptyList, validSizeOfBoard),
                "Tile list cannot be null or empty.");
    }

    @Test
    void ConstructorWithNonPositiveBoardSizeShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new GraphBuilder(validTileList, 0),
                "Board size must be positive.");
    }

    @Test
    void ConstructorWithBoardSize1ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new GraphBuilder(validTileList, 0),
                "Board size cant be 1.");
    }

    @Test
    void constructorWithIncorrectTileListSizeShouldThrowException() {
        List<Tile> incorrectTileList = new ArrayList<>();
        for (int i = 0; i < validTileList.size() - 1; i++) {
            incorrectTileList.add(new Tile(TileType.WASTE));
        }

        assertThrows(IllegalArgumentException.class, () -> new GraphBuilder(incorrectTileList, validSizeOfBoard),
                "Tile list size mismatch.");
    }

    @Test
    void constructorWithValidParametersShouldNotThrowException() {
        assertDoesNotThrow(() -> new GraphBuilder(validTileList, validSizeOfBoard));
    }

    @ParameterizedTest
    @MethodSource("duplicateListProvider")
    void findDuplicateTileListTest(List<Tile> tileList, Tile duplicateTile) {
        Assertions.assertEquals(GraphBuilder.findDuplicateTile(tileList), duplicateTile);
    }

    static Stream<Arguments> duplicateListProvider() {
        Tile duplicateTile = new Tile(TileType.WHEAT);
        Tile otherTile1 = new Tile(TileType.WHEAT);
        Tile otherTile2 = new Tile(TileType.WHEAT);
        Tile otherTile3 = new Tile(TileType.WHEAT);

        return Stream.of(
                // first element is duplicate
                Arguments.of(List.of(duplicateTile, duplicateTile, otherTile1, otherTile2, otherTile3), duplicateTile),
                Arguments.of(List.of(duplicateTile, otherTile1, duplicateTile, otherTile2, otherTile3), duplicateTile),
                Arguments.of(List.of(duplicateTile, otherTile1, otherTile2, duplicateTile, otherTile3), duplicateTile),
                Arguments.of(List.of(duplicateTile, otherTile1, otherTile3, otherTile2, duplicateTile), duplicateTile),

                // second element is duplicate
                Arguments.of(List.of(otherTile1, duplicateTile, duplicateTile, otherTile2, otherTile3), duplicateTile),
                Arguments.of(List.of(otherTile1, duplicateTile, otherTile2, duplicateTile, otherTile3), duplicateTile),
                Arguments.of(List.of(otherTile1, duplicateTile, otherTile3, otherTile2, duplicateTile), duplicateTile),

                // third element is duplicate
                Arguments.of(List.of(otherTile1, otherTile2, duplicateTile, duplicateTile, otherTile3), duplicateTile),
                Arguments.of(List.of(otherTile1, otherTile2, duplicateTile, otherTile3, duplicateTile), duplicateTile),

                // fourth element is duplicate
                Arguments.of(List.of(otherTile1, otherTile2, otherTile3, duplicateTile, duplicateTile), duplicateTile)
        );
    }

    @Test
    void integrationTestGraphConstructionForLargeSize() {
        int sizeOfHex = 10;
        int size = 10; // normal board has 3
        TileListBuilder tileBuilder = new StandardTileListBuilder();
        TileListDirector director = new TileListDirector(tileBuilder);
        director.constructStandardTileList(size, sizeOfHex, true);
        List<Tile> tileList = tileBuilder.getTileList();


        GraphBuilder graphBuilder = new GraphBuilder(tileList, size);
        List<SettlementPosition> graph = graphBuilder.generateGraph();

        assertEquals(GraphBuilder.calculateTotalSettlementPositions(size), graph.size(), "size doesn't match");

        for (SettlementPosition node : graph) {
            assertNotEquals(new double[]{0.0}, node.getCoordinates(), " coordinates have not been set");
            assertTrue(node.getNeighbours().size() >= 2, "Each node has at least 2 neighbours");
            assertFalse(node.getTiles().isEmpty(), "Each node is at least connected to one tile");

            double[] coordinates = node.getCoordinates();
            for (Tile tile : node.getTiles()) {
                double[] tileCoordinates = tile.getCoordinates();
                assertEquals(sizeOfHex, calculateDistance(coordinates, tileCoordinates), 0.001, "position is wrong");
            }

            for (SettlementPosition neighbour : node.getNeighbours()) {
                double[] neighbourCoordinates = neighbour.getCoordinates();
                assertEquals(sizeOfHex, calculateDistance(coordinates, neighbourCoordinates), 0.001, "position is wrong");
            }
        }

        for (int i = 0; i < GraphBuilder.calculateTotalSettlementPositions(size - 1); i++) {
            SettlementPosition node = graph.get(i);
            assertEquals(3, node.getTiles().size(), "Inner nodes should be connected to three tiles");
            assertEquals(3, node.getNeighbours().size(), "Inner nodes should be connected to three neighbours");
        }
    }

    static double calculateDistance(double[] posA, double[] posB) {
        double sum = 0;
        for (int i = 0; i < 2; i++) {
            sum += StrictMath.pow(posA[i] - posB[i], 2);
        }
        return StrictMath.sqrt(sum);
    }

    @Test
    void checkAndThrowAssertionErrorShouldNotThrowWhenSuccessIsTrue() {
        assertDoesNotThrow(() -> GraphBuilder.checkAndThrowAssertionError(true, "Shouldn't Throw"), "Shouldn't throw");
    }

    @Test
    void checkAndThrowAssertionErrorShouldThrowAssertionErrorWhenSuccessIsFalse() {
        String message = "Test failed";
        assertThrows(AssertionError.class,
                () -> GraphBuilder.checkAndThrowAssertionError(false, message)
                , "should throw exception"
        );
    }

    @Test
    void getRoadListShouldThrowExceptionWhenRoadListIsNull() {
        GraphBuilder graphBuilder = mock(GraphBuilder.class);
        when(graphBuilder.getRoadList()).thenCallRealMethod();
        IllegalStateException exception = assertThrows(IllegalStateException.class, graphBuilder::getRoadList);
        assertEquals("Build graph before accessing road list.", exception.getMessage());
    }

    @Test
    void getRoadListShouldThrowExceptionWhenRoadListIsEmpty() {
        GraphBuilder graphBuilder = mock(GraphBuilder.class);
        when(graphBuilder.getRoadList()).thenCallRealMethod();
        graphBuilder.roadList = List.of();
        IllegalStateException exception = assertThrows(IllegalStateException.class, graphBuilder::getRoadList);
        assertEquals("Build graph before accessing road list.", exception.getMessage());
    }

}



