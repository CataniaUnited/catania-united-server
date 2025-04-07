package com.example.cataniaunited.game.board.tile_list_builder;

import java.util.List;

public interface TileListBuilder {
    void reset();

    void setConfiguration(int sizeOfBoard, int sizeOfHex, boolean flipYAxis);

    void buildTiles();

    void shuffleTiles();

    void assignTileIds();

    void calculateTilePositions();

    List<Tile> getTileList();
}