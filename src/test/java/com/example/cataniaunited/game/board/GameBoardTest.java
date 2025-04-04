package com.example.cataniaunited.game.board;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


@QuarkusTest
public class GameBoardTest {
    @ParameterizedTest
    @MethodSource("duplicateListProvider")
    void findDuplicateTileListTest(List<Tile> tileList, Tile duplicateTile){
        Assertions.assertEquals(GameBoard.findDuplicateTile(tileList), duplicateTile);
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
