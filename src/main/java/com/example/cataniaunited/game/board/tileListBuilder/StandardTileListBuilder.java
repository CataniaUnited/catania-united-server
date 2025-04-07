package com.example.cataniaunited.game.board.tileListBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class StandardTileListBuilder implements TileListBuilder{

    int sizeOfBoard;
    int sizeOfHex;
    boolean flipYAxis;

    int amountOfTilesOnBoard;
    double distanceBetweenTiles;
    double[] northWestAddition;
    double[] southEastAddition;

    // precomputed values of the angle to get to the midpoint of the starting tile of the next layer
    // calculated by the formula k * PI / 3;
    // to prevent rounding errors used Bigdecimal to compute the values and then transformed them to with k.doubleValue()
    private static final double NORTH_WEST_ANGLE = 2.0943951023931957; // k = 2
    private static final double SOUTH_EAST_ANGLE = 5.235987755982989; // k = 5

    List<Tile> tileList;

    public StandardTileListBuilder(){
        this.reset(); // Initialize and Reset
    }

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

    @Override
    public void setConfiguration(int sizeOfBoard, int sizeOfHex, boolean flipYAxis) {
        if (sizeOfBoard <= 0 || sizeOfHex <= 0) {
            throw new IllegalArgumentException("Board size and hex size must be positive.");
        }
        this.sizeOfBoard = sizeOfBoard;
        this.sizeOfHex = sizeOfHex;
        this.flipYAxis = flipYAxis;

        this.distanceBetweenTiles = StrictMath.sqrt(3)*this.sizeOfHex;
        amountOfTilesOnBoard = calculateAmountOfTilesForLayerK(sizeOfBoard);

        // precomputing offset since working with bigDecimalObjects takes time
        northWestAddition = polarToCartesian(distanceBetweenTiles, NORTH_WEST_ANGLE, this.flipYAxis);
        southEastAddition = polarToCartesian(distanceBetweenTiles, SOUTH_EAST_ANGLE, this.flipYAxis);

        tileList = new ArrayList<>(amountOfTilesOnBoard);
    }

    @Override
    public void buildTiles() {
        if (this.tileList == null || amountOfTilesOnBoard <= 0) {
            throw new IllegalStateException("Configuration must be set before building tiles.");
        }

        TileType[] availableTypes = TileType.values();
        int usableTypeCount = availableTypes.length - 1; // Assuming one WASTE type (last index)
        if (usableTypeCount <= 0) throw new IllegalStateException("Requires non-WASTE types.");
        if (availableTypes[usableTypeCount] != TileType.WASTE) throw new IllegalStateException("WASTE-Type needs to be at last position.");


        tileList.add(new Tile(TileType.WASTE));
        for(int i = 0; i < amountOfTilesOnBoard-1; i++){
            TileType currentTileType = availableTypes[i % (usableTypeCount)];
            tileList.add(new Tile(currentTileType));
        }
    }

    @Override
    public void shuffleTiles() {
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before shuffling.");
        }


        Collections.shuffle(this.tileList);
    }

    @Override
    public void assignTileIds() {
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before assigning IDs.");
        }

        for (int i = 0; i < this.tileList.size(); i++) {
            this.tileList.get(i).setId(i + 1);
        }
    }

    @Override
    public void calculateTilePositions() {
        if (this.northWestAddition == null || this.southEastAddition == null) {
            throw new IllegalStateException("Configuration must be set before building tiles.");
        }
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before calculating coordinates.");
        }

        Function<Integer, Integer> getIndexOfMiddleElementOfLayerK = (k) -> (calculateAmountOfTilesForLayerK(k+1) - calculateAmountOfTilesForLayerK(k))/2 + calculateAmountOfTilesForLayerK(k);
        // set coordinates of center Tile
        tileList.get(0).setCoordinates(0, 0);

        if (this.tileList.size() == 1){ // If the size is one, there is only one Tile
            return;
        }

        // Set Middle row coordinates
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 2, 7, -1, true);
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 5, 10, -1, false);

        // set coordinates for tiles in every row
        int indexOfFirstTileOfThisLayer, indexOfFirstTileOfPreviousLayer, indexOfMiddleTileOfThisLayer, indexOfMiddleTileOfPreviousLayer;
        for(int layerIndex = 1; layerIndex < sizeOfBoard; layerIndex++){
            // get indices of current Layer
            indexOfFirstTileOfThisLayer = calculateAmountOfTilesForLayerK(layerIndex); // index of current Tile regarding tileList
            indexOfFirstTileOfPreviousLayer = calculateAmountOfTilesForLayerK(layerIndex-1); // amount of tiles placed before-1 to get index
            indexOfMiddleTileOfThisLayer = getIndexOfMiddleElementOfLayerK.apply(layerIndex);
            indexOfMiddleTileOfPreviousLayer = getIndexOfMiddleElementOfLayerK.apply(layerIndex-1);

            // calculate next tiles on diagonal
            addCoordinatesToMainDiagonal(indexOfFirstTileOfThisLayer,
                    indexOfFirstTileOfPreviousLayer,
                    indexOfMiddleTileOfThisLayer,
                    indexOfMiddleTileOfPreviousLayer);

            // calculate rows depending on newly discovered diagonal tiles
            addCoordinatesToRows(layerIndex, indexOfFirstTileOfThisLayer, indexOfMiddleTileOfThisLayer);
        }

    }

    @Override
    public List<Tile> getTileList() {
        if (this.tileList == null || this.tileList.isEmpty()) {
            throw new IllegalStateException("Tiles must be built before returning coordinates.");
        }

        return this.tileList;
    }

    // --- coordinate calculation ---
    private void addCoordinatesToRows(int layerIndex, int southRowStartingTileIndex, int northRowStartingTileIndex){
        int layer = layerIndex+1;
        int offset;

        // Add East Part Of South Row
        offset =  calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer-1)+ 1;
        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(layer, southRowStartingTileIndex, offset, true);

        // Add West Part Of North Row
        offset =  calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer-1)+ 4;
        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(layer, northRowStartingTileIndex, offset, false);

        // Add East part Of North Row
        offset =  calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer-1)+ 1;
        addNeighboringTile(layer, northRowStartingTileIndex, offset, layer-1, true);

        // Add West Part South Row
        offset =  calculateAmountOfTilesForLayerK(layer) - calculateAmountOfTilesForLayerK(layer-1)+ 4;
        int irregularIndex = calculateAmountOfTilesForLayerK(layer)-1; // Last tile of current layer
        changeStartingPositionForSouthWestHalfAndMiddleRow(layer, southRowStartingTileIndex, irregularIndex, offset, layer-2, false);

    }

    private void addCoordinatesForRowWhereEveryStepIsIntoANewLayer(int startingLayer, int startingTileIndex, int startingOffset, boolean xIsGettingLarger){
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
            if (xIsGettingLarger){
                currentTile.setCoordinates(coordinates[0] + distanceBetweenTiles, coordinates[1]);
            } else {
                currentTile.setCoordinates(coordinates[0] - distanceBetweenTiles, coordinates[1]);
            }

            lastTileIndex = currentTileIndex;
            lastTile = currentTile;
            offset += 6;
        }
    }

    private void addNeighboringTile(int startingLayer, int startingTileIndex, int startingOffsetForSubRoutine, int counter, boolean xIsGettingLarger){
        Tile lastTile = tileList.get(startingTileIndex);
        int lastTileIndex = startingTileIndex;
        int currentTileIndex;
        Tile currentTile;
        double[] coordinates;
        for(; counter > 0; counter--){
            currentTileIndex = lastTileIndex - 1;
            currentTile = tileList.get(currentTileIndex);

            coordinates = lastTile.getCoordinates();
            if (xIsGettingLarger){
                currentTile.setCoordinates(coordinates[0] + distanceBetweenTiles, coordinates[1]);
            } else {
                currentTile.setCoordinates(coordinates[0] - distanceBetweenTiles, coordinates[1]);
            }

            lastTileIndex = currentTileIndex;
            lastTile = currentTile;
        }

        addCoordinatesForRowWhereEveryStepIsIntoANewLayer(startingLayer, lastTileIndex, startingOffsetForSubRoutine, xIsGettingLarger);
    }
    private void changeStartingPositionForSouthWestHalfAndMiddleRow(int startingLayerForSubRoutine, int startingTileIndex, int irregularTileIndex, int startingOffsetForSubRoutine, int counterForSubRoutine, boolean xIsGettingLarger){
        Tile lastTile = tileList.get(startingTileIndex);
        Tile currentTile = tileList.get(irregularTileIndex);

        double[] coordinates = lastTile.getCoordinates();
        if (xIsGettingLarger){
            currentTile.setCoordinates(coordinates[0] + distanceBetweenTiles, coordinates[1]);
        } else {
            currentTile.setCoordinates(coordinates[0] - distanceBetweenTiles, coordinates[1]);
        }

        addNeighboringTile(startingLayerForSubRoutine, irregularTileIndex, startingOffsetForSubRoutine, counterForSubRoutine, xIsGettingLarger);
    }

    private void addCoordinatesToMainDiagonal(int indexOfFirstTileOfThisLayer,
                                             int indexOfFirstTileOfPreviousLayer,
                                             int indexOfMiddleTileOfThisLayer,
                                             int indexOfMiddleTileOfPreviousLayer){
        double x, y;
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




    // --- Static Helper Methods  ---
    public static int calculateAmountOfTilesForLayerK(int layers) {
        if (layers <= 0) return 0;
        int n = layers - 1;
        return 3 * n * (n + 1) + 1;
    }

    static double[] polarToCartesian(double r, double theta, boolean flipYAxis){
        double[] coordinates = new double[2];
        double trigResult;
        BigDecimal multiplicationResult;

        trigResult = StrictMath.cos(theta);
        multiplicationResult = BigDecimal.valueOf(r).multiply(BigDecimal.valueOf(trigResult));
        coordinates[0] = multiplicationResult.doubleValue();

        trigResult = StrictMath.sin(theta);
        multiplicationResult = BigDecimal.valueOf(r).multiply(BigDecimal.valueOf(trigResult));
        coordinates[1] = multiplicationResult.doubleValue();
        coordinates[1] = (flipYAxis) ? -coordinates[1] : coordinates[1];

        return coordinates;
    }

}
