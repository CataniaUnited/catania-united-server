package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.example.cataniaunited.util.CatanBoardUtils.calculateAmountOfTilesForLayerK;
import static com.example.cataniaunited.util.CatanBoardUtils.polarToCartesian;

// fixme avoid long methods with hard to understand functionality. can you improve understandability with naming or submethods?

/**
 * A standard implementation of {@link TileListBuilder} for creating a list of Catan tiles.
 * This builder handles tile creation, value assignment, shuffling, ID assignment,
 * and calculation of tile positions on a hexagonal grid.
 */
public class StandardTileListBuilder implements TileListBuilder {

    int sizeOfBoard;
    int sizeOfHex;
    boolean flipYAxis;

    int amountOfTilesOnBoard;
    double distanceBetweenTiles;
    double[] northWestAddition;
    double[] southEastAddition;

    // precomputed values of the angle to get to the midpoint of the starting tile of the next layer
    // to prevent rounding errors used Bigdecimal to compute the values and then transformed them to with k.doubleValue()
    private static final double NORTH_WEST_ANGLE = 2.0943951023931957; // k = 2
    private static final double SOUTH_EAST_ANGLE = 5.235987755982989; // k = 5

    // --- Constants for Production Number Generation ---
    private static final int MIN_DICE_VALUE = 2; // Having two die the lowest number one can roll is 2
    private static final int MAX_DICE_VALUE = 12; // Having two die the highest number one can roll is 12
    private static final int ROBBER_ACTIVATION_ROLL = 7; // This roll number does not produce resources, it activates the robber
    private static final int COUNT_OF_DISTINCT_PRODUCTION_NUMBERS = 10; // all numbers that produce resources (-> all numbers except 7 -> 10 Numbers) (2,3,4,5,6,8,9,10,11,12)

    List<Tile> tileList;

    /**
     * Constructs a new StandardTileListBuilder and initializes its state by calling {@link #reset()}.
     */
    public StandardTileListBuilder() {
        this.reset(); // Initialize and Reset
    }

    /**
     * Resets the builder to its initial state, clearing any previously configured
     * settings or generated tile list.
     */
    @Override
    public void reset() {
        this.sizeOfBoard = 0;
        this.sizeOfHex = 0;
        this.flipYAxis = true;
        this.tileList = new ArrayList<>();
        this.amountOfTilesOnBoard = 0;
        this.distanceBetweenTiles = 0;
        this.northWestAddition = null;
        this.southEastAddition = null;
    }

    /**
     * Sets the configuration parameters for the tile list generation.
     *
     * @param sizeOfBoard The size of the board (number of rings/layers).
     * @param sizeOfHex   The size parameter of a single hexagon tile (e.g., side length or apothem), used for coordinate calculation.
     * @param flipYAxis   Whether to flip the Y-axis for coordinate calculations (true for typical screen coordinates).
     * @throws IllegalArgumentException if sizeOfBoard or sizeOfHex is not positive.
     */
    @Override
    public void setConfiguration(int sizeOfBoard, int sizeOfHex, boolean flipYAxis) {
        if (sizeOfBoard <= 0 || sizeOfHex <= 0) {
            throw new IllegalArgumentException("Board size and hex size must be positive.");
        }
        this.sizeOfBoard = sizeOfBoard;
        this.sizeOfHex = sizeOfHex;
        this.flipYAxis = flipYAxis;

        this.distanceBetweenTiles = StrictMath.sqrt(3) * this.sizeOfHex; // Distance between centers of adjacent hexes
        amountOfTilesOnBoard = calculateAmountOfTilesForLayerK(sizeOfBoard);

        // precomputing offset since working with bigDecimalObjects takes time
        northWestAddition = polarToCartesian(distanceBetweenTiles, NORTH_WEST_ANGLE, this.flipYAxis);
        southEastAddition = polarToCartesian(distanceBetweenTiles, SOUTH_EAST_ANGLE, this.flipYAxis);

        tileList = new ArrayList<>(amountOfTilesOnBoard);
    }

