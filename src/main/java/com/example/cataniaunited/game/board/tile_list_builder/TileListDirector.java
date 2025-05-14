package com.example.cataniaunited.game.board.tile_list_builder;

/**
 * Director class in the Builder pattern for constructing a list of Catan game tiles
 * using a {@link TileListBuilder}.
 */
public class TileListDirector {
    TileListBuilder builder;

    /**
     * Constructs a TileListDirector with a specified {@link TileListBuilder}.
     *
     * @param builder The {@link TileListBuilder} to use for constructing the tile list.
     * @throws NullPointerException if the builder is null.
     */
    public TileListDirector(TileListBuilder builder) {
        if (builder == null){
            throw new NullPointerException("Builder cannot be null");
        }
        this.builder = builder;
    }

    /**
     * Sets or changes the {@link TileListBuilder} to be used by the director.
     *
     * @param builder The new {@link TileListBuilder}.
     * @throws NullPointerException if the builder is null.
     */
    public void setBuilder(TileListBuilder builder) {
        if (builder == null){
            throw new NullPointerException("Builder cannot be null");
        }
        this.builder = builder;
    }

    /**
     * Constructs a standard list of Catan game tiles using the configured builder.
     *
     * @param sizeOfBoard The size of the board (number of rings/layers).
     * @param sizeOfHex   The size of a single hexagon tile.
     * @param flipYAxis   Whether to flip the Y-axis for coordinate calculations.
     */
    public void constructStandardTileList(int sizeOfBoard, int sizeOfHex, boolean flipYAxis) {
        builder.reset();
        builder.setConfiguration(sizeOfBoard, sizeOfHex, flipYAxis);
        builder.buildTiles();
        builder.addValues();
        builder.shuffleTiles();
        builder.assignTileIds();
        builder.calculateTilePositions();
    }
}