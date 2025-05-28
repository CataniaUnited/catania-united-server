package com.example.cataniaunited.util;

import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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

    @Test
    void testPrivateConstructorThrowsException() {
        Exception exception = assertThrows(InvocationTargetException.class, () -> {
            Constructor<CatanBoardUtils> constructor = CatanBoardUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });

        assertNotNull(exception.getCause(), "InvocationTargetException should have a cause.");
        assertEquals(IllegalStateException.class, exception.getCause().getClass(), "The cause should be an IllegalStateException.");
        assertEquals("Utility class", exception.getCause().getMessage(), "The exception message should match.");
    }
}