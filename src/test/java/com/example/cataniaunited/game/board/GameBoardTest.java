package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.buildings.Settlement;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
        } catch(Exception e) {
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

    static Stream<Arguments> playerCountProvider(){
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
    void generateBoardThrowsExceptionIfGeneratingGraphBeforeTileList(){
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
    void benchMarkTest(int sizeOfBoard){
        try {
            new GameBoard(sizeOfBoard);
        } catch (OutOfMemoryError e){
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
        GameBoard gameBoard = new GameBoard(2);
        var settlementPosition = gameBoard.getSettlementPositionGraph().getFirst();
        String playerId = "Player1";
        gameBoard.placeSettlement(playerId, settlementPosition.getId());
        assertNotNull(settlementPosition.building);
        assertEquals(Settlement.class, settlementPosition.building.getClass());
        assertEquals(playerId, settlementPosition.building.getOwnerPlayerId());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfPositionIsLessThanZero() {
        GameBoard gameBoard = new GameBoard(2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement("Player1", -1));
        assertEquals("Settlement position not found: id = %s".formatted(-1), ge.getMessage());
    }

    @Test
    void placeSettlementShouldThrowExceptionIfPositionIsBiggerThanSize() {
        GameBoard gameBoard = new GameBoard(2);
        int positionId = gameBoard.getSettlementPositionGraph().size() + 1;
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement("Player1", positionId));
        assertEquals("Settlement position not found: id = %s".formatted(positionId), ge.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidPlayerIds")
    void placeSettlementShouldThrowExceptionIfPlayerIdIsEmpty(String playerId){
        GameBoard gameBoard = new GameBoard(2);
        int positionId = gameBoard.getSettlementPositionGraph().getFirst().getId();
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeSettlement(playerId, positionId));
        assertEquals("Owner Id of building must not be empty", ge.getMessage());
    }

    @Test
    void testPlaceRoad() throws GameException {
        GameBoard gameBoard = new GameBoard(2);
        var road = gameBoard.roadList.getFirst();
        String playerId = "Player1";
        gameBoard.placeRoad(playerId, road.getId());
        assertEquals(playerId, road.ownerPlayerId);
    }

    @Test
    void placeRoadShouldThrowExceptionIfRoadIdIsLessThanZero() {
        GameBoard gameBoard = new GameBoard(2);
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad("Player1", -1));
        assertEquals("Road not found: id = %s".formatted(-1), ge.getMessage());
    }

    @Test
    void placeRoadShouldThrowExceptionIfRoadIdIsGreaterThanSize() {
        GameBoard gameBoard = new GameBoard(2);
        int roadId = gameBoard.roadList.size() + 1;
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad("Player1", roadId));
        assertEquals("Road not found: id = %s".formatted(roadId), ge.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidPlayerIds")
    void placeRoadShouldThrowExceptionIfPlayerIdIsEmpty(String playerId){
        GameBoard gameBoard = new GameBoard(2);
        var road = gameBoard.roadList.getFirst();
        GameException ge = assertThrows(GameException.class, () -> gameBoard.placeRoad(playerId, road.getId()));
        assertEquals("Owner Id of road must not be empty", ge.getMessage());
    }

    static Stream<Arguments> invalidPlayerIds(){
        return Stream.of(null, Arguments.of(""));
    }

    @Disabled("test used for debugging purposes, passes automatically")
    @Test
    void debuggingTest(){
        GameBoard board = new GameBoard(4);
        board.generateBoard();
        List<SettlementPosition> graph = board.getSettlementPositionGraph();
        for (SettlementPosition node: graph){
            System.out.println(node);
        }
        assertTrue(true);
    }

}
