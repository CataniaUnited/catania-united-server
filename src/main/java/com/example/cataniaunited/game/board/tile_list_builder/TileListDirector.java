package com.example.cataniaunited.game.board.tile_list_builder;

public class TileListDirector {
    TileListBuilder builder;

    public TileListDirector(TileListBuilder builder) {
        if (builder == null){
            throw new NullPointerException("Builder cannot be null");
        }
        this.builder = builder;
    }

    public void setBuilder(TileListBuilder builder) {
        if (builder == null){
            throw new NullPointerException("Builder cannot be null");
        }
        this.builder = builder;
    }

    public void constructStandardTileList(int sizeOfBoard, int sizeOfHex, boolean flipYAxis) {
        builder.reset();
        builder.setConfiguration(sizeOfBoard, sizeOfHex, flipYAxis);
        builder.buildTiles();
        builder.shuffleTiles();
        builder.assignTileIds();
        builder.calculateTilePositions();
    }
}
