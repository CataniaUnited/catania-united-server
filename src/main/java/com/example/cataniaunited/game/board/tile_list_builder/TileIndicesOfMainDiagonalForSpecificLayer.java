package com.example.cataniaunited.game.board.tile_list_builder;

/**
 * A data structure to hold key tile indices relevant to constructing a specific layer
 * of the hexagonal grid. These indices primarily define the anchor points for the
 * South-East (SE) and North-West (NW) "main diagonals" of the layer, from which
 * other tile positions are calculated.
 * <p>
 * Indices are 0-based and refer to positions within the {@code tileList}.
 * "Current Layer" refers to the layer being processed in the iteration.
 * "Previous Layer" refers to the layer immediately inside the current layer.
 */
record TileIndicesOfMainDiagonalForSpecificLayer(
        /*
         * Index of the tile that serves as the South-East (SE) anchor for the current layer.
         * This tile's position is typically calculated relative to the SE anchor of the previous layer.
         * It is often the first tile encountered when spiraling outwards to build the layer.
         * Calculated as {@code calculateAmountOfTilesForLayerK(currentLayerNumber)}.
         */
        int firstTileOfCurrentLayer, // Represents the SE anchor of the current layer

        /*
         * Index of the tile that served as the South-East (SE) anchor for the previous layer.
         * Used as a reference to position the {@code firstTileOfCurrentLayer}.
         * Calculated as {@code calculateAmountOfTilesForLayerK(currentLayerNumber - 1)}.
         */
        int firstTileOfPreviousLayer, // Represents the SE anchor of the previous layer

        /*
         * Index of the tile that serves as the North-West (NW) anchor for the current layer.
         * This tile's position is typically calculated relative to the NW anchor of the previous layer.
         * This forms the other end of the main diagonal calculation for the layer.
         * Calculated using {@code getIndexOfMiddleTileOfLayerK.applyAsInt(currentLayerNumber)}.
         */
        int middleTileOfCurrentLayer, // Represents the NW anchor of the current layer

        /*
         * Index of the tile that served as the North-West (NW) anchor for the previous layer.
         * Used as a reference to position the {@code middleTileOfCurrentLayer}.
         * Calculated using {@code getIndexOfMiddleTileOfLayerK.applyAsInt(currentLayerNumber - 1)}.
         */
        int middleTileOfPreviousLayer // Represents the NW anchor of the previous layer
) {}
