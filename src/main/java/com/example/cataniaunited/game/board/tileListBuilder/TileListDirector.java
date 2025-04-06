package com.example.cataniaunited.game.board.tileListBuilder;

import java.util.Objects;

public class TileListDirector {
    private TileListBuilder builder;

    public TileListDirector(TileListBuilder builder) {
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
    }

    public void setBuilder(TileListBuilder builder) {
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
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