    /**
     * Builds the initial list of tiles with their types.
     * One tile is designated as WASTE (desert), and the rest are populated with
     * resource-producing tile types in a repeating sequence.
     *
     * @throws IllegalStateException if configuration has not been set, if no usable
     *                               (non-WASTE) tile types are available or Only the Waste Tile is available.
     */
    @Override
    public void buildTiles() {
        if (this.tileList == null || amountOfTilesOnBoard <= 0) {
            throw new IllegalStateException("Configuration must be set before building tiles.");
        }

        TileType[] availableTypes = TileType.values();
        int usableTypeCount = availableTypes.length - 1; // Assuming one WASTE type (last index)
        if (usableTypeCount <= 0) throw new IllegalStateException("Requires non-WASTE types.");
        if (availableTypes[usableTypeCount] != TileType.WASTE)
            throw new IllegalStateException("WASTE-Type needs to be at last position.");


        tileList.add(new Tile(TileType.WASTE));
        for (int i = 0; i < amountOfTilesOnBoard - 1; i++) {
            TileType currentTileType = availableTypes[i % (usableTypeCount)];
            tileList.add(new Tile(currentTileType));
        }
    }

    /**
     * Assigns production numbers (dice roll values) to the non-WASTE tiles.
     * Aims for a balanced distribution of numbers 2-6 and 8-12.
     *
     * @throws IllegalStateException if tiles have not been built yet.
     */
    @Override
    public void addValues() {
        if (Util.isEmpty(tileList)) {
            throw new IllegalStateException("Tiles must be built before assigning values.");
        }

        // Calculate the number of tiles that require a production value.
        // Excluding the TileType.WASTE (Desert) tile since it doesn't produce any resources.
        int amountOfValuesToCreate = 0;
        for (Tile tile : tileList) {
            if (tile.getType() != TileType.WASTE) {
                amountOfValuesToCreate++;
            }
        }

        List<Integer> productionValues = generateShuffledProductionValues(amountOfValuesToCreate);
        Collections.shuffle(productionValues); // Shuffle the list again, since the overhead got added at the end

        int valueIndex = 0;
        for (Tile tile : tileList) {
            if (tile.getType() != TileType.WASTE) { // Do not add a value for Waste tiles since they dont produce resources
                tile.setValue(productionValues.get(valueIndex++));
            }
        }
    }


    /**
     * Shuffles the order of the generated tiles in the list.
     *
     * @throws IllegalStateException if tiles have not been built yet.
     */
    @Override
    public void shuffleTiles() {
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before shuffling.");
        }


