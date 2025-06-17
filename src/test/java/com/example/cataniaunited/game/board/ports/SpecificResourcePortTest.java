package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpecificResourcePortTest {
    @Test
    void constructorWithValidResourceShouldCreatePort() {
        SpecificResourcePort woodPort = new SpecificResourcePort(TileType.WOOD);
        assertEquals(2, woodPort.inputResourceAmount, "SpecificResourcePort should have an inputResourceAmount of 2.");
        assertEquals(TileType.WOOD, woodPort.getTradeAbleResource(), "Port resource should be WOOD.");
    }

    @Test
    void constructorWithNullResourceShouldThrowIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> new SpecificResourcePort(null));
        assertEquals("Specific port must trade a valid resource type.", exception.getMessage());
    }

    @Test
    void constructorWithWasteResourceShouldThrowIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> new SpecificResourcePort(TileType.WASTE));
        assertEquals("Specific port must trade a valid resource type.", exception.getMessage());
    }

    // Helper Method to find a resource that is different from the given one.
    private TileType getDifferentResource(List<TileType> typesToAvoid) {
        for (TileType t : TileType.values()) {
            if (!typesToAvoid.contains(t) && t != TileType.WASTE) {
                return t;
            }
        }
        // Should not happen in a normal Catan game context
        throw new IllegalStateException("No other resource type found to trade for.");
    }


    @ParameterizedTest
    @EnumSource(value = TileType.class, names = {"WOOD", "SHEEP", "WHEAT", "CLAY", "ORE"})
    void canTradeWithTwoCorrectResourcesForOneDifferentShouldReturnTrue(TileType portResource) {
        SpecificResourcePort port = new SpecificResourcePort(portResource);
        Map<TileType, Integer> offered = Map.of(portResource, 2);
        Map<TileType, Integer> desired = Map.of(getDifferentResource(List.of(portResource)), 1);
        assertTrue(port.canTrade(offered, desired), "Trading 2 " + portResource + " for 1 other should be valid.");
    }

    @ParameterizedTest
    @EnumSource(value = TileType.class, names = {"WOOD", "SHEEP", "WHEAT", "CLAY", "ORE"})
    void canTradeWithFourCorrectResourcesForTwoDifferentShouldReturnTrue(TileType portResource) {
        SpecificResourcePort port = new SpecificResourcePort(portResource);
        Map<TileType, Integer> offered = Map.of(portResource, 4);

        TileType desired1 = getDifferentResource(List.of(portResource));
        TileType desired2 = getDifferentResource(List.of(portResource, desired1));

        Map<TileType, Integer> desired = Map.of(desired1, 1, desired2, 1);
        assertTrue(port.canTrade(offered, desired), "Trading 4 " + portResource + " for 2 others should be valid.");
    }

    @Test
    void canTradeWithNullOfferedResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(port.canTrade(null, desired), "Trade with null offered resources should be invalid.");
    }

    @Test
    void canTradeWithNullDesiredResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 2);
        assertFalse(port.canTrade(offered, null), "Trade with null desired resources should be invalid.");
    }

    @Test
    void canTradeWithEmptyOfferedResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        Map<TileType, Integer> offered = Collections.emptyMap();
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(port.canTrade(offered, desired), "Trade with empty offered resources should be invalid.");
    }

    @Test
    void canTradeWithEmptyDesiredResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 2);
        Map<TileType, Integer> desired = Collections.emptyMap();
        assertFalse(port.canTrade(offered, desired), "Trade with empty desired resources should be invalid.");
    }

    @Test
    void canTradeWhenOfferedAmountIsNotMultipleOfRatioShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 1); // Only 1 offered for 2:1
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(port.canTrade(offered, desired), "Offering 1 for 1 at 2:1 port should be invalid.");
    }

    @Test
    void canTradeWhenDesiredAmountDoesNotMatchRatioShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 2); // Offering 2
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1, TileType.ORE, 1);  // Desiring 2
        assertFalse(port.canTrade(offered, desired), "Offering 2 for 2 at 2:1 port should be invalid.");
    }

    @Test
    void canTradeOfferingWrongResourceTypeForWoodPortShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        Map<TileType, Integer> offered = Map.of(TileType.SHEEP, 2); // Offering sheep to wood port
        Map<TileType, Integer> desired = Map.of(TileType.CLAY, 1);
        assertFalse(port.canTrade(offered, desired), "Offering sheep to WOOD port should be invalid.");
    }

    @Test
    void canTradeOfferingMixedResourceTypesWhenSpecificIsRequiredShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.ORE);
        Map<TileType, Integer> offered = Map.of(TileType.ORE, 1, TileType.WOOD, 1); // Mixed, but port needs only ORE
        Map<TileType, Integer> desired = Map.of(TileType.SHEEP, 1);
        assertFalse(port.canTrade(offered, desired), "Offering mixed types to ORE port should be invalid.");
    }

    @ParameterizedTest
    @EnumSource(value = TileType.class, names = {"WOOD", "SHEEP", "WHEAT", "CLAY", "ORE"})
    void canTradeWhenDesiredResourceIsTheSameAsPortTypeShouldReturnFalse(TileType portResource) {
        SpecificResourcePort port = new SpecificResourcePort(portResource);
        Map<TileType, Integer> offered = Map.of(portResource, 2);
        Map<TileType, Integer> desired = Map.of(portResource, 1);
        assertFalse(port.canTrade(offered, desired), "Trading " + portResource + " for " + portResource + " should be invalid.");
    }

    @ParameterizedTest
    @EnumSource(value = TileType.class, names = {"WOOD", "SHEEP", "WHEAT", "CLAY", "ORE"})
    void getTradeAbleResourceShouldReturnCorrectResource(TileType portResource) {
        SpecificResourcePort woodPort = new SpecificResourcePort(portResource);
        assertEquals(portResource, woodPort.getTradeAbleResource(), "Wrong Resource Returned.");
    }

    @Test
    void toJsonShouldIncludePortTypeResourceAndSuperClassData() {
        SpecificResourcePort woodPort = new SpecificResourcePort(TileType.WOOD);

        ObjectNode jsonNode = woodPort.toJson();

        assertTrue(jsonNode.has("portType"), "JSON should contain 'portType' field.");
        assertEquals("SpecificResourcePort", jsonNode.get("portType").asText(), "'portType' field should be 'SpecificResourcePort'.");

        assertTrue(jsonNode.has("resource"), "JSON should contain 'resource' field for the specific tradeable resource.");
        assertEquals(TileType.WOOD.name(), jsonNode.get("resource").asText(), "'resource' field should be 'WOOD'.");

        assertTrue(jsonNode.has("inputResourceAmount"), "JSON should contain 'inputResourceAmount' from superclass.");
        assertEquals(2, jsonNode.get("inputResourceAmount").asInt(), "'inputResourceAmount' should be 3 for a GeneralPort.");

        assertTrue(jsonNode.has("portVisuals"), "JSON should contain 'portStructure' node from superclass.");
        JsonNode portStructureNode = jsonNode.get("portVisuals");

        assertTrue(portStructureNode.has("portTransform"), "'portStructure' should contain a 'port' object.");
        JsonNode portSubNode = portStructureNode.get("portTransform");
        assertEquals(0.0, portSubNode.get("x").asDouble(), 0.001, "Default port x-coordinate should be 0.0.");
        assertEquals(0.0, portSubNode.get("y").asDouble(), 0.001, "Default port y-coordinate should be 0.0.");
        assertEquals(0.0, portSubNode.get("rotation").asDouble(), 0.001, "Default port rotation should be 0.0.");

        assertFalse(portStructureNode.has("settlementPosition1Id"), "Should not have settlementPosition1Id when settlements are not set.");
        assertFalse(portStructureNode.has("settlementPosition2Id"), "Should not have settlementPosition2Id when settlements are not set.");

        SpecificResourcePort sheepPort = new SpecificResourcePort(TileType.SHEEP);
        ObjectNode sheepJsonNode = sheepPort.toJson();
        assertTrue(sheepJsonNode.has("resource"), "JSON for sheep port should contain 'resource' field.");
        assertEquals(TileType.SHEEP.name(), sheepJsonNode.get("resource").asText(), "'resource' field should be 'SHEEP' for a sheep port.");
    }
}