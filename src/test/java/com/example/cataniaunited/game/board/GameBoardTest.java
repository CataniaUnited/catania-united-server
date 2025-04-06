package com.example.cataniaunited.game.board;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;


@QuarkusTest
public class GameBoardTest {
    @Test
    void michi(){
        GameBoard board = new GameBoard(4, true);
    }

    @ParameterizedTest
    @MethodSource("benchMarkTestProvider")
    void benchMarkTest(int sizeOfBoard){
        try {
            new GameBoard(sizeOfBoard, false);
        } catch (OutOfMemoryError e){
            Assertions.assertFalse(true);
        }

    }

    static Stream<Arguments> benchMarkTestProvider() {
        return Stream.of(
                Arguments.of(1),
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

    @Test void testCoordinateCalculationOfTiles(){
        int sizeOfBoard = 4;
        List<Tile> tileList = new TileListGenerator(sizeOfBoard, 10, true).generateShuffledTileList();
        for(Tile node:tileList){
            System.out.println(node);
        }
    }

    @Test
    void testTileCoordinateSettercalculateAmountOfTilesForLayerK(){
        Assertions.assertEquals(1, TileListGenerator.calculateAmountOfTilesForLayerK(1));
        Assertions.assertEquals(7, TileListGenerator.calculateAmountOfTilesForLayerK(2));
        Assertions.assertEquals(19, TileListGenerator.calculateAmountOfTilesForLayerK(3));
        Assertions.assertEquals(37, TileListGenerator.calculateAmountOfTilesForLayerK(4));
    }

    @ParameterizedTest
    @MethodSource("duplicateListProvider")
    void findDuplicateTileListTest(List<Tile> tileList, Tile duplicateTile){
        Assertions.assertEquals(GraphBuilder.findDuplicateTile(tileList), duplicateTile);
    }

    static Stream<Arguments> duplicateListProvider(){
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
                Arguments.of(List.of(otherTile1, otherTile2, duplicateTile,  otherTile3, duplicateTile), duplicateTile),

                // fourth element is duplicate
                Arguments.of(List.of(otherTile1, otherTile2, otherTile3, duplicateTile, duplicateTile), duplicateTile)
        );
    }
}
