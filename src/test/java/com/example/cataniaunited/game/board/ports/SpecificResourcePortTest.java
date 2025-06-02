package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;


import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpecificResourcePortTest {
    @Test
    void constructorWithValidResourceShouldCreatePort() {
        SpecificResourcePort woodPort = new SpecificResourcePort(TileType.WOOD);
        assertEquals(2, woodPort.inputResourceAmount, "SpecificResourcePort should have an inputResourceAmount of 2.");
        assertEquals(TileType.WOOD, woodPort.tradeAbleResource, "Port resource should be WOOD.");
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

    // Helper Method to avoid complications
    private TileType getDifferentResource(TileType typeToAvoid) {
        for (TileType t : TileType.values()) {
            if (t != typeToAvoid && t != TileType.WASTE) {
                return t;
            }
        }
        return TileType.WASTE;
    }


    @ParameterizedTest
    @EnumSource(value = TileType.class, names = {"WOOD", "SHEEP", "WHEAT", "CLAY", "ORE"})
    void canTradeWithTwoCorrectResourcesForOneDifferentShouldReturnTrue(TileType portResource) {
        SpecificResourcePort port = new SpecificResourcePort(portResource);
        List<TileType> offered = Arrays.asList(portResource, portResource);
        List<TileType> desired = Collections.singletonList(getDifferentResource(portResource));
        assertTrue(port.canTrade(offered, desired), "Trading 2 " + portResource + " for 1 other should be valid.");
    }

    @ParameterizedTest
    @EnumSource(value = TileType.class, names = {"WOOD", "SHEEP", "WHEAT", "CLAY", "ORE"})
    void canTradeWithFourCorrectResourcesForTwoDifferentShouldReturnTrue(TileType portResource) {
        SpecificResourcePort port = new SpecificResourcePort(portResource);
        List<TileType> offered = Arrays.asList(portResource, portResource, portResource, portResource);

        TileType desired1 = getDifferentResource(portResource);
        TileType desired2 = getDifferentResource(portResource);
        if (desired1 == desired2) { // ensure they are different
            for (TileType t : TileType.values()) {
                if (t != portResource && t != TileType.WASTE && t != desired1) {
                    desired2 = t;
                    break;
                }
            }
        }

        List<TileType> desired = Arrays.asList(desired1, desired2);
        assertTrue(port.canTrade(offered, desired), "Trading 4 " + portResource + " for 2 others should be valid.");
    }

    @Test
    void canTradeWithNullOfferedResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(port.canTrade(null, desired), "Trade with null offered resources should be invalid.");
    }

    @Test
    void canTradeWithNullDesiredResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD);
        assertFalse(port.canTrade(offered, null), "Trade with null desired resources should be invalid.");
    }

    @Test
    void canTradeWithEmptyOfferedResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        List<TileType> offered = Collections.emptyList();
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(port.canTrade(offered, desired), "Trade with empty offered resources should be invalid.");
    }

    @Test
    void canTradeWithEmptyDesiredResourcesShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD);
        List<TileType> desired = Collections.emptyList();
        assertFalse(port.canTrade(offered, desired), "Trade with empty desired resources should be invalid.");
    }


    @Test
    void canTradeWhenOfferedAmountIsNotMultipleOfRatioShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        List<TileType> offered = Collections.singletonList(TileType.WOOD); // Only 1 offered for 2:1
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(port.canTrade(offered, desired), "Offering 1 for 1 at 2:1 port should be invalid.");
    }

    @Test
    void canTradeWhenDesiredAmountDoesNotMatchRatioShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        List<TileType> offered = Arrays.asList(TileType.WOOD, TileType.WOOD); // Offering 2
        List<TileType> desired = Arrays.asList(TileType.SHEEP, TileType.ORE);  // Eypecting 2
        assertFalse(port.canTrade(offered, desired), "Offering 2 for 2 at 2:1 port should be invalid.");
    }

    @Test
    void canTradeOfferingWrongResourceTypeForWoodPortShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.WOOD);
        List<TileType> offered = Arrays.asList(TileType.SHEEP, TileType.SHEEP); // Offering sheep to wood port
        List<TileType> desired = Collections.singletonList(TileType.CLAY);
        assertFalse(port.canTrade(offered, desired), "Offering sheep to WOOD port should be invalid.");
    }

    @Test
    void canTradeOfferingMixedResourceTypesWhenSpecificIsRequiredShouldReturnFalse() {
        SpecificResourcePort port = new SpecificResourcePort(TileType.ORE);
        List<TileType> offered = Arrays.asList(TileType.ORE, TileType.WOOD); // Mixed, but port needs only ORE
        List<TileType> desired = Collections.singletonList(TileType.SHEEP);
        assertFalse(port.canTrade(offered, desired), "Offering mixed types to ORE port should be invalid.");
    }

    @ParameterizedTest
    @EnumSource(value = TileType.class, names = {"WOOD", "SHEEP", "WHEAT", "CLAY", "ORE"})
    void canTradeWhenDesiredResourceIsTheSameAsPortTypeShouldReturnFalse(TileType portResource) {
        SpecificResourcePort port = new SpecificResourcePort(portResource);
        List<TileType> offered = Arrays.asList(portResource, portResource);
        List<TileType> desired = Collections.singletonList(portResource);
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