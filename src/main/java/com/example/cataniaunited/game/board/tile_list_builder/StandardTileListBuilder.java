package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.example.cataniaunited.util.CatanBoardUtils.calculateAmountOfTilesForLayerK;
import static com.example.cataniaunited.util.CatanBoardUtils.polarToCartesian;

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
     * One tile is designated as DESERT, and the rest are populated with
     * resource-producing tile types in a repeating sequence.
     *
     * @throws IllegalStateException if configuration has not been set, or if no
     *                               usable (non-DESERT) tile types are available.
     */
    @Override
    public void buildTiles() {
        if (this.tileList == null || amountOfTilesOnBoard <= 0) {
            throw new IllegalStateException("Configuration must be set before building tiles.");
        }

        // Filter out the DESERT type to get a list of resource-producing types
        List<TileType> resourceProducingTypes = Arrays.stream(TileType.values())
                .filter(type -> type != TileType.DESERT)
                .toList();

        // Add one DESERT tile
        tileList.add(new Tile(TileType.DESERT));

        // Add the remaining tiles as resource-producing tiles
        // The loop runs for one times less than the total number of tiles because one tile is already added as DESERT.
        for (int i = 0; i < amountOfTilesOnBoard - 1; i++) {
            // Cycle through the resourceProducingTypes
            TileType currentTileType = resourceProducingTypes.get(i % resourceProducingTypes.size());
            tileList.add(new Tile(currentTileType));
        }
    }

    /**
     * Assigns production numbers (dice roll values) to the non-DESERT tiles.
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
        // Excluding the TileType.DESERT tile since it doesn't produce any resources.
        int amountOfValuesToCreate = 0;
        for (Tile tile : tileList) {
            if (tile.getType() != TileType.DESERT) {
                amountOfValuesToCreate++;
            }
        }

        List<Integer> productionValues = generateShuffledProductionValues(amountOfValuesToCreate);
        Collections.shuffle(productionValues); // Shuffle the list again, since the overhead got added at the end

        int valueIndex = 0;
        for (Tile tile : tileList) {
            if (tile.getType() != TileType.DESERT) { // Do not add a value for DESERT tiles since they dont produce resources
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
        if (Util.isEmpty(tileList)) {
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
        if (Util.isEmpty(tileList)) {
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
        if (Util.isEmpty(tileList)) {
            throw new IllegalStateException("Tiles must be built before calculating coordinates.");
        }

        if (tileList.get(0).id == 0) {
            throw new IllegalStateException("ID's must be built before calculating coordinates.");
        }

        // Operator to find the index of the "middle" tile of a given layer k.
        // The "middle" here refers to a tile along one of the hexagonal grid's axes.
        IntUnaryOperator getIndexOfMiddleTileOfLayerK = k ->
                (calculateAmountOfTilesForLayerK(k + 1) - calculateAmountOfTilesForLayerK(k)) / 2
                        + calculateAmountOfTilesForLayerK(k);

        // Set coordinates of the center tile (index 0)
        tileList.get(0).setCoordinates(0, 0);

        // Initialize coordinates for the first few tiles in the "middle rows" of the first ring (layer 2 conceptually)
        // These serve as anchors for subsequent calculations.
        // Arguments: (startingLayerForSubRoutine, startingTileIndex (anchor), irregularTileIndex (tile to position),
        //             startingOffsetForSubRoutine, counterForSubRoutine, xIsGettingLarger)
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 2, 7, -1, true);
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 5, 10, -1, false);

        for (int currentLayerNumber = 1; currentLayerNumber < sizeOfBoard; currentLayerNumber++) {
            // Calculate all relevant indices for the current and previous layer
            TileIndicesOfMainDiagonalForSpecificLayer layerIndices = new TileIndicesOfMainDiagonalForSpecificLayer(
                    calculateAmountOfTilesForLayerK(currentLayerNumber),              // indexOfFirstTileOfThisLayer
                    calculateAmountOfTilesForLayerK(currentLayerNumber - 1),        // indexOfFirstTileOfPreviousLayer
                    getIndexOfMiddleTileOfLayerK.applyAsInt(currentLayerNumber),      // indexOfMiddleTileOfThisLayer
                    getIndexOfMiddleTileOfLayerK.applyAsInt(currentLayerNumber - 1) // indexOfMiddleTileOfPreviousLayer
            );

            // Calculate coordinates for key tiles on the main "diagonals" of the current layer
            addCoordinatesToMainDiagonal(layerIndices);

            // Calculate coordinates for the remaining tiles in the rows of the current layer,
            // based on the newly positioned diagonal tiles.
            addCoordinatesToRows(currentLayerNumber, layerIndices.firstTileOfCurrentLayer(), layerIndices.middleTileOfCurrentLayer());
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
        if (Util.isEmpty(tileList)) {
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
     * Adds coordinates to two key tiles that form a "main diagonal" in a layer of the hexagonal grid.
     * These tiles are the first tile of a new layer (e.g., southeast direction) and a tile
     * towards the middle/northwest of that layer. Their positions are calculated relative to
     * corresponding tiles in the previous layer.
     *
     * @param indices A {@link TileIndicesOfMainDiagonalForSpecificLayer} object containing the necessary tile indices.
     */
    private void addCoordinatesToMainDiagonal(TileIndicesOfMainDiagonalForSpecificLayer indices) {
        double x;
        double y;
        double[] previousCoordinates;

        // -------------- set south-east (SE) anchor tile for the current layer --------------
        Tile previousLayerSETile = tileList.get(indices.firstTileOfPreviousLayer());
        Tile currentLayerSETile = tileList.get(indices.firstTileOfCurrentLayer());

        previousCoordinates = previousLayerSETile.getCoordinates();
        x = previousCoordinates[0] + southEastAddition[0]; // southEastAddition is a precomputed [dx, dy]
        y = previousCoordinates[1] + southEastAddition[1];
        currentLayerSETile.setCoordinates(x, y);

        // -------------- set north-west (NW) anchor tile for the current layer --------------
        Tile previousLayerNWTile = tileList.get(indices.middleTileOfPreviousLayer());
        Tile currentLayerNWTile = tileList.get(indices.middleTileOfCurrentLayer());

        previousCoordinates = previousLayerNWTile.getCoordinates();
        x = previousCoordinates[0] + northWestAddition[0]; // northWestAddition is a precomputed [dx, dy]
        y = previousCoordinates[1] + northWestAddition[1];
        currentLayerNWTile.setCoordinates(x, y);
    }

    /**
     * Adds coordinates to tiles forming rows radiating from diagonal anchor points
     * for a specific layer of the board.
     *
     * @param currentLayerNumber The current layer number (0-based from the loop in calculateTilePositions).
     * @param southRowStartingTileIndex Index of the tile starting the "south" radiating row (SE anchor of current layer).
     * @param northRowStartingTileIndex Index of the tile starting the "north" radiating row (NW anchor of current layer).
     */
    private void addCoordinatesToRows(int currentLayerNumber, int southRowStartingTileIndex, int northRowStartingTileIndex) {
        // The 'layer' variable here is 1-based for offset calculations,
        // currentLayerNumber from the calling loop is 0-based for the first ring, 1-based for the second, etc.
        // So, if currentLayerNumber = 1 (second ring), layer becomes 2.
        int layerForOffsetCalc = currentLayerNumber + 1;
        int offset;

        // Calculate for the "East Part Of South Row"
        // This row extends eastward from the south-east anchor tile of the current layer.
        offset = calculateAmountOfTilesForLayerK(layerForOffsetCalc) - calculateAmountOfTilesForLayerK(layerForOffsetCalc - 1) + 1;
        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(layerForOffsetCalc, southRowStartingTileIndex, offset, true);

        // Calculate for the "West Part Of North Row"
        // This row extends westward from the north-west anchor tile of the current layer.
        offset = calculateAmountOfTilesForLayerK(layerForOffsetCalc) - calculateAmountOfTilesForLayerK(layerForOffsetCalc - 1) + 4;
        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(layerForOffsetCalc, northRowStartingTileIndex, offset, false);

        // Calculate for the "East Part Of North Row"
        // This involves placing tiles adjacent (to the east) to the north-west anchor.
        offset = calculateAmountOfTilesForLayerK(layerForOffsetCalc) - calculateAmountOfTilesForLayerK(layerForOffsetCalc - 1) + 1;
        // currentLayerNumber represents how many tiles are in this immediate segment.
        addNeighboringTile(layerForOffsetCalc, northRowStartingTileIndex, offset, currentLayerNumber, true);


        // Calculate for the "West Part Of South Row"
        // This involves placing tiles adjacent (to the west) to the south-east anchor.
        offset = calculateAmountOfTilesForLayerK(layerForOffsetCalc) - calculateAmountOfTilesForLayerK(layerForOffsetCalc - 1) + 4;
        int lastTileOfCurrentLayerIndex = calculateAmountOfTilesForLayerK(layerForOffsetCalc) - 1;
        // (currentLayerNumber - 1) represents how many tiles are in this immediate segment.
        changeStartingPositionForSouthWestHalfAndMiddleRow(layerForOffsetCalc, southRowStartingTileIndex, lastTileOfCurrentLayerIndex, offset, currentLayerNumber -1, false);
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
}