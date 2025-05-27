package com.example.cataniaunited.game.board;

import com.example.cataniaunited.game.board.ports.GeneralPort;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.ports.SpecificResourcePort;
import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.TileListDirector;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.CatanBoardUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@QuarkusTest
class GraphBuilderTest {

    private List<Tile> validTileList;
    private int validSizeOfBoard;

    @BeforeEach
    void setUp() {
        validSizeOfBoard = 3;
        int expectedTileCount = CatanBoardUtils.calculateAmountOfTilesForLayerK(validSizeOfBoard);
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


    @Test
    void getPortListBeforeGraphGenerationShouldReturnNull() {
        GraphBuilder graphBuilder = new GraphBuilder(validTileList, validSizeOfBoard);
        assertNull(graphBuilder.getPortList(), "Port list should be null before generateGraph is called.");
    }

    @Test
    void generateGraphShouldPopulatePortList() {
        GraphBuilder graphBuilder = new GraphBuilder(validTileList, validSizeOfBoard);
        graphBuilder.generateGraph();
        List<Port> ports = graphBuilder.getPortList();

        assertNotNull(ports, "Port list should not be null after graph generation.");
        assertFalse(ports.isEmpty(), "Port list should not be empty for a standard board size.");
        assertEquals(9, ports.size(), "For board size 3, expected 9 ports.");
    }

    @Test
    void portTypesAndCountsShouldBeCorrectForStandardBoardSize3() {
        GraphBuilder graphBuilder = new GraphBuilder(validTileList, validSizeOfBoard);
        graphBuilder.generateGraph();
        List<Port> ports = graphBuilder.getPortList();

        assertEquals(9, ports.size(), "Total ports for size 3 board should be 9.");

        long generalPortCount = ports.stream().filter(GeneralPort.class::isInstance).count();
        long specificPortCount = ports.stream().filter(SpecificResourcePort.class::isInstance).count();

        assertEquals(4, generalPortCount, "Should have 4 GeneralPorts for size 3 board.");
        assertEquals(5, specificPortCount, "Should have 5 SpecificResourcePorts for size 3 board.");
    }

    @Test
    void portTypesAndCountsShouldBeCorrectForBoardSize2() {
        int boardSize = 2;
        int expectedTileCount = CatanBoardUtils.calculateAmountOfTilesForLayerK(boardSize);
        List<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < expectedTileCount; i++) {
            tiles.add(new Tile(TileType.WOOD));
        }
        GraphBuilder graphBuilder = new GraphBuilder(tiles, boardSize);
        graphBuilder.generateGraph();
        List<Port> ports = graphBuilder.getPortList();

        assertEquals(5, ports.size(), "Total ports for size 2 board should be 5.");
        assertEquals(0, ports.stream().filter(GeneralPort.class::isInstance).count(), "Should have 0 GeneralPorts for size 2 board.");
        assertEquals(5, ports.stream().filter(SpecificResourcePort.class::isInstance).count(), "Should have 5 SpecificResourcePorts for size 2 board.");
    }

    @Test
    void portTypesAndCountsShouldBeCorrectForBoardSize4() {
        int boardSize = 4;
        int expectedTileCount = CatanBoardUtils.calculateAmountOfTilesForLayerK(boardSize);
        List<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < expectedTileCount; i++) {
            tiles.add(new Tile(TileType.ORE));
        }
        GraphBuilder graphBuilder = new GraphBuilder(tiles, boardSize);
        graphBuilder.generateGraph();
        List<Port> ports = graphBuilder.getPortList();

        assertEquals(11, ports.size(), "Total ports for size 4 board should be 11.");
        assertEquals(6, ports.stream().filter(GeneralPort.class::isInstance).count(), "Should have 6 GeneralPorts for size 4 board.");
        assertEquals(5, ports.stream().filter(SpecificResourcePort.class::isInstance).count(), "Should have 5 SpecificResourcePorts for size 4 board.");
    }
    @Test
    void specificResourcePortsShouldHaveVarietyForStandardBoard() {
        GraphBuilder graphBuilder = new GraphBuilder(validTileList, validSizeOfBoard);
        graphBuilder.generateGraph();
        List<Port> ports = graphBuilder.getPortList();

        List<SpecificResourcePort> specificPorts = ports.stream()
                .filter(SpecificResourcePort.class::isInstance)
                .map(p -> (SpecificResourcePort) p)
                .toList();

        assertEquals(5, specificPorts.size());

        Map<TileType, Long> resourceCounts = specificPorts.stream()
                .collect(Collectors.groupingBy(SpecificResourcePort::getTradeAbleResource, Collectors.counting()));

        for (TileType type : TileType.values()) {
            if (type != TileType.WASTE) {
                assertTrue(resourceCounts.containsKey(type), "Specific port for " + type + " should be present.");
                assertEquals(1L, resourceCounts.get(type), "Should be exactly one specific port for " + type);
            }
        }
    }

    @Test
    void checkPortPlacementRhythmDoesNotGoOutOfBounds() {
        int boardSize = 2;
        int expectedTileCount = CatanBoardUtils.calculateAmountOfTilesForLayerK(boardSize);
        List<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < expectedTileCount; i++) {
            tiles.add(new Tile(TileType.CLAY));
        }
        GraphBuilder graphBuilder = new GraphBuilder(tiles, boardSize);

        assertDoesNotThrow(graphBuilder::generateGraph,
                "Port placement should not cause index out of bounds with calculated rhythm.");

        List<Port> ports = graphBuilder.getPortList();
        assertEquals(5, ports.size());
        for(Port p : ports) {
            assertNotNull(p.getSettlementPositions().get(0));
            assertNotNull(p.getSettlementPositions().get(1));
        }
    }

    @Test
    void portDistributionForVeryLargeBoardSize() {
        int boardSize = 7;

        int expectedTileCount = CatanBoardUtils.calculateAmountOfTilesForLayerK(boardSize);
        List<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < expectedTileCount; i++) {
            tiles.add(new Tile(TileType.SHEEP));
        }
        GraphBuilder graphBuilder = new GraphBuilder(tiles, boardSize);
        graphBuilder.generateGraph();
        List<Port> ports = graphBuilder.getPortList();

        assertEquals(15, ports.size(), "Total ports for size " + boardSize);

        long generalPortCount = ports.stream().filter(GeneralPort.class::isInstance).count();
        long specificPortCount = ports.stream().filter(SpecificResourcePort.class::isInstance).count();

        assertEquals(7, generalPortCount, "GeneralPorts for size " + boardSize);
        assertEquals(8, specificPortCount, "SpecificResourcePorts for size " + boardSize);

        Map<TileType, Long> resourceDistribution = ports.stream()
                .filter(SpecificResourcePort.class::isInstance)
                .map(p -> (SpecificResourcePort) p)
                .collect(Collectors.groupingBy(SpecificResourcePort::getTradeAbleResource, Collectors.counting()));

        assertTrue(resourceDistribution.size() <= 5 && !resourceDistribution.isEmpty(), "Should have between 1 and 5 unique specific resource port types.");
        resourceDistribution.values().forEach(count -> assertTrue(count >= 1 && count <= 2, "Each specific resource type should appear 1 or 2 times for 8 specific ports."));
    }

}



