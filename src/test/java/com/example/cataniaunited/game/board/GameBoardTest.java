package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.ui.BuildableLimitReachedException;
import com.example.cataniaunited.exception.ui.InsufficientResourcesException;
import com.example.cataniaunited.game.BuildRequest;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.buildings.City;
import com.example.cataniaunited.game.buildings.Settlement;
import com.example.cataniaunited.player.Player;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            fail("GameBoard constructor threw an unexpected exception.");
        }

        assertNotNull(gameBoard, "GameBoard instance should be created");
        assertEquals(GameBoard.calculateSizeOfBoard(playerCount), gameBoard.sizeOfBoard, "Internal board size should be set correctly");

        List<Tile> tiles = gameBoard.getTileList();
        List<BuildingSite> graph = gameBoard.getBuildingSitePositionGraph();
        List<Port> ports = gameBoard.portList;

        // More Detailed tests have been conducted in the respective test classes
        assertNotNull(tiles, "Generated tile list should not be null");
        assertNotNull(graph, "Generated settlement graph should not be null");
        assertFalse(tiles.isEmpty(), "Generated tile list should not be empty");
        assertFalse(graph.isEmpty(), "Generated settlement graph should not be empty");
        assertFalse(ports.isEmpty(), "Generated Port List should not be empty");
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
    void benchMarkTest(int playerCount) {
        try {
            new GameBoard(playerCount);
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
        Player player = new Player();
        player.receiveResource(TileType.WOOD, 1);
        player.receiveResource(TileType.CLAY, 1);
        player.receiveResource(TileType.WHEAT, 1);
        player.receiveResource(TileType.SHEEP, 1);
        String playerId = player.getUniqueId();
        GameBoard gameBoard = new GameBoard(2);
        var settlementPosition = gameBoard.getBuildingSitePositionGraph().get(0);
        Road road = settlementPosition.roads.get(0);
        road.setOwner(player);

        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, settlementPosition.getId(), true, 2);
        gameBoard.placeSettlement(buildRequest);
        assertNotNull(settlementPosition.building);
        assertEquals(Settlement.class, settlementPosition.building.getClass());
        assertEquals(playerId, settlementPosition.building.getPlayer().getUniqueId());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfPositionIsLessThanZero() {
        GameBoard gameBoard = new GameBoard(2);
        Player player = new Player("Player1");
        player.receiveResource(TileType.WOOD, 1);
        player.receiveResource(TileType.CLAY, 1);
        player.receiveResource(TileType.WHEAT, 1);
        player.receiveResource(TileType.SHEEP, 1);
        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, -1, true, 2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement(buildRequest));
        assertEquals("Settlement position not found: id = %s".formatted(-1), ge.getMessage());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfPositionIsBiggerThanSize() {
        GameBoard gameBoard = new GameBoard(2);
        int positionId = gameBoard.getBuildingSitePositionGraph().size() + 1;
        Player player = new Player("Player1");
        player.receiveResource(TileType.WOOD, 1);
        player.receiveResource(TileType.CLAY, 1);
        player.receiveResource(TileType.WHEAT, 1);
        player.receiveResource(TileType.SHEEP, 1);
        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, positionId, true, 2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement(buildRequest));
        assertEquals("Settlement position not found: id = %s".formatted(positionId), ge.getMessage());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfPlayerIsNull() {
        GameBoard gameBoard = new GameBoard(2);
        int positionId = gameBoard.getBuildingSitePositionGraph().get(0).getId();
        var buildRequest = new BuildRequest(null, PlayerColor.LIGHT_ORANGE, positionId, true, 2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement(buildRequest));
        assertEquals("Owner of building must not be empty", ge.getMessage());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfMaxLimitIsReached() {
        GameBoard gameBoard = spy(new GameBoard(2));
        int positionId = gameBoard.getBuildingSitePositionGraph().size() + 1;
        Player player = new Player("Player1");
        player.receiveResource(TileType.WOOD, 1);
        player.receiveResource(TileType.CLAY, 1);
        player.receiveResource(TileType.WHEAT, 1);
        player.receiveResource(TileType.SHEEP, 1);
        doReturn(5L).when(gameBoard).getPlayerStructureCount(player.getUniqueId(), Settlement.class);
        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, positionId, true, 2);
        GameException ge = assertThrows(BuildableLimitReachedException.class, () -> gameBoard.placeSettlement(buildRequest));

        assertEquals("You've reached the %s limit of %s!".formatted(Settlement.class.getSimpleName(), 5), ge.getMessage());
        verify(gameBoard).getPlayerStructureCount(player.getUniqueId(), Settlement.class);
    }

    @Test
    void placeCityShouldThrowExceptionIfMaxLimitIsReached() {
        GameBoard gameBoard = spy(new GameBoard(2));
        int positionId = gameBoard.getBuildingSitePositionGraph().size() + 1;
        Player player = new Player("Player1");
        player.receiveResource(TileType.ORE, 3);
        player.receiveResource(TileType.WHEAT, 2);
        doReturn(4L).when(gameBoard).getPlayerStructureCount(player.getUniqueId(), City.class);
        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, positionId, true, 2);
        GameException ge = assertThrows(BuildableLimitReachedException.class, () -> gameBoard.placeCity(buildRequest));

        assertEquals("You've reached the %s limit of %s!".formatted(City.class.getSimpleName(), 4), ge.getMessage());
        verify(gameBoard).getPlayerStructureCount(player.getUniqueId(), City.class);
    }

    @Test
    void placeBuildingShouldThrowExceptionIfPlayerDoesNotHaveResources() {
        GameBoard gameBoard = new GameBoard(2);
        int positionId = gameBoard.getBuildingSitePositionGraph().get(0).getId();
        Player mockPlayer = spy(new Player("Player1"));
        when(mockPlayer.getResourceCount(any(TileType.class))).thenReturn(0);
        var buildRequest = new BuildRequest(mockPlayer, PlayerColor.LIGHT_ORANGE, positionId, true, 2);
        GameException ge = assertThrows(InsufficientResourcesException.class, () -> gameBoard.placeSettlement(buildRequest));
        assertEquals("Insufficient resources!", ge.getMessage());
    }

    @Test
    void testPlaceRoad() throws GameException {
        GameBoard gameBoard = new GameBoard(2);
        var road = gameBoard.roadList.get(0);
        Player player = new Player("Player1");
        player.receiveResource(TileType.WOOD, 1);
        player.receiveResource(TileType.CLAY, 1);
        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, road.getId(), true, 2);
        gameBoard.placeRoad(buildRequest);
        assertEquals(player, road.getOwner());
    }

    @Test
    void placeRoadShouldThrowExceptionIfRoadIdIsLessThanZero() {
        GameBoard gameBoard = new GameBoard(2);
        var buildRequest = new BuildRequest(new Player("Player1"), PlayerColor.LIGHT_ORANGE, -1, true, 2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad(buildRequest));
        assertEquals("Road not found: id = %s".formatted(-1), ge.getMessage());
    }

    @Test
    void placeRoadShouldThrowExceptionIfRoadIdIsGreaterThanSize() {
        GameBoard gameBoard = new GameBoard(2);
        int roadId = gameBoard.roadList.size() + 1;
        var buildRequest = new BuildRequest(new Player("Player1"), PlayerColor.LIGHT_ORANGE, roadId, true, 2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad(buildRequest));
        assertEquals("Road not found: id = %s".formatted(roadId), ge.getMessage());
    }

    @Test
    void placeRoadShouldThrowExceptionIfPlayerIsNull() {
        GameBoard gameBoard = new GameBoard(2);
        var road = gameBoard.roadList.get(0);
        var buildRequest = new BuildRequest(null, PlayerColor.LIGHT_ORANGE, road.getId(), true, 2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad(buildRequest));
        assertEquals("Player must not be null", ge.getMessage());
    }

    @Test
    void placeRoadShouldThrowExceptionIfMaxLimitIsReached() {
        GameBoard gameBoard = spy(new GameBoard(2));
        var road = gameBoard.roadList.get(0);
        var player = new Player("Player1");
        player.receiveResource(TileType.WOOD, 1);
        player.receiveResource(TileType.CLAY, 1);
        doReturn((long) road.getBuildLimit()).when(gameBoard).getPlayerStructureCount(anyString(), eq(Road.class));
        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, road.getId(), true, 2);
        GameException ge = assertThrows(BuildableLimitReachedException.class, () -> gameBoard.placeRoad(buildRequest));
        assertEquals("You've reached the %s limit of %s!".formatted(Road.class.getSimpleName(), road.getBuildLimit()), ge.getMessage());
    }

    @Test
    void testUpdateOfPlayerStructures() throws GameException {
        GameBoard gameBoard = spy(new GameBoard(2));
        BuildingSite buildingSite1 = gameBoard.getBuildingSitePositionGraph().get(0);
        BuildingSite buildingSite2 = gameBoard.getBuildingSitePositionGraph().get(9);
        Road road1 = buildingSite1.getRoads().get(0);
        Road road2 = buildingSite2.getRoads().get(1);
        int buildingSite1Id = buildingSite1.getId();
        int buildingSite2Id = buildingSite2.getId();
        Player player = new Player("Player1");
        player.receiveResource(TileType.WOOD, 4);
        player.receiveResource(TileType.CLAY, 4);
        player.receiveResource(TileType.SHEEP, 2);
        player.receiveResource(TileType.ORE, 3);
        player.receiveResource(TileType.WHEAT, 4);

        var roadCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Road.class);
        var cityCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), City.class);
        var settlementCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Settlement.class);

        assertEquals(0, roadCount);
        assertEquals(0, cityCount);
        assertEquals(0, settlementCount);

        var buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, road1.getId(), true, 2);
        gameBoard.placeRoad(buildRequest);

        roadCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Road.class);
        cityCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), City.class);
        settlementCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Settlement.class);

        assertEquals(1, roadCount);
        assertEquals(0, cityCount);
        assertEquals(0, settlementCount);

        buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, buildingSite1Id, true, 2);
        gameBoard.placeSettlement(buildRequest);

        roadCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Road.class);
        cityCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), City.class);
        settlementCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Settlement.class);

        assertEquals(1, roadCount);
        assertEquals(0, cityCount);
        assertEquals(1, settlementCount);

        buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, road2.getId(), true, 2);
        gameBoard.placeRoad(buildRequest);

        roadCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Road.class);
        cityCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), City.class);
        settlementCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Settlement.class);

        assertEquals(2, roadCount);
        assertEquals(0, cityCount);
        assertEquals(1, settlementCount);

        buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, buildingSite2Id, true, 2);
        gameBoard.placeSettlement(buildRequest);

        roadCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Road.class);
        cityCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), City.class);
        settlementCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Settlement.class);

        assertEquals(2, roadCount);
        assertEquals(0, cityCount);
        assertEquals(2, settlementCount);

        buildRequest = new BuildRequest(player, PlayerColor.LIGHT_ORANGE, buildingSite1Id, true, 2);
        gameBoard.placeCity(buildRequest);

        roadCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Road.class);
        cityCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), City.class);
        settlementCount = gameBoard.getPlayerStructureCount(player.getUniqueId(), Settlement.class);

        assertEquals(2, roadCount);
        assertEquals(1, cityCount);
        assertEquals(1, settlementCount);


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
        assertTrue(boardJson.has("ports"), "JSON should contain 'ports' field");
        assertTrue(boardJson.has("ringsOfBoard"), "JSON should contain 'ringsOfBoard' field");
        assertTrue(boardJson.has("sizeOfHex"), "JSON should contain 'sizeOfHex' field");

        // Check metadata values
        assertEquals(expectedSize, boardJson.get("ringsOfBoard").asInt(), "ringsOfBoard should match calculated board size");
        assertEquals(GameBoard.DEFAULT_TILES_PER_PLAYER_GOAL, boardJson.get("sizeOfHex").asInt(), "sizeOfHex should match the constant");

        // Check array types
        assertTrue(boardJson.get("tiles").isArray(), "'tiles' field should be a JSON array");
        assertTrue(boardJson.get("settlementPositions").isArray(), "'settlementPositions' field should be a JSON array");
        assertTrue(boardJson.get("roads").isArray(), "'roads' field should be a JSON array");
        assertTrue(boardJson.get("ports").isArray(), "'ports' field should be a JSON array");
    }

    @Test
    void testGetJsonContentSize() {
        int playerCount = 3; // Use a different simple case
        GameBoard gameBoard = new GameBoard(playerCount);

        // Get the generated lists for size comparison
        List<Tile> expectedTiles = gameBoard.getTileList();
        List<BuildingSite> expectedPositions = gameBoard.getBuildingSitePositionGraph();
        List<Road> expectedRoads = gameBoard.getRoadList();
        List<Port> expectedPorts = gameBoard.portList;

        assertNotNull(expectedTiles, "Internal tile list should exist");
        assertNotNull(expectedPositions, "Internal position list should exist");
        assertNotNull(expectedRoads, "Internal road list should exist");
        assertNotNull(expectedPorts, "Internal Port list should exist");

        // Get the JSON
        ObjectNode boardJson = gameBoard.getJson();
        assertNotNull(boardJson);

        // Get the arrays from JSON
        JsonNode tilesNode = boardJson.get("tiles");
        JsonNode positionsNode = boardJson.get("settlementPositions");
        JsonNode roadsNode = boardJson.get("roads");
        JsonNode portsNode = boardJson.get("ports");

        assertTrue(tilesNode.isArray(), "'tiles' node should be an array");
        assertTrue(positionsNode.isArray(), "'settlementPositions' node should be an array");
        assertTrue(roadsNode.isArray(), "'roads' node should be an array");
        assertTrue(portsNode.isArray(), "'ports' node should be an array");

        // Compare sizes
        assertEquals(expectedTiles.size(), tilesNode.size(), "JSON tiles array size should match internal list size");
        assertEquals(expectedPositions.size(), positionsNode.size(), "JSON positions array size should match internal list size");
        assertEquals(expectedRoads.size(), roadsNode.size(), "JSON roads array size should match internal list size");
        assertEquals(expectedPorts.size(), portsNode.size(), "JSON roads array size should match internal list size");

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

    /**
     * Test for debugging Purposes, generates a GameBoard and Passes Automatically. Used to get information of a generated board
     * For example an example JSON ...
     */
    @Disabled("For Debugging Purposes")
    @Test
    void debuggingTest() {
        GameBoard board = new GameBoard(4);
        board.generateBoard();
        assertTrue(true);
    }
}
