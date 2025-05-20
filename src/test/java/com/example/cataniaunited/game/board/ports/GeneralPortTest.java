package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GeneralPortTest {

    private GeneralPort generalPort;

    @BeforeEach
    void setUp() {
        generalPort = new GeneralPort(); // 3:1 port
    }

    @Test
    void constructorShouldSetCorrectRatio() {
        assertEquals(3, generalPort.inputResourceAmount);
    }

    @Test
    void canTradeValidSingleTypeTradeShouldReturnTrue() {
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertTrue(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeValidMultipleTypesTradeShouldReturnTrue() {
        List<TileType> offered = Arrays.asList(
                TileType.WOOD, TileType.WOOD, TileType.WOOD,
                TileType.CLAY, TileType.CLAY, TileType.CLAY
        );
        List<TileType> desired = Arrays.asList(TileType.SHEEP, TileType.ORE);
        assertTrue(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeValidSixForTwoShouldReturnTrue() {
        List<TileType> offered = Arrays.asList(
                TileType.WHEAT, TileType.WHEAT, TileType.WHEAT,
                TileType.WHEAT, TileType.WHEAT, TileType.WHEAT
        );
        List<TileType> desired = Arrays.asList(TileType.ORE, TileType.SHEEP);
        assertTrue(generalPort.canTrade(offered, desired));
    }


    @Test
    void canTradeEmptyOfferedListShouldReturnFalse() {
        List<TileType> offered = Collections.emptyList();
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeEmptyOfferedNullShouldReturnFalse() {
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(generalPort.canTrade(null, desired));
    }

    @Test
    void canTradeEmptyDesiredListShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> desired = Collections.emptyList();
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeNullDesiredListShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.WOOD);
        assertFalse(generalPort.canTrade(offered, null));
    }

    @Test
    void canTradeOfferedNotMultipleOfRatioShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD);
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeDesiredAmountMismatchShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> desired = Arrays.asList(TileType.SHEEP, TileType.ORE);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeOfferedSixButWantsOneShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(
                TileType.WHEAT, TileType.WHEAT, TileType.WHEAT,
                TileType.WHEAT, TileType.WHEAT, TileType.WHEAT
        );
        List<TileType> desired = Collections.singletonList(TileType.ORE);
        assertFalse(generalPort.canTrade(offered, desired));
    }


    @Test
    void canTradeInvalidBundleSingleTypeShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(generalPort.canTrade(offered, desired), "Should fail on overall ratio first");

        List<TileType> offeredMixedInvalid = Arrays.asList(
                TileType.WOOD, TileType.WOOD, TileType.WOOD,
                TileType.CLAY, TileType.CLAY,
                TileType.SHEEP
        );
        List<TileType> desiredForMixedInvalid = Arrays.asList(TileType.ORE, TileType.WHEAT);
        assertFalse(generalPort.canTrade(offeredMixedInvalid, desiredForMixedInvalid));
    }


    @Test
    void canTradeTradingForOfferedResourceShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> desired = Collections.singletonList(TileType.WOOD);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeTradingForOneOfMultipleOfferedResourcesShouldReturnFalse() {
        List<TileType> offered = Arrays.asList(
                TileType.WOOD, TileType.WOOD, TileType.WOOD,
                TileType.CLAY, TileType.CLAY, TileType.CLAY
        );
        List<TileType> desired = Arrays.asList(TileType.SHEEP, TileType.CLAY);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @ParameterizedTest
    @MethodSource("validTradeScenarios")
    void canTradeVariousValidScenariosShouldReturnTrue(List<TileType> offered, List<TileType> desired, String description) {
        assertTrue(generalPort.canTrade(offered, desired), "Failed: " + description);
    }

    static Stream<Arguments> validTradeScenarios() {
        return Stream.of(
                Arguments.of(
                        Arrays.asList(TileType.ORE, TileType.ORE, TileType.ORE),
                        Collections.singletonList(TileType.WHEAT),
                        "3 Ore for 1 Wheat"
                ),
                Arguments.of(
                        Arrays.asList(
                                TileType.SHEEP, TileType.SHEEP, TileType.SHEEP,
                                TileType.SHEEP, TileType.SHEEP, TileType.SHEEP
                        ),
                        Arrays.asList(TileType.WOOD, TileType.CLAY),
                        "6 Sheep for 1 Wood and 1 Clay"
                ),
                Arguments.of(
                        Arrays.asList(
                                TileType.WOOD, TileType.WOOD, TileType.WOOD,
                                TileType.CLAY, TileType.CLAY, TileType.CLAY,
                                TileType.ORE, TileType.ORE, TileType.ORE
                        ),
                        Arrays.asList(TileType.SHEEP, TileType.WHEAT, TileType.SHEEP),
                        "3W, 3C, 3O for 2S, 1Wh"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBundleScenarios")
    void canTradeVariousInvalidBundleScenariosShouldReturnFalse(List<TileType> offered, List<TileType> desired, String description) {
        assertFalse(generalPort.canTrade(offered, desired), "Failed: " + description);
    }

    static Stream<Arguments> invalidBundleScenarios() {
        return Stream.of(
                Arguments.of(
                        Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.CLAY, TileType.CLAY),
                        Arrays.asList(TileType.SHEEP, TileType.ORE),
                        "3W, 2C (total 5) for 2 resources - fails overall ratio"
                ),
                Arguments.of(
                        Arrays.asList(TileType.WOOD, TileType.WOOD, TileType.CLAY, TileType.CLAY, TileType.SHEEP, TileType.SHEEP),
                        Arrays.asList(TileType.ORE, TileType.WHEAT),
                        "2W, 2C, 2S for 2 resources - fails specific bundle check"
                )
        );
    }
}