        Collections.shuffle(this.tileList);
    }

    /**
     * Assigns unique, sequential IDs to each tile in the list.
     *
     * @throws IllegalStateException if tiles have not been built or the list is empty.
     */
    @Override
    public void assignTileIds() {
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before assigning IDs.");
        }

        for (int i = 0; i < this.tileList.size(); i++) {
            this.tileList.get(i).setId(i + 1);
        }
    }

    /**
     * Calculates and assigns 2D coordinates to each tile, arranging them in a hexagonal grid pattern.
     *
     * @throws IllegalStateException if configuration has not been set, or if tiles have not been built.
     */
    @Override
    public void calculateTilePositions() {
        if (this.northWestAddition == null || this.southEastAddition == null) {
            throw new IllegalStateException("Configuration must be set before building tiles.");
        }
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before calculating coordinates.");
        }

        IntUnaryOperator getIndexOfMiddleElementOfLayerK = k ->
                (calculateAmountOfTilesForLayerK(k + 1) - calculateAmountOfTilesForLayerK(k)) / 2
                        + calculateAmountOfTilesForLayerK(k);

        // set coordinates of center Tile
        tileList.get(0).setCoordinates(0, 0);

        if (this.tileList.size() == 1) { // If the size is one, there is only one Tile
            return;
        }

        // Set Middle row coordinates
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 2, 7, -1, true);
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 5, 10, -1, false);

        // fixme add datastructure for this set of recurring ints, and improve naming for readability
        // set coordinates for tiles in every row
        int indexOfFirstTileOfThisLayer;
        int indexOfFirstTileOfPreviousLayer;
        int indexOfMiddleTileOfThisLayer;
        int indexOfMiddleTileOfPreviousLayer;

        for (int layerIndex = 1; layerIndex < sizeOfBoard; layerIndex++) {
            // get indices of current Layer
            indexOfFirstTileOfThisLayer = calculateAmountOfTilesForLayerK(layerIndex); // index of current Tile regarding tileList
            indexOfFirstTileOfPreviousLayer = calculateAmountOfTilesForLayerK(layerIndex - 1); // amount of tiles placed before-1 to get index
            indexOfMiddleTileOfThisLayer = getIndexOfMiddleElementOfLayerK.applyAsInt(layerIndex);
            indexOfMiddleTileOfPreviousLayer = getIndexOfMiddleElementOfLayerK.applyAsInt(layerIndex - 1);

            // calculate next tiles on diagonal
            addCoordinatesToMainDiagonal(indexOfFirstTileOfThisLayer,
                    indexOfFirstTileOfPreviousLayer,
                    indexOfMiddleTileOfThisLayer,
                    indexOfMiddleTileOfPreviousLayer);

            // calculate rows depending on newly discovered diagonal tiles
            addCoordinatesToRows(layerIndex, indexOfFirstTileOfThisLayer, indexOfMiddleTileOfThisLayer);
        }

    }

    /**
     * Retrieves the list of generated and configured tiles.
     *
     * @return The list of {@link Tile} objects.
     * @throws IllegalStateException if tiles have not been built or the list is empty.
     */
    @Override
    public List<Tile> getTileList() {
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before returning coordinates.");
        }

        return this.tileList;
    }

    /**
     * Generates a list of production numbers intended for tile assignment.
     * The method aims for a somewhat balanced distribution.
     *
     * @param numberOfValuesToGenerate The total count of production numbers needed (typically total tiles - 1 for desert).
     * @return A list of integers representing the production numbers, ready to be shuffled and assigned.
     */
    private List<Integer> generateShuffledProductionValues(int numberOfValuesToGenerate) {
        if (numberOfValuesToGenerate <= 0) {
            return new ArrayList<>(); // Return empty if no values are needed
        }

        // Calculate how many times each distinct production number (2-6, 8-12) should appear at a minimum.
        // There are 10 such distinct numbers.
        int baseRepetitionsPerNumber = numberOfValuesToGenerate / COUNT_OF_DISTINCT_PRODUCTION_NUMBERS;

        List<Integer> coreProductionValues = new ArrayList<>(numberOfValuesToGenerate);

        // This list will hold "extra" copies of each production number.
        // These are used to fill up the list if numberOfValuesToGenerate is not a neat multiple of 10.
        // Each distinct production number gets one slot in this "overhead" list initially.
        // To ensure a fair distribution (19 Numbers, all once, 9 twice and not all once but one number 10 times)
        List<Integer> overheadProductionValues = new ArrayList<>(COUNT_OF_DISTINCT_PRODUCTION_NUMBERS);

        int currentDiceValue = MIN_DICE_VALUE;
        while (currentDiceValue <= MAX_DICE_VALUE) {
            if (currentDiceValue == ROBBER_ACTIVATION_ROLL) {
                currentDiceValue++; // Skip 7, as it activates the robber, not production.
                continue;
            }

            // Add the number of repetitions for the current dice value.
            for (int i = 0; i < baseRepetitionsPerNumber; i++) {
                coreProductionValues.add(currentDiceValue);
            }

            // Add one instance of the current dice value to the overhead list.
            // This ensures each production number is available at once beyond the base repetitions,
            // helping to cover remainders.
            overheadProductionValues.add(currentDiceValue);

            currentDiceValue++;
        }

        // Shuffle both lists before combining. To ensure random distribution
        Collections.shuffle(coreProductionValues);
        Collections.shuffle(overheadProductionValues);

        // Add the (shuffled) overhead values to the main list.
        // This ensures there are enough total values, even if numberOfValuesToGenerate wasn't a perfect multiple.
        // The overhead list acts as a pool to draw from to meet the total required count.
        // If coreProductionValues.size() < numberOfValuesToGenerate, the needed difference will be
        // taken from the beginning of the shuffled overheadProductionValues.
        coreProductionValues.addAll(overheadProductionValues);

        // Ensure the final list is exactly the size requested.
        // If more values were generated than needed (due to adding all of overhead), trim it.
        if (coreProductionValues.size() > numberOfValuesToGenerate) {
            return new ArrayList<>(coreProductionValues.subList(0, numberOfValuesToGenerate));
        }

        return coreProductionValues;
    }

    // --- coordinate calculation ---

    /**
     * Adds coordinates to tiles forming rows radiating from diagonal anchor points.
     * This is part of the hexagonal grid coordinate calculation.
     *
     * @param layerIndex                The current layer index (0-based for calculation logic within this method).
     * @param southRowStartingTileIndex Index of the tile starting the "south" radiating row.
     * @param northRowStartingTileIndex Index of the tile starting the "north" radiating row.
     */
    private void addCoordinatesToRows(int layerIndex, int southRowStartingTileIndex, int northRowStartingTileIndex) {
        int layer = layerIndex + 1; // Convert to 1-based layer for offset calculations
        int offset;

        // Add East Part Of South Row
        offset = calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer - 1) + 1;
        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(layer, southRowStartingTileIndex, offset, true);

        // Add West Part Of North Row
        offset = calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer - 1) + 4;
        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(layer, northRowStartingTileIndex, offset, false);

        // Add East part Of North Row
        offset = calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer - 1) + 1;
        addNeighboringTile(layer, northRowStartingTileIndex, offset, layer - 1, true);

        // Add West Part South Row
        offset = calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer - 1) + 4;
        int irregularIndex = calculateAmountOfTilesForLayerK(layer) - 1; // Last tile of current layer
        changeStartingPositionForSouthWestHalfAndMiddleRow(layer, southRowStartingTileIndex, irregularIndex, offset, layer - 2, false);

    }

    /**
     * Helper method to calculate coordinates for a row of tiles where each step (tile)
     * moves into a conceptually new layer or ring further out from the center.
     * Used for specific diagonal/axial lines of tiles in the hexagonal grid.
     *
     * @param startingLayer     The layer number from which this row calculation starts.
     * @param startingTileIndex The index in {@code tileList} of the first tile in this row.
     * @param startingOffset    The initial offset to find the next tile in the sequence. This offset changes for subsequent tiles.
     * @param xIsGettingLarger  Boolean indicating if the x-coordinate should increase (true) or decrease (false) for subsequent tiles.
     */
    private void addCoordinatesForRowWhereEveryStepIsIntoANewLayer(int startingLayer, int startingTileIndex, int startingOffset, boolean xIsGettingLarger) {
        Tile lastTile = tileList.get(startingTileIndex);
        int lastTileIndex = startingTileIndex;
        int currentTileIndex;
        int offset = startingOffset;
        Tile currentTile;
        double[] coordinates;

        for (int currentLayer = startingLayer; currentLayer < sizeOfBoard; currentLayer++) {
            currentTileIndex = lastTileIndex + offset;
            currentTile = tileList.get(currentTileIndex);

            coordinates = lastTile.getCoordinates();
            if (xIsGettingLarger) {
                currentTile.setCoordinates(coordinates[0] + distanceBetweenTiles, coordinates[1]);
            } else {
                currentTile.setCoordinates(coordinates[0] - distanceBetweenTiles, coordinates[1]);
            }

            lastTileIndex = currentTileIndex;
            lastTile = currentTile;
            offset += 6; // Standard offset increase for hexagonal grid layers
        }
    }

    /**
     * Helper method to calculate coordinates for a sequence of neighboring tiles along a specific axis,
     * and then continue with {@link #addCoordinatesForRowWhereEveryStepIsIntoANewLayer} for tiles further out.
     *
     * @param startingLayer               The layer number for the subsequent call to {@code addCoordinatesForRowWhereEveryStepIsIntoANewLayer}.
     * @param startingTileIndex           The index in {@code tileList} of the anchor tile for this sequence.
     * @param startingOffsetForSubRoutine The offset for the subsequent call to {@code addCoordinatesForRowWhereEveryStepIsIntoANewLayer}.
     * @param counter                     The number of immediately adjacent tiles to calculate before calling the subroutine.
     * @param xIsGettingLarger            Boolean indicating if the x-coordinate should increase (true) or decrease (false).
     */
    private void addNeighboringTile(int startingLayer, int startingTileIndex, int startingOffsetForSubRoutine, int counter, boolean xIsGettingLarger) {
        Tile lastTile = tileList.get(startingTileIndex);
        int lastTileIndex = startingTileIndex;
        int currentTileIndex;
        Tile currentTile;
        double[] coordinates;
        for (; counter > 0; counter--) {
            currentTileIndex = lastTileIndex - 1; // Assumes tiles are ordered such that -1 moves in the desired direction
            currentTile = tileList.get(currentTileIndex);

            coordinates = lastTile.getCoordinates();
            if (xIsGettingLarger) {
                currentTile.setCoordinates(coordinates[0] + distanceBetweenTiles, coordinates[1]);
            } else {
                currentTile.setCoordinates(coordinates[0] - distanceBetweenTiles, coordinates[1]);
            }

            lastTileIndex = currentTileIndex;
            lastTile = currentTile;
        }

        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(startingLayer, lastTileIndex, startingOffsetForSubRoutine, xIsGettingLarger);
    }

    /**
     * Helper method to set coordinates for an "irregular" starting tile in a row (often a corner or edge case)
     * and then proceed to calculate coordinates for the rest of the row using {@link #addNeighboringTile}.
     * This is used for specific parts of the hexagonal grid generation, particularly for rows that might
     * not start from a main diagonal.
     *
     * @param startingLayerForSubRoutine  The layer for the subsequent call to {@code addNeighboringTile}.
     * @param startingTileIndex           The index of a reference tile (often already positioned).
     * @param irregularTileIndex          The index of the tile whose position is being set relative to {@code startingTileIndex}.
     * @param startingOffsetForSubRoutine The offset for the call to {@code addNeighboringTile}.
     * @param counterForSubRoutine        The counter for the call to {@code addNeighboringTile}.
     * @param xIsGettingLarger            Boolean indicating if the x-coordinate should increase (true) or decrease (false).
     */
    private void changeStartingPositionForSouthWestHalfAndMiddleRow(int startingLayerForSubRoutine, int startingTileIndex, int irregularTileIndex, int startingOffsetForSubRoutine, int counterForSubRoutine, boolean xIsGettingLarger) {
        Tile lastTile = tileList.get(startingTileIndex);
        Tile currentTile = tileList.get(irregularTileIndex);

        double[] coordinates = lastTile.getCoordinates();
        if (xIsGettingLarger) {
            currentTile.setCoordinates(coordinates[0] + distanceBetweenTiles, coordinates[1]);
        } else {
            currentTile.setCoordinates(coordinates[0] - distanceBetweenTiles, coordinates[1]);
        }

        addNeighboringTile(startingLayerForSubRoutine, irregularTileIndex, startingOffsetForSubRoutine, counterForSubRoutine, xIsGettingLarger);
    }

    /**
     * Adds coordinates to two key tiles that form a "main diagonal" in a layer of the hexagonal grid.
     * These tiles are the first tile of a new layer (e.g., southeast direction) and a tile
     * towards the middle/northwest of that layer. Their positions are calculated relative to
     * corresponding tiles in the previous layer.
     *
     * @param indexOfFirstTileOfThisLayer      Index of the first tile in the current layer (e.g., SE direction).
     * @param indexOfFirstTileOfPreviousLayer  Index of the corresponding tile in the previous layer (SE direction).
     * @param indexOfMiddleTileOfThisLayer     Index of a middle/NW tile in the current layer.
     * @param indexOfMiddleTileOfPreviousLayer Index of the corresponding middle/NW tile in the previous layer.
     */
    private void addCoordinatesToMainDiagonal(int indexOfFirstTileOfThisLayer,
                                              int indexOfFirstTileOfPreviousLayer,
                                              int indexOfMiddleTileOfThisLayer,
                                              int indexOfMiddleTileOfPreviousLayer) {
        double x;
        double y;
        double[] previousCoordinates;

        // -------------- set south-east tile --------------
        // get tile indices


        // get tiles
        Tile previousTile = tileList.get(indexOfFirstTileOfPreviousLayer);
        Tile currentTile = tileList.get(indexOfFirstTileOfThisLayer);

        // edit coordinates
        previousCoordinates = previousTile.getCoordinates();
        x = previousCoordinates[0] + southEastAddition[0];
        y = previousCoordinates[1] + southEastAddition[1];
        currentTile.setCoordinates(x, y);

        // -------------- set north-west tile --------------
        // get tile indices

        // get tiles
        currentTile = tileList.get(indexOfMiddleTileOfThisLayer);
        previousTile = tileList.get(indexOfMiddleTileOfPreviousLayer);

        // edit coordinates
        previousCoordinates = previousTile.getCoordinates();
        x = previousCoordinates[0] + northWestAddition[0];
        y = previousCoordinates[1] + northWestAddition[1];
        currentTile.setCoordinates(x, y);
    }
}