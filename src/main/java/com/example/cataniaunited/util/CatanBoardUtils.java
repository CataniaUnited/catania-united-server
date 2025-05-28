package com.example.cataniaunited.util;

import java.math.BigDecimal;

public class CatanBoardUtils {

    private CatanBoardUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Converts polar coordinates (radius, angle) to Cartesian coordinates (x, y).
     * Uses {@link BigDecimal} for intermediate multiplication to potentially improve precision
     * before converting back to double.
     *
     * @param r         The radius.
     * @param theta     The angle in radians.
     * @param flipYAxis If true, the calculated y-coordinate is negated (common for screen coordinates).
     * @return A double array `[x, y]` representing the Cartesian coordinates.
     */
    public static double[] polarToCartesian(double r, double theta, boolean flipYAxis){
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


    /**
     * Calculates the total number of tiles on a Catan board with a given number of layers/rings.
     * The formula is 3*n*(n+1) + 1, where n = layers - 1.
     *
     * @param layers The number of layers/rings (sizeOfBoard). A board with 1 layer has 1 tile (center).
     *               A board with 2 layers has 1 (center) + 6 (first ring) = 7 tiles.
     *               A board with 3 layers has 1 + 6 + 12 = 19 tiles.
     * @return The total number of tiles. Returns 0 if layers is non-positive.
     */
    public static int calculateAmountOfTilesForLayerK(int layers) {
        if (layers <= 0) return 0;
        int n = layers - 1; // n represents the number of rings around the central tile
        return 3 * n * (n + 1) + 1;
    }
}
