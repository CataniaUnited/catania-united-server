package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PortTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    void constructorWithZeroOrNegativeInputAmountShouldThrowIllegalArgumentException(int invalidAmount) {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> new TestablePort(invalidAmount));
        assertEquals("Input resource amount must be positive.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void constructorWithPositiveInputAmountShouldNotThrowException(int validAmount) {
        assertDoesNotThrow(() -> {
            new TestablePort(validAmount);
        });
    }

    @Test
    void isNotTradingForOfferedResourcesWithNullOfferedShouldReturnFalse() {
        Port port = new TestablePort(3);
        assertFalse(port.isNotTradingForOfferedResources(null, Collections.singletonList(TileType.CLAY)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithNullDesiredShouldReturnFalse() {
        Port port = new TestablePort(3);
        assertFalse(port.isNotTradingForOfferedResources(Collections.singletonList(TileType.WOOD), null));
    }
    @Test
    void isNotTradingForOfferedResourcesWithEmptyOfferedShouldReturnFalse() {
        Port port = new TestablePort(3);
        assertFalse(port.isNotTradingForOfferedResources(Collections.emptyList(), Collections.singletonList(TileType.CLAY)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithEmptyDesiredShouldReturnFalse() {
        Port port = new TestablePort(3);
        assertFalse(port.isNotTradingForOfferedResources(Collections.singletonList(TileType.WOOD), Collections.emptyList()));
    }

    // We need an actual implementation to test the abstract class
    static class TestablePort extends Port {
        protected TestablePort(int inputResourceAmount) {
            super(inputResourceAmount);
        }

        @Override
        public boolean canTrade(List<TileType> offeredResources, List<TileType> desiredResources) {
            return false;
        }
    }
}