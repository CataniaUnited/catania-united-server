package com.example.cataniaunited.game.board.tile_list_builder;

import java.util.List;

/**
 * Interface for building a list of Catan game tiles.
 * Defines the steps involved in creating a complete and configured tile list,
 */
public interface TileListBuilder {
    /**
     * Resets the builder to its initial default state, clearing any previous configurations or results.
     */
    void reset();

    /**
     * Sets the configuration parameters for the tile list generation.
     *
     * @param sizeOfBoard The size of the board (number of rings/layers).
     * @param sizeOfHex   The size parameter of a single hexagon tile (e.g., side length).
     * @param flipYAxis   Whether to flip the Y-axis for coordinate calculations.
     */
    void setConfiguration(int sizeOfBoard, int sizeOfHex, boolean flipYAxis);

    /**
     * Creates the basic tile objects with their respective types (e.g., WHEAT, WOOD, DESERT).
     */
    void buildTiles();

    /**
     * Assigns production numbers (dice roll values from 2-6 and 8-12) to the resource-producing tiles.
     */
    void addValues();

    /**
     * Shuffles the order of the generated tiles in the list to randomize their placement on the board.
     */
    void shuffleTiles();

    /**
     * Assigns unique, sequential IDs to each tile in the list.
     */
    void assignTileIds();

    /**
     * Calculates and assigns 2D coordinates to each tile, arranging them in a hexagonal grid pattern.
     */
    void calculateTilePositions();

    /**
     * Retrieves the final list of generated and configured tiles.
     *
     * @return A list of {@link Tile} objects.
     */
    List<Tile> getTileList();
}