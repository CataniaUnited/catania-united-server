package com.example.cataniaunited.game.board;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class TileListGenerator {

    private final int sizeOfBoard;
    private final List<Tile> tileList;
    private final int sizeOfHex;
    private int amountOfTilesOnBoard;

    // precomputed values of the angle to get to the midpoint of the starting tile of the next layer k -> (k*StrictMath.PI)/3;
    // to prevent rounding errors used Bigdecimal to compute the values and then transformed them to with k.doubleValue()
    double northWestAngle = 2.0943951023931957; // k = 2
    double southEastAngle = 5.235987755982989; // k = 5
    double distanceBetweenTiles;

    boolean flipYAxis;

    double[] northWestAddition;
    double[] southEastAddition;
    public TileListGenerator(int sizeOfBoard, int sizeOfHex, boolean flipYAxis){
        amountOfTilesOnBoard = calculateAmountOfTilesForLayerK(sizeOfBoard);
        tileList = new ArrayList<>(amountOfTilesOnBoard);
        this.sizeOfBoard = sizeOfBoard;
        this.sizeOfHex =sizeOfHex;
        this.distanceBetweenTiles = StrictMath.sqrt(3)*sizeOfHex;
        this.flipYAxis = flipYAxis;

        // precomputing offset since working with bigDecimalObjects takes time
        northWestAddition = polarToCartesian(distanceBetweenTiles, northWestAngle);
        southEastAddition = polarToCartesian(distanceBetweenTiles, southEastAngle);
    }

    public List<Tile> generateShuffledTileList(){

        TileType[] tileTypes = TileType.values();

        tileList.add(new Tile(TileType.WASTE));

        for(int i = 1; i < amountOfTilesOnBoard; i++){
            TileType currentTileType = tileTypes[i % (tileTypes.length -1)];
            tileList.add(new Tile(currentTileType));
        }

        Collections.shuffle(tileList);
        for(int i = 0; i < tileList.size(); i++){
            tileList.get(i).setId(i+1);
        }

        addCoordinatesToTileList();
        return tileList;
    }


    public static int calculateAmountOfTilesForLayerK(int k) {
        return (k > 0) ? (int) (3*Math.pow((k-1), 2) + 3 * (k-1) + 1) : 0;
    }

    public void addCoordinatesToTileList() {
        Function<Integer, Integer> getIndexOfMiddleElementOfLayerK = (k) -> (calculateAmountOfTilesForLayerK(k+1) - calculateAmountOfTilesForLayerK(k))/2 + calculateAmountOfTilesForLayerK(k);
        // set coordinates of center Tile
        tileList.get(0).setCoordinates(0, 0);
        // Set Middle row coordinates
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 2, 7, -1, true);
        changeStartingPositionForSouthWestHalfAndMiddleRow(2, 0, 5, 10, -1, false);

        // set coordinates for tiles in every other row (regular)
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

    void addCoordinatesToRows(int layerIndex, int southRowStartingTileIndex, int northRowStartingTileIndex){
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

    void addCoordinatesForRowWhereEveryStepIsIntoANewLayer(int startingLayer, int startingTileIndex, int startingOffset, boolean xIsGettingLarger){
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

    public void addCoordinatesToMainDiagonal(int indexOfFirstTileOfThisLayer,
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


    double[] polarToCartesian(double r, double theta){
        double[] coordinates = new double[2];
        double trigResult;
        BigDecimal multiplicationResult;

        trigResult = StrictMath.cos(theta);
        multiplicationResult = new BigDecimal(r).multiply(new BigDecimal(trigResult));
        coordinates[0] = multiplicationResult.doubleValue();

        trigResult = StrictMath.sin(theta);
        multiplicationResult = new BigDecimal(r).multiply(new BigDecimal(trigResult));
        coordinates[1] = multiplicationResult.doubleValue();
        coordinates[1] = (flipYAxis) ? -coordinates[1] : coordinates[1];

        return coordinates;
    }

}

    /*
        //Random Calculations might not be needed
        // double apothem = StrictMath.sqrt(3.0/2.0) * sizeOfHex;
        double distanceBetweenTheMidpointOfTwoHexes = StrictMath.sqrt(3)*sizeOfHex;
        //double anglesToGetToAnotherMidpoint = (k -> ((double)k/3 + (double)1/6)*StrictMath.PI);
        //double anglesToGetToSettlementPosition = k -> (k*StrictMath.PI)/3;

        //int y = r * StrictMath.sin(theta);
        //int x = r * StrictMath.cos(theta);
        //int r = StrictMath.sqrt(StrictMath.pow(x, 2) + StrictMath.pow(y,2));
        //theta = StrictMath.atan(y / x);
    */