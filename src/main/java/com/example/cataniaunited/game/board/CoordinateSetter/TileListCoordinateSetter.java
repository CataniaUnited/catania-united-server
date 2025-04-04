package com.example.cataniaunited.game.board.CoordinateSetter;

import com.example.cataniaunited.game.board.Tile;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public class TileListCoordinateSetter {

    private int sizeOfBoard;
    private List<Tile> tileList;
    private final int sizeOfHex;

    // precomputed values of the angle to get to the midpoint of the starting tile of the next layer k -> (k*StrictMath.PI)/3;
    // to prevent rounding errors used Bigdecimal to compute the values and then transformed them to with k.doubleValue()
    double northWestAngle = 2.0943951023931957; // k = 2
    double southEastAngle = 5.235987755982989; // k = 5
    double distanceBetweenTiles;

    boolean flipYAxis;

    double[] northWestAddition;
    double[] southEastAddition;
    public TileListCoordinateSetter(List<Tile> tileList, int sizeOfBoard, int sizeOfHex, boolean flipYAxis){
        this.tileList = tileList;
        this.sizeOfBoard = sizeOfBoard;
        this.sizeOfHex =sizeOfHex;
        this.distanceBetweenTiles = StrictMath.sqrt(3)*sizeOfHex;
        this.flipYAxis = flipYAxis;

        // precomputing offset since working with bigDecimalObjects takes time
        northWestAddition = polarToCartesian(distanceBetweenTiles, northWestAngle);
        southEastAddition = polarToCartesian(distanceBetweenTiles, southEastAngle);
    }

    private static int calculateAmountOfTilesForLayerK(int k) {
        return (k > 0) ? (int) (3*Math.pow((k-1), 2) + 3 * (k-1) + 1) : 0;
    }

    public List<Tile> addCoordinatesToTileList() {

        // set coordinates of center Tile
        tileList.get(0).setCoordinates(0, 0);

        for(int layer = 1; layer < sizeOfBoard; layer++){
            addCoordinatesToMainDiagonal(tileList, layer, northWestAddition, southEastAddition);
        }


        return tileList;
    }

    public void addCoordinatesToMainDiagonal(List<Tile> tileList, int layer, double[] northWestAddition, double[] southEastAddition){
        Function<Integer, Integer> getIndexOfMiddleElementOfLayerK = (k) -> (calculateAmountOfTilesForLayerK(k+1) - calculateAmountOfTilesForLayerK(k))/2 + calculateAmountOfTilesForLayerK(k);
        int indexOfFirstTileOfThisLayer, indexOfFirstTileOfPreviousLayer, indexOfMiddleTileOfThisLayer, indexOfMiddleTileOfPreviousLayer;
        double x, y;
        double[] previousCoordinates;


        // -------------- set south-east tile --------------
        // get tile indices
        indexOfFirstTileOfThisLayer = calculateAmountOfTilesForLayerK(layer); // index of current Tile regarding tileList
        indexOfFirstTileOfPreviousLayer = calculateAmountOfTilesForLayerK(layer-1); // amount of tiles placed before-1 to get index

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
        indexOfMiddleTileOfThisLayer = getIndexOfMiddleElementOfLayerK.apply(layer);
        indexOfMiddleTileOfPreviousLayer = getIndexOfMiddleElementOfLayerK.apply(layer-1);

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