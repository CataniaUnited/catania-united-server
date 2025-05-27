package com.example.cataniaunited.util;

import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import org.junit.jupiter.api.Test;

import static com.example.cataniaunited.util.CatanBoardUtils.calculateAmountOfTilesForLayerK;
import static com.example.cataniaunited.util.CatanBoardUtils.polarToCartesian;
import static org.junit.jupiter.api.Assertions.*;

public class CatanBoardUtilTest {


    @Test
    void polarToCartesianConvertsCorrectly() {
        double r = 10.0;
        assertArrayEquals(new double[]{10.0, 0.0}, polarToCartesian(r, 0, false), 0.001);
        assertArrayEquals(new double[]{10.0, -0.0}, polarToCartesian(r, 0, true), 0.001);
        assertArrayEquals(new double[]{0.0, 10.0}, polarToCartesian(r, Math.PI / 2, false), 0.001);
        assertArrayEquals(new double[]{0.0, -10.0}, polarToCartesian(r, Math.PI / 2, true), 0.001);
        assertArrayEquals(new double[]{-10.0, 0.0}, polarToCartesian(r, Math.PI, false), 0.001);
        assertArrayEquals(new double[]{-10.0, -0.0}, polarToCartesian(r, Math.PI, true), 0.001);
        assertArrayEquals(new double[]{0.0, -10.0}, polarToCartesian(r, 3 * Math.PI / 2, false), 0.001);
        assertArrayEquals(new double[]{0.0, 10.0}, polarToCartesian(r, 3 * Math.PI / 2, true), 0.001);
    }

    @Test
    void calculateAmountOfTilesForLayerKReturnsCorrectValues() {
        assertEquals(0, calculateAmountOfTilesForLayerK(0));
        assertEquals(1, calculateAmountOfTilesForLayerK(1));
        assertEquals(7, calculateAmountOfTilesForLayerK(2));
        assertEquals(19, calculateAmountOfTilesForLayerK(3));
        assertEquals(37, calculateAmountOfTilesForLayerK(4));
    }
}