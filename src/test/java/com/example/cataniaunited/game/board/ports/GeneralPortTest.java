package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Map;
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
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3);
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertTrue(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeValidMultipleTypesTradeShouldReturnTrue() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3, TileType.CLAY, 3);
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1, TileType.ORE, 1);
        assertTrue(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeValidSixForTwoShouldReturnTrue() {
        Map<TileType, Integer> offered = Map.of(TileType.WHEAT, 6);
        Map<TileType, Integer> desired = Map.of(TileType.ORE, 1, TileType.SHEEP, 1);
        assertTrue(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeEmptyOfferedMapShouldReturnFalse() {
        Map<TileType, Integer> offered = Collections.emptyMap();
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeNullOfferedMapShouldReturnFalse() {
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(generalPort.canTrade(null, desired));
    }

    @Test
    void canTradeEmptyDesiredMapShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3);
        Map<TileType, Integer> desired = Collections.emptyMap();
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeNullDesiredMapShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3);
        assertFalse(generalPort.canTrade(offered, null));
    }

    @Test
    void canTradeOfferedNotMultipleOfRatioShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 2);
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeDesiredAmountMismatchShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3);
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1, TileType.ORE, 1);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeOfferedSixButWantsOneShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WHEAT, 6);
        Map<TileType, Integer> desired = Map.of(TileType.ORE, 1);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeInvalidBundleSingleTypeShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 4);
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(generalPort.canTrade(offered, desired), "Should fail because 4 is not a multiple of 3");
    }

    @Test
    void canTradeInvalidMixedBundleShouldReturnFalse() {
        Map<TileType, Integer> offeredMixedInvalid = Map.of(TileType.WOOD, 3, TileType.CLAY, 2, TileType.SHEEP, 1);
        Map<TileType, Integer> desiredForMixedInvalid = Map.of(TileType.ORE, 1, TileType.WHEAT, 1);
        assertFalse(generalPort.canTrade(offeredMixedInvalid, desiredForMixedInvalid));
    }

    @Test
    void canTradeTradingForOfferedResourceShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3);
        Map<TileType, Integer> desired = Map.of(TileType.WOOD, 1);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @Test
    void canTradeTradingForOneOfMultipleOfferedResourcesShouldReturnFalse() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3, TileType.CLAY, 3);
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1, TileType.CLAY, 1);
        assertFalse(generalPort.canTrade(offered, desired));
    }

    @ParameterizedTest
    @MethodSource("validTradeScenarios")
    void canTradeVariousValidScenariosShouldReturnTrue(Map<TileType, Integer> offered, Map<TileType, Integer> desired, String description) {
        assertTrue(generalPort.canTrade(offered, desired), "Failed: " + description);
    }

    static Stream<Arguments> validTradeScenarios() {
        return Stream.of(
                Arguments.of(Map.of(TileType.ORE, 3), Map.of(TileType.WHEAT, 1), "3 Ore for 1 Wheat"),
                Arguments.of(Map.of(TileType.SHEEP, 6), Map.of(TileType.WOOD, 1, TileType.CLAY, 1), "6 Sheep for 1 Wood and 1 Clay"),
                Arguments.of(Map.of(TileType.WOOD, 3, TileType.CLAY, 3, TileType.ORE, 3), Map.of(TileType.SHEEP, 2, TileType.WHEAT, 1), "3W, 3C, 3O for 2S, 1Wh")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBundleScenarios")
    void canTradeVariousInvalidBundleScenariosShouldReturnFalse(Map<TileType, Integer> offered, Map<TileType, Integer> desired, String description) {
        assertFalse(generalPort.canTrade(offered, desired), "Failed: " + description);
    }

    static Stream<Arguments> invalidBundleScenarios() {
        return Stream.of(
                Arguments.of(Map.of(TileType.WOOD, 3, TileType.CLAY, 2), Map.of(TileType.SHEEP, 1, TileType.ORE, 1), "3W, 2C (total 5) for 2 resources - fails overall ratio"),
                Arguments.of(Map.of(TileType.WOOD, 2, TileType.CLAY, 2, TileType.SHEEP, 2), Map.of(TileType.ORE, 2), "2W, 2C, 2S for 2 resources - fails specific bundle check")
        );
    }

    @Test
    void toJsonShouldIncludePortTypeAndSuperClassData() {
        ObjectNode jsonNode = generalPort.toJson();

        assertTrue(jsonNode.has("portType"), "JSON should contain 'portType' field.");
        assertEquals("GeneralPort", jsonNode.get("portType").asText(), "'portType' field should be 'GeneralPort'.");

        assertTrue(jsonNode.has("inputResourceAmount"), "JSON should contain 'inputResourceAmount' from superclass.");
        assertEquals(3, jsonNode.get("inputResourceAmount").asInt(), "'inputResourceAmount' should be 3 for a GeneralPort.");

        assertTrue(jsonNode.has("portVisuals"), "JSON should contain 'portStructure' node from superclass.");
        JsonNode portStructureNode = jsonNode.get("portVisuals");

        assertTrue(portStructureNode.has("portTransform"), "'portStructure' should contain a 'port' object.");
        JsonNode portSubNode = portStructureNode.get("portTransform");
        assertEquals(0.0, portSubNode.get("x").asDouble(), 0.001, "Default port x-coordinate should be 0.0.");
        assertEquals(0.0, portSubNode.get("y").asDouble(), 0.001, "Default port y-coordinate should be 0.0.");
        assertEquals(0.0, portSubNode.get("rotation").asDouble(), 0.001, "Default port rotation should be 0.0.");

        assertFalse(portStructureNode.has("settlementPosition1Id"), "Should not have settlementPosition1Id when settlements are not set.");
        assertFalse(portStructureNode.has("settlementPosition2Id"), "Should not have settlementPosition2Id when settlements are not set.");
    }
}