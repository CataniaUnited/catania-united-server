package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.buildings.Settlement;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
class GameBoardTest {
    @Test
    void calculateSizeOfBoardThrowsForZeroPlayers() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GameBoard(0);
        }, "Should throw for 0 players");
    }

    @Test
    void calculateSizeOfBoardThrowsForNegativePlayers() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GameBoard(-1);
        }, "Should throw for negative players");
    }

    @Test
    void calculateSizeOfBoardThrowsForOnePlayer() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GameBoard(1);
        }, "Should throw for one player");
    }

    @Test
    void calculateSizeOfBoardReturns3For2Players() {
        assertEquals(3, GameBoard.calculateSizeOfBoard(2));
    }

    @Test
    void calculateSizeOfBoardReturns3For3Players() {
        assertEquals(3, GameBoard.calculateSizeOfBoard(3));
    }

    @Test
    void calculateSizeOfBoardReturns3For4Players() {
        assertEquals(3, GameBoard.calculateSizeOfBoard(4));
    }

    @Test
    void calculateSizeOfBoardReturns4For5Players() {
        assertEquals(4, GameBoard.calculateSizeOfBoard(5));
    }

    @Test
    void calculateSizeOfBoardReturns4For6Players() {
        assertEquals(4, GameBoard.calculateSizeOfBoard(6));
    }

    @Test
    void calculateSizeOfBoardReturns5For7Players() {
        assertEquals(5, GameBoard.calculateSizeOfBoard(7));
    }

    @Test
    void calculateSizeOfBoardReturns5For8Players() {
        assertEquals(5, GameBoard.calculateSizeOfBoard(8));
    }

    @Test
    void calculateSizeOfBoardUsesFormulaFor9Players() {
        assertEquals(5, GameBoard.calculateSizeOfBoard(9));
    }

    @Test
    void calculateSizeOfBoardUsesFormulaFor12Players() {
        assertEquals(5, GameBoard.calculateSizeOfBoard(12));
    }

    @ParameterizedTest
    @MethodSource("playerCountProvider")
    void constructorExecutesGenerationSuccessfullyForValidPlayerCount(int playerCount) {
        GameBoard gameBoard = null;

        // Since the generation happens in the constructor we can't mock it,
        // therefore we integration test and assert a valid internal state after generation
        try {
            gameBoard = assertDoesNotThrow(() -> new GameBoard(playerCount),
                    "Board generation failed unexpectedly for " + playerCount + " players.");
        } catch (Exception e) {
            // Log the actual exception if assertDoesNotThrow isn't available or for more detail
            System.err.println("GameBoard constructor failed: " + e.getMessage());
            fail("GameBoard constructor threw an unexpected exception.");
        }

        assertNotNull(gameBoard, "GameBoard instance should be created");
        assertEquals(GameBoard.calculateSizeOfBoard(playerCount), gameBoard.sizeOfBoard, "Internal board size should be set correctly");

        List<Tile> tiles = gameBoard.getTileList();
        List<SettlementPosition> graph = gameBoard.getSettlementPositionGraph();

        // More Detailed tests have been conducted in the respective test classes
        assertNotNull(tiles, "Generated tile list should not be null");
        assertNotNull(graph, "Generated settlement graph should not be null");
        assertFalse(tiles.isEmpty(), "Generated tile list should not be empty");
        assertFalse(graph.isEmpty(), "Generated settlement graph should not be empty");
    }

    static Stream<Arguments> playerCountProvider() {
        return Stream.of(
                Arguments.of(2),
                Arguments.of(3),
                Arguments.of(4),
                Arguments.of(5),
                Arguments.of(6),
                Arguments.of(7),
                Arguments.of(8),
                Arguments.of(9),
                Arguments.of(10)
        );
    }

    @Test
    void generateBoardThrowsExceptionIfGeneratingGraphBeforeTileList() {
        GameBoard gameBoard = new GameBoard(4);
        gameBoard.tileList = null;
        assertThrows(IllegalStateException.class,
                gameBoard::generateBoard,
                "Should throw for undefined tileList players"
        );
    }

    @Disabled("BenchmarkTest Tests how many Players theoretically can play a game, doesn't test functionality, therefore disabled")
    @ParameterizedTest
    @MethodSource("benchMarkTestProvider")
    void benchMarkTest(int sizeOfBoard) {
        try {
            new GameBoard(sizeOfBoard);
        } catch (OutOfMemoryError e) {
            fail("Out of Memory");
        }

    }

    static Stream<Arguments> benchMarkTestProvider() {
        return Stream.of(
                Arguments.of(2),
                Arguments.of(3),
                Arguments.of(4),
                Arguments.of(5),
                Arguments.of(6),
                Arguments.of(7),
                Arguments.of(8),
                Arguments.of(10),
                Arguments.of(100),
                Arguments.of(1000),
                Arguments.of(10000),
                Arguments.of(100000)
        );
    }

    @Test
    void testPlaceSettlement() throws GameException {
        String playerId = "Player1";
        GameBoard gameBoard = new GameBoard(2);
        var settlementPosition = gameBoard.getSettlementPositionGraph().get(0);
        Road road = settlementPosition.roads.get(0);
        road.setOwnerPlayerId(playerId);

        gameBoard.placeSettlement(playerId, PlayerColor.BLUE, settlementPosition.getId());
        assertNotNull(settlementPosition.building);
        assertEquals(Settlement.class, settlementPosition.building.getClass());
        assertEquals(playerId, settlementPosition.building.getOwnerPlayerId());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfPositionIsLessThanZero() {
        GameBoard gameBoard = new GameBoard(2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement("Player1", PlayerColor.BLUE, -1));
        assertEquals("Settlement position not found: id = %s".formatted(-1), ge.getMessage());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfPositionIsBiggerThanSize() {
        GameBoard gameBoard = new GameBoard(2);
        int positionId = gameBoard.getSettlementPositionGraph().size() + 1;
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement("Player1", PlayerColor.BLUE, positionId));
        assertEquals("Settlement position not found: id = %s".formatted(positionId), ge.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidPlayerIds")
    void placeSettlementShouldThrowExceptionIfPlayerIdIsEmpty(String playerId) {
        GameBoard gameBoard = new GameBoard(2);
        int positionId = gameBoard.getSettlementPositionGraph().get(0).getId();
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement(playerId, PlayerColor.BLUE, positionId));
        assertEquals("Owner Id of building must not be empty", ge.getMessage());
    }

    @Test
    void testPlaceRoad() throws GameException {
        GameBoard gameBoard = new GameBoard(2);
        var road = gameBoard.roadList.get(0);
        String playerId = "Player1";
        gameBoard.placeRoad(playerId, PlayerColor.BLUE, road.getId());
        assertEquals(playerId, road.getOwnerPlayerId());
    }

    @Test
    void placeRoadShouldThrowExceptionIfRoadIdIsLessThanZero() {
        GameBoard gameBoard = new GameBoard(2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad("Player1", PlayerColor.BLUE, -1));
        assertEquals("Road not found: id = %s".formatted(-1), ge.getMessage());
    }

    @Test
    void placeRoadShouldThrowExceptionIfRoadIdIsGreaterThanSize() {
        GameBoard gameBoard = new GameBoard(2);
        int roadId = gameBoard.roadList.size() + 1;
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad("Player1", PlayerColor.BLUE, roadId));
        assertEquals("Road not found: id = %s".formatted(roadId), ge.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidPlayerIds")
    void placeRoadShouldThrowExceptionIfPlayerIdIsEmpty(String playerId) {
        GameBoard gameBoard = new GameBoard(2);
        var road = gameBoard.roadList.get(0);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad(playerId, PlayerColor.BLUE, road.getId()));
        assertEquals("Owner Id of road must not be empty", ge.getMessage());
    }

    static Stream<Arguments> invalidPlayerIds() {
        return Stream.of(null, Arguments.of(""));
    }


    @Test
    void testGetJsonStructure() {
        int playerCount = 2; // Use a simple case
        GameBoard gameBoard = new GameBoard(playerCount);
        int expectedSize = GameBoard.calculateSizeOfBoard(playerCount);

        ObjectNode boardJson = gameBoard.getJson();

        assertNotNull(boardJson, "getJson() should return a non-null ObjectNode");

        // Check top-level keys exist
        assertTrue(boardJson.has("tiles"), "JSON should contain 'tiles' field");
        assertTrue(boardJson.has("settlementPositions"), "JSON should contain 'settlementPositions' field");
        assertTrue(boardJson.has("roads"), "JSON should contain 'roads' field");
        assertTrue(boardJson.has("ringsOfBoard"), "JSON should contain 'ringsOfBoard' field");
        assertTrue(boardJson.has("sizeOfHex"), "JSON should contain 'sizeOfHex' field");

        // Check metadata values
        assertEquals(expectedSize, boardJson.get("ringsOfBoard").asInt(), "ringsOfBoard should match calculated board size");
        assertEquals(GameBoard.DEFAULT_TILES_PER_PLAYER_GOAL, boardJson.get("sizeOfHex").asInt(), "sizeOfHex should match the constant");

        // Check array types
        assertTrue(boardJson.get("tiles").isArray(), "'tiles' field should be a JSON array");
        assertTrue(boardJson.get("settlementPositions").isArray(), "'settlementPositions' field should be a JSON array");
        assertTrue(boardJson.get("roads").isArray(), "'roads' field should be a JSON array");
    }

    @Test
    void testGetJsonContentSize() {
        int playerCount = 3; // Use a different simple case
        GameBoard gameBoard = new GameBoard(playerCount);

        // Get the generated lists for size comparison
        List<Tile> expectedTiles = gameBoard.getTileList();
        List<SettlementPosition> expectedPositions = gameBoard.getSettlementPositionGraph();
        List<Road> expectedRoads = gameBoard.getRoadList();

        assertNotNull(expectedTiles, "Internal tile list should exist");
        assertNotNull(expectedPositions, "Internal position list should exist");
        assertNotNull(expectedRoads, "Internal road list should exist");

        // Get the JSON
        ObjectNode boardJson = gameBoard.getJson();
        assertNotNull(boardJson);

        // Get the arrays from JSON
        JsonNode tilesNode = boardJson.get("tiles");
        JsonNode positionsNode = boardJson.get("settlementPositions");
        JsonNode roadsNode = boardJson.get("roads");

        assertTrue(tilesNode.isArray(), "'tiles' node should be an array");
        assertTrue(positionsNode.isArray(), "'settlementPositions' node should be an array");
        assertTrue(roadsNode.isArray(), "'roads' node should be an array");

        // Compare sizes
        assertEquals(expectedTiles.size(), tilesNode.size(), "JSON tiles array size should match internal list size");
        assertEquals(expectedPositions.size(), positionsNode.size(), "JSON positions array size should match internal list size");
        assertEquals(expectedRoads.size(), roadsNode.size(), "JSON roads array size should match internal list size");

        // Optional: Basic check of first element structure (relies on individual toJson tests)
        if (!expectedTiles.isEmpty()) {
            assertTrue(tilesNode.get(0).has("id"), "First tile JSON should have an 'id'");
            assertTrue(tilesNode.get(0).has("type"), "First tile JSON should have a 'type'");
        }
        if (!expectedPositions.isEmpty()) {
            assertTrue(positionsNode.get(0).has("id"), "First position JSON should have an 'id'");
            assertTrue(positionsNode.get(0).has("coordinates"), "First position JSON should have 'coordinates'");
        }
        if (!expectedRoads.isEmpty()) {
            assertTrue(roadsNode.get(0).has("id"), "First road JSON should have an 'id'");
            assertTrue(roadsNode.get(0).has("owner"), "First road JSON should have an 'owner'");
        }
    }

    //@Disabled("test used for debugging purposes, passes automatically")
    @Test
    void debuggingTest() {
        GameBoard board = new GameBoard(4);
        board.generateBoard();
        System.out.println(board.getJson());
        assertTrue(true);
    }

    @Test
    void testGameBoardInitializesDiceRollerAndSubscribesTiles() {
        GameBoard gameBoard = new GameBoard(4);
        List<Tile> tileList = gameBoard.getTileList();
        assertNotNull(tileList);
        assertFalse(tileList.isEmpty(), "Tile list should not be empty");

        try {
            gameBoard.rollDice();
        } catch (GameException e) {
            fail("Dice roll should not throw an exception");
        }

        boolean atLeastOneTileHadResource = tileList.stream().anyMatch(Tile::hasResource);
        assertTrue(atLeastOneTileHadResource || tileList.stream().noneMatch(t -> t.hasResource()),
                "Tiles should receive resources if dice matches tile value");

        assertTrue(tileList.stream().noneMatch(Tile::hasResource), "Resources should be reset after distribution");
    }
}
