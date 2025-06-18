package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.BuildingSite;
import com.example.cataniaunited.game.board.Transform;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class PortTest {

    private TestablePort port;
    private BuildingSite mockSettlement1;
    private BuildingSite mockSettlement2;

    @BeforeEach
    void setUp() {
        // Default port for most tests
        port = new TestablePort(3);
        mockSettlement1 = mock(BuildingSite.class);
        mockSettlement2 = mock(BuildingSite.class);
    }

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
        assertFalse(port.isNotTradingForOfferedResources(null, Map.of(TileType.CLAY, 1)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithNullDesiredShouldReturnFalse() {
        assertFalse(port.isNotTradingForOfferedResources(Map.of(TileType.WOOD, 1), null));
    }
    @Test
    void isNotTradingForOfferedResourcesWithEmptyOfferedShouldReturnFalse() {
        assertFalse(port.isNotTradingForOfferedResources(Collections.emptyMap(), Map.of(TileType.CLAY, 1)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithEmptyDesiredShouldReturnFalse() {
        assertFalse(port.isNotTradingForOfferedResources(Map.of(TileType.WOOD, 1), Collections.emptyMap()));
    }

    @Test
    void isNotTradingForOfferedResourcesWithOverlapShouldReturnFalse() {
        assertFalse(port.isNotTradingForOfferedResources(Map.of(TileType.WOOD, 3), Map.of(TileType.WOOD, 1)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithPartialOverlapShouldReturnFalse() {
        assertFalse(port.isNotTradingForOfferedResources(Map.of(TileType.WOOD, 6, TileType.SHEEP, 3), Map.of(TileType.WHEAT, 2, TileType.WOOD, 1)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithNoOverlapShouldReturnTrue() {
        assertTrue(port.isNotTradingForOfferedResources(Map.of(TileType.WOOD, 3), Map.of(TileType.WHEAT, 1)));
    }


    @Test
    void tradeRatioIsInvalidWithNullOfferedShouldReturnTrue() {
        assertTrue(port.tradeRatioIsInvalid(null, Map.of(TileType.CLAY, 1)));
    }

    @Test
    void tradeRatioIsInvalidWithEmptyOfferedShouldReturnTrue() {
        assertTrue(port.tradeRatioIsInvalid(Collections.emptyMap(), Map.of(TileType.CLAY, 1)));
    }

    @Test
    void tradeRatioIsInvalidWhenOfferedSizeNotMultipleOfInputAmountShouldReturnTrue() {
        // Port is 3:1, offering 2 resources total
        assertTrue(port.tradeRatioIsInvalid(Map.of(TileType.WOOD, 1, TileType.SHEEP, 1), Map.of(TileType.CLAY, 1)));
    }

    @Test
    void tradeRatioIsInvalidWhenDesiredAmountDoesNotMatchRatioShouldReturnTrue() {
        // Port is 3:1, offering 3, expecting 1, but desiring 2
        assertTrue(port.tradeRatioIsInvalid(Map.of(TileType.WOOD, 3), Map.of(TileType.CLAY, 2)));
    }

    @Test
    void tradeRatioIsInvalidWithValidRatioShouldReturnFalse() {
        // Port is 3:1, offering 6 total, desiring 2 total
        assertFalse(port.tradeRatioIsInvalid(Map.of(TileType.WOOD, 6), Map.of(TileType.CLAY, 2)));
    }


    @Test
    void setAssociatedSettlementsShouldSetSettlementPositions() {
        port.setAssociatedBuildingSites(mockSettlement1, mockSettlement2);
        assertSame(mockSettlement1, port.buildingSite1, "SettlementPosition1 was not set correctly.");
        assertSame(mockSettlement2, port.buildingSite2, "SettlementPosition2 was not set correctly.");
    }

    @Test
    void getSettlementPositionShouldReturnEmptyListIfNoneOfTheSettlementPositionsAreSet(){
        assertEquals(List.of(), port.getBuildingSites(), "getSettlementPosition should return empty list if not both settlements are set");
    }

    @Test
    void getSettlementPositionShouldReturnEmptyListIfOneOfTheSettlementPositionsIsNotSet(){
        port.setAssociatedBuildingSites(mockSettlement1, null);
        assertEquals(List.of(), port.getBuildingSites(), "getSettlementPosition should return empty list if not both settlements are set");
    }

    @Test
    void getSettlementPositionShouldReturnEmptyListIfTheSecondOneOfTheSettlementPositionsIsNotSet(){
        port.setAssociatedBuildingSites(null, mockSettlement2);
        assertEquals(List.of(), port.getBuildingSites(), "getSettlementPosition should return empty list if not both settlements are set");
    }

    @Test
    void getSettlementPositionShouldReturnCorrectListIfAllSettlementsAreSet(){
        port.setAssociatedBuildingSites(mockSettlement1, mockSettlement2);
        assertEquals(List.of(mockSettlement1, mockSettlement2), port.getBuildingSites(), "getSettlementPosition should return correct list");
    }

    @Test
    void getCoordinatesShouldReturnCoordinatesFromManuallySetTransform() {
        port.portStructureTransform = new Transform(12.3, 45.6, 0.0);
        assertArrayEquals(new double[]{12.3, 45.6}, port.getCoordinates(), "getCoordinates did not return the set port center.");
    }

    @Test
    void getCoordinatesBeforeCalculationShouldReturnDefaultZeros() {
        assertArrayEquals(new double[]{0.0, 0.0}, port.getCoordinates(), "getCoordinates should return [0,0] from Transform.ORIGIN before calculation.");
    }


    @Test
    void toJsonShouldContainInputResourceAmount() {
        TestablePort port2To1 = new TestablePort(2);

        ObjectNode json = port2To1.toJson();
        assertEquals(2, json.get("inputResourceAmount").asInt());

        TestablePort port4To1 = new TestablePort(4);
        json = port4To1.toJson();
        assertEquals(4, json.get("inputResourceAmount").asInt());
    }

    @Test
    void toJsonShouldContainVisualsAndTransformsWhenCalculated() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{20.0, 0.0});
        when(mockSettlement1.getId()).thenReturn(1);
        when(mockSettlement2.getId()).thenReturn(2);

        port.setAssociatedBuildingSites(mockSettlement1, mockSettlement2);
        port.calculatePosition(); // This will set port.portStructureTransform, etc.
        ObjectNode json = port.toJson();

        assertTrue(json.has("portVisuals"), "JSON should have portVisuals node.");
        JsonNode portVisuals = json.get("portVisuals");

        assertTrue(portVisuals.has("portTransform"), "PortVisuals should have portTransform node.");
        JsonNode portTransformNode = portVisuals.get("portTransform");
        assertEquals(port.portStructureTransform.x(), portTransformNode.get("x").asDouble(), 0.001);
        assertEquals(port.portStructureTransform.y(), portTransformNode.get("y").asDouble(), 0.001);
        assertEquals(port.portStructureTransform.rotation(), portTransformNode.get("rotation").asDouble(), 0.001);

        // Assert 'buildingSite1Position' within 'portVisuals'
        assertTrue(portVisuals.has("buildingSite1Position"), "PortVisuals should have buildingSite1Position node.");
        JsonNode bs1PosNode = portVisuals.get("buildingSite1Position");
        assertNotNull(bs1PosNode, "buildingSite1Position node should not be null.");
        assertTrue(bs1PosNode.isArray(), "buildingSite1Position should be an array.");
        assertEquals(2, bs1PosNode.size(), "buildingSite1Position array should have 2 elements.");
        assertEquals(mockSettlement1.getCoordinates()[0], bs1PosNode.get(0).asDouble(), 0.001, "X coordinate of buildingSite1Position");
        assertEquals(mockSettlement1.getCoordinates()[1], bs1PosNode.get(1).asDouble(), 0.001, "Y coordinate of buildingSite1Position");

        // Assert 'buildingSite2Position' within 'portVisuals'
        assertTrue(portVisuals.has("buildingSite2Position"), "PortVisuals should have buildingSite2Position node.");
        JsonNode bs2PosNode = portVisuals.get("buildingSite2Position");
        assertNotNull(bs2PosNode, "buildingSite2Position node should not be null.");
        assertTrue(bs2PosNode.isArray(), "buildingSite2Position should be an array.");
        assertEquals(2, bs2PosNode.size(), "buildingSite2Position array should have 2 elements.");
        assertEquals(mockSettlement2.getCoordinates()[0], bs2PosNode.get(0).asDouble(), 0.001, "X coordinate of buildingSite2Position");
        assertEquals(mockSettlement2.getCoordinates()[1], bs2PosNode.get(1).asDouble(), 0.001, "Y coordinate of buildingSite2Position");

        assertTrue(portVisuals.has("settlementPosition1Id"), "PortVisuals should have settlementPosition1Id.");
        assertEquals(1, portVisuals.get("settlementPosition1Id").asInt());
        assertTrue(portVisuals.has("settlementPosition2Id"), "PortVisuals should have settlementPosition2Id.");
        assertEquals(2, portVisuals.get("settlementPosition2Id").asInt());
    }

    @Test
    void toJsonShouldNotContainSettlementIdsIfSettlementsAreNull() {
        // port.setAssociatedSettlements(null, null); // Default is null
        ObjectNode json = port.toJson();
        JsonNode portVisuals = json.get("portVisuals");
        assertNotNull(portVisuals, "portVisuals node should always exist.");

        assertFalse(portVisuals.has("settlementPosition1Id"), "PortVisuals should not have settlementPosition1Id if settlements are null.");
        assertFalse(portVisuals.has("settlementPosition2Id"), "PortVisuals should not have settlementPosition2Id if settlements are null.");
    }

    @Test
    void toJsonShouldHaveVisualsWithDefaultTransformsEvenIfSettlementsAreNullAndNoManualSet() {
        // Here, port.portStructureTransform etc. are Transform.ORIGIN by constructor
        ObjectNode json = port.toJson();
        assertTrue(json.has("portVisuals"), "JSON should have portVisuals node.");
        JsonNode portVisuals = json.get("portVisuals");

        assertTrue(portVisuals.has("portTransform"));
        assertEquals(Transform.ORIGIN.x(), portVisuals.get("portTransform").get("x").asDouble(), 0.001);

        assertFalse(portVisuals.has("settlementPosition1Id"));
        assertFalse(portVisuals.has("settlementPosition2Id"));
    }
    @Test
    void toJsonShouldReflectManuallySetTransformsWhenSettlementsAreNull() {
        // Manually set transforms (possible because port fields are protected)
        port.portStructureTransform = new Transform(1.0, 2.0, 0.5);

        ObjectNode json = port.toJson();
        assertTrue(json.has("portVisuals"), "JSON should have portVisuals node.");
        JsonNode portVisuals = json.get("portVisuals");

        assertEquals(1.0, portVisuals.get("portTransform").get("x").asDouble(), 0.001);

        assertFalse(portVisuals.has("settlementPosition1Id"));
        assertFalse(portVisuals.has("settlementPosition2Id"));
    }

    @Test
    void calculatePositionWhenSettlementsAreNullShouldNotThrowExceptionAndTransformsRemainOrigin() {
        port.calculatePosition(); // settlements are null
        assertArrayEquals(Transform.ORIGIN.getCoordinatesArray(), port.getCoordinates(), "Coordinates should be from Transform.ORIGIN.");
        assertEquals(Transform.ORIGIN, port.portStructureTransform, "portStructureTransform should be ORIGIN.");
    }

    @Test
    void calculatePositionWhenOneSettlementIsNullShouldNotThrowExceptionAndTransformsRemainOrigin() {
        port.setAssociatedBuildingSites(mockSettlement1, null);
        port.calculatePosition();
        assertArrayEquals(Transform.ORIGIN.getCoordinatesArray(), port.getCoordinates(), "Coordinates should be from Transform.ORIGIN.");
        assertEquals(Transform.ORIGIN, port.portStructureTransform, "portStructureTransform should be ORIGIN.");
    }

    @Test
    void calculatePositionWithValidSettlementsShouldCalculatePositions() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{20.0, 0.0});

        port.setAssociatedBuildingSites(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portStructureTransform.x(), 0.001, "PortTransform X is incorrect.");
        assertEquals(10.0, port.portStructureTransform.y(), 0.001, "PortTransform Y is incorrect.");
        assertEquals(0.0, port.portStructureTransform.rotation(), 0.001, "PortTransform Rotation is incorrect.");

        assertArrayEquals(new double[]{10.0, 10.0}, port.getCoordinates(), 0.001, "getCoordinates returned incorrect values.");
    }

    @Test
    void calculatePositionNormalVectorFlipCheck() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{-20.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{0.0, -20.0});

        port.setAssociatedBuildingSites(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        double expectedPortX = -10.0 - (10.0 / Math.sqrt(2)); // midX + unitNormalX * PORT_DISTANCE
        double expectedPortY = -10.0 - (10.0 / Math.sqrt(2)); // midY + unitNormalY * PORT_DISTANCE

        assertEquals(expectedPortX, port.portStructureTransform.x(), 0.001, "PortTransform X after normal flip is incorrect.");
        assertEquals(expectedPortY, port.portStructureTransform.y(), 0.001, "PortTransform Y after normal flip is incorrect.");
        assertEquals(Math.atan2(-20, 20), port.portStructureTransform.rotation(), 0.001, "PortTransform Rotation is incorrect.");
    }

    @Test
    void calculatePositionWithValidSettlementsEnsuresUnitNormalCalculated() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{20.0, 0.0});

        port.setAssociatedBuildingSites(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portStructureTransform.x(), 0.001, "PortTransform X indicates unit normal was used for offset.");
        assertEquals(10.0, port.portStructureTransform.y(), 0.001, "PortTransform Y indicates unit normal was used for offset.");
        assertArrayEquals(new double[]{10.0, 10.0}, port.getCoordinates(), 0.001);
    }


    @Test
    void calculatePositionWhenSettlementsAreAtSameLocationShouldPlacePortAtMidpoint() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{10.0, 15.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{10.0, 15.0});

        port.setAssociatedBuildingSites(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portStructureTransform.x(), 0.001, "PortTransform X should be midX as unitNormalX is 0.");
        assertEquals(15.0, port.portStructureTransform.y(), 0.001, "PortTransform Y should be midY as unitNormalY is 0.");
        assertEquals(0.0, port.portStructureTransform.rotation(), 0.001, "PortTransform Rotation should be 0 for identical points.");
        assertArrayEquals(new double[]{10.0, 15.0}, port.getCoordinates(), 0.001, "Port should be at the common settlement location.");
    }

    @Test
    void toJsonWhenSettlement1IsNullShouldNotIncludeIds() {
        when(mockSettlement2.getId()).thenReturn(2);
        port.setAssociatedBuildingSites(null, mockSettlement2);

        // Manually set transforms to ensure portVisuals content is predictable beyond defaults
        port.portStructureTransform = new Transform(1.0, 2.0, 0.1);

        ObjectNode json = port.toJson();
        JsonNode portVisuals = json.get("portVisuals");

        assertNotNull(portVisuals, "portVisuals node should always exist.");
        assertFalse(portVisuals.has("settlementPosition1Id"), "Should NOT have settlementPosition1Id when s1 is null.");
        assertFalse(portVisuals.has("settlementPosition2Id"), "Should NOT have settlementPosition2Id when s1 is null (due to s1 && s2 condition).");

        assertTrue(portVisuals.has("portTransform"));
        assertEquals(1.0, portVisuals.get("portTransform").get("x").asDouble(), 0.001);
    }

    @Test
    void toJsonWhenSettlement2IsNullShouldNotIncludeIds() {
        when(mockSettlement1.getId()).thenReturn(1);
        port.setAssociatedBuildingSites(mockSettlement1, null);

        port.portStructureTransform = new Transform(3.0, 4.0, 0.2);

        ObjectNode json = port.toJson();
        JsonNode portVisuals = json.get("portVisuals");

        assertNotNull(portVisuals, "portVisuals node should always exist.");
        assertFalse(portVisuals.has("settlementPosition1Id"), "Should NOT have settlementPosition1Id when s2 is null (due to s1 && s2 condition).");
        assertFalse(portVisuals.has("settlementPosition2Id"), "Should NOT have settlementPosition2Id when s2 is null.");

        assertTrue(portVisuals.has("portTransform"));
        assertEquals(3.0, portVisuals.get("portTransform").get("x").asDouble(), 0.001);
    }
}