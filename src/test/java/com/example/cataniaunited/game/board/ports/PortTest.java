package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.SettlementPosition;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class PortTest {

    private TestablePort port;
    private SettlementPosition mockSettlement1;
    private SettlementPosition mockSettlement2;

    @BeforeEach
    void setUp() {
        // Default port for most tests
        port = new TestablePort(3);
        mockSettlement1 = mock(SettlementPosition.class);
        mockSettlement2 = mock(SettlementPosition.class);
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
        Port customTestPort = new TestablePort(3);
        assertFalse(customTestPort.isNotTradingForOfferedResources(null, List.of(TileType.CLAY)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithNullDesiredShouldReturnFalse() {
        Port customTestPort = new TestablePort(3);
        assertFalse(customTestPort.isNotTradingForOfferedResources(List.of(TileType.WOOD), null));
    }
    @Test
    void isNotTradingForOfferedResourcesWithEmptyOfferedShouldReturnFalse() {
        Port customTestPort = new TestablePort(3);
        assertFalse(customTestPort.isNotTradingForOfferedResources(List.of(), List.of(TileType.CLAY)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithEmptyDesiredShouldReturnFalse() {
        Port customTestPort = new TestablePort(3);
        assertFalse(customTestPort.isNotTradingForOfferedResources(List.of(TileType.WOOD), List.of()));
    }

    @Test
    void isNotTradingForOfferedResourcesWithOfferedResourcesInDesiredShouldReturnFalse(){
        Port customTestPort = new TestablePort(3);
        assertFalse(customTestPort.isNotTradingForOfferedResources(List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD), List.of(TileType.WOOD)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithOneOfferedResourcesInDesiredShouldReturnFalse(){
        Port customTestPort = new TestablePort(3);
        assertFalse(customTestPort.isNotTradingForOfferedResources(List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD), List.of(TileType.WHEAT, TileType.WHEAT, TileType.WOOD)));
    }

    @Test
    void isNotTradingForOfferedResourcesWithNoOfferedResourcesInDesiredShouldReturnTrue(){
        Port customTestPort = new TestablePort(3);
        assertTrue(customTestPort.isNotTradingForOfferedResources(List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD), List.of(TileType.WHEAT)));
    }


    @Test
    void tradeRatioIsInvalidWithNullOfferedShouldReturnTrue() {
        assertTrue(port.tradeRatioIsInvalid(null, List.of(TileType.CLAY)));
    }

    @Test
    void tradeRatioIsInvalidWithNullDesiredShouldReturnTrue() {
        assertTrue(port.tradeRatioIsInvalid(List.of(TileType.WOOD), null));
    }

    @Test
    void tradeRatioIsInvalidWithEmptyOfferedShouldReturnTrue() {
        assertTrue(port.tradeRatioIsInvalid(List.of(), List.of(TileType.CLAY)));
    }

    @Test
    void tradeRatioIsInvalidWithEmptyDesiredShouldReturnTrue() {
        assertTrue(port.tradeRatioIsInvalid(List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD), List.of()));
    }

    @Test
    void tradeRatioIsInvalidWhenOfferedSizeNotMultipleOfInputAmountShouldReturnTrue() {
        // Port is 3:1, offering 2 resources
        assertTrue(port.tradeRatioIsInvalid(List.of(TileType.WOOD, TileType.SHEEP), List.of(TileType.CLAY)));
    }

    @Test
    void tradeRatioIsInvalidWhenOfferedSizeIsMultipleButDesiredSizeDoesNotMatchRatioShouldReturnTrue() {
        // Port is 3:1, offering 3, expecting 1, but desired is 2
        assertTrue(port.tradeRatioIsInvalid(List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD), List.of(TileType.CLAY, TileType.ORE)));
    }

    @Test
    void tradeRatioIsInvalidWhenOfferedSizeIsMultipleAndDesiredSizeMatchesRatioShouldReturnFalse() {
        // Port is 3:1, offering 3, desiring 1
        assertFalse(port.tradeRatioIsInvalid(List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD), List.of(TileType.CLAY)));
    }

    @Test
    void tradeRatioIsInvalidWithDifferentPortRatioShouldReturnFalse() {
        TestablePort port2To1 = new TestablePort(2);
        // Port is 2:1, offering 4, desiring 2
        assertFalse(port2To1.tradeRatioIsInvalid(List.of(TileType.WOOD, TileType.WOOD, TileType.SHEEP, TileType.SHEEP), List.of(TileType.CLAY, TileType.ORE)));
    }

    @Test
    void tradeRatioIsInvalidWithDifferentPortRatioAndIncorrectDesiredShouldReturnTrue() {
        TestablePort port2To1 = new TestablePort(2);
        // Port is 2:1, offering 4, desiring 1
        assertTrue(port2To1.tradeRatioIsInvalid(List.of(TileType.WOOD, TileType.WOOD, TileType.SHEEP, TileType.SHEEP), List.of(TileType.CLAY)));
    }


    @Test
    void setAssociatedSettlementsShouldSetSettlementPositions() {
        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        assertSame(mockSettlement1, port.settlementPosition1, "SettlementPosition1 was not set correctly.");
        assertSame(mockSettlement2, port.settlementPosition2, "SettlementPosition2 was not set correctly.");
    }

    @Test
    void getSettlementPositionShouldReturnEmptyListIfNoneOfTheSettlementPositionsAreSet(){
        assertEquals(List.of(), port.getSettlementPositions(), "getSettlementPosition should return empty list if not both settlements are set");
    }

    @Test
    void getSettlementPositionShouldReturnEmptyListIfOneOfTheSettlementPositionsIsNotSet(){
        port.setAssociatedSettlements(mockSettlement1, null);
        assertEquals(List.of(), port.getSettlementPositions(), "getSettlementPosition should return empty list if not both settlements are set");
    }

    @Test
    void getSettlementPositionShouldReturnEmptyListIfTheSecondOneOfTheSettlementPositionsIsNotSet(){
        port.setAssociatedSettlements(null, mockSettlement2);
        assertEquals(List.of(), port.getSettlementPositions(), "getSettlementPosition should return empty list if not both settlements are set");
    }

    @Test
    void getSettlementPositionShouldReturnCorrectListIfAllSettlementsAreSet(){
        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        assertEquals(List.of(mockSettlement1, mockSettlement2), port.getSettlementPositions(), "getSettlementPosition should return correct list");
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

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition(); // This will set port.portStructureTransform, etc.
        ObjectNode json = port.toJson();

        assertTrue(json.has("portVisuals"), "JSON should have portVisuals node.");
        JsonNode portVisuals = json.get("portVisuals");

        assertTrue(portVisuals.has("portTransform"), "PortVisuals should have portTransform node.");
        JsonNode portTransformNode = portVisuals.get("portTransform");
        assertEquals(port.portStructureTransform.x(), portTransformNode.get("x").asDouble(), 0.001);
        assertEquals(port.portStructureTransform.y(), portTransformNode.get("y").asDouble(), 0.001);
        assertEquals(port.portStructureTransform.rotation(), portTransformNode.get("rotation").asDouble(), 0.001);

        assertTrue(portVisuals.has("bridge1Transform"), "PortVisuals should have bridge1Transform node.");
        JsonNode bridge1Node = portVisuals.get("bridge1Transform");
        assertEquals(port.bridge1Transform.x(), bridge1Node.get("x").asDouble(), 0.001);
        assertEquals(port.bridge1Transform.y(), bridge1Node.get("y").asDouble(), 0.001);
        assertEquals(port.bridge1Transform.rotation(), bridge1Node.get("rotation").asDouble(), 0.001);

        assertTrue(portVisuals.has("bridge2Transform"), "PortVisuals should have bridge2Transform node.");
        JsonNode bridge2Node = portVisuals.get("bridge2Transform");
        assertEquals(port.bridge2Transform.x(), bridge2Node.get("x").asDouble(), 0.001);
        assertEquals(port.bridge2Transform.y(), bridge2Node.get("y").asDouble(), 0.001);
        assertEquals(port.bridge2Transform.rotation(), bridge2Node.get("rotation").asDouble(), 0.001);

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
        assertTrue(portVisuals.has("bridge1Transform"));
        assertEquals(Transform.ORIGIN.x(), portVisuals.get("bridge1Transform").get("x").asDouble(), 0.001);
        assertTrue(portVisuals.has("bridge2Transform"));
        assertEquals(Transform.ORIGIN.x(), portVisuals.get("bridge2Transform").get("x").asDouble(), 0.001);

        assertFalse(portVisuals.has("settlementPosition1Id"));
        assertFalse(portVisuals.has("settlementPosition2Id"));
    }
    @Test
    void toJsonShouldReflectManuallySetTransformsWhenSettlementsAreNull() {
        // Manually set transforms (possible because port fields are protected)
        port.portStructureTransform = new Transform(1.0, 2.0, 0.5);
        port.bridge1Transform = new Transform(0.1, 0.2, 0.3);
        port.bridge2Transform = new Transform(0.4, 0.5, 0.6);

        ObjectNode json = port.toJson();
        assertTrue(json.has("portVisuals"), "JSON should have portVisuals node.");
        JsonNode portVisuals = json.get("portVisuals");

        assertEquals(1.0, portVisuals.get("portTransform").get("x").asDouble(), 0.001);
        assertEquals(0.1, portVisuals.get("bridge1Transform").get("x").asDouble(), 0.001);
        assertEquals(0.4, portVisuals.get("bridge2Transform").get("x").asDouble(), 0.001);

        assertFalse(portVisuals.has("settlementPosition1Id"));
        assertFalse(portVisuals.has("settlementPosition2Id"));
    }

    @Test
    void calculatePositionWhenSettlementsAreNullShouldNotThrowExceptionAndTransformsRemainOrigin() {
        port.calculatePosition(); // settlements are null
        assertArrayEquals(Transform.ORIGIN.getCoordinatesArray(), port.getCoordinates(), "Coordinates should be from Transform.ORIGIN.");
        assertEquals(Transform.ORIGIN, port.portStructureTransform, "portStructureTransform should be ORIGIN.");
        assertEquals(Transform.ORIGIN, port.bridge1Transform, "bridge1Transform should be ORIGIN.");
    }

    @Test
    void calculatePositionWhenOneSettlementIsNullShouldNotThrowExceptionAndTransformsRemainOrigin() {
        port.setAssociatedSettlements(mockSettlement1, null);
        port.calculatePosition();
        assertArrayEquals(Transform.ORIGIN.getCoordinatesArray(), port.getCoordinates(), "Coordinates should be from Transform.ORIGIN.");
        assertEquals(Transform.ORIGIN, port.portStructureTransform, "portStructureTransform should be ORIGIN.");
    }

    @Test
    void calculatePositionWithValidSettlementsShouldCalculatePositions() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{20.0, 0.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portStructureTransform.x(), 0.001, "PortTransform X is incorrect.");
        assertEquals(10.0, port.portStructureTransform.y(), 0.001, "PortTransform Y is incorrect.");
        assertEquals(0.0, port.portStructureTransform.rotation(), 0.001, "PortTransform Rotation is incorrect.");

        assertEquals(5.0, port.bridge1Transform.x(), 0.001, "Bridge1Transform X is incorrect.");
        assertEquals(5.0, port.bridge1Transform.y(), 0.001, "Bridge1Transform Y is incorrect.");
        assertEquals(Math.atan2(10,10), port.bridge1Transform.rotation(), 0.001, "Bridge1Transform Rotation is incorrect.");

        assertEquals(15.0, port.bridge2Transform.x(), 0.001, "Bridge2Transform X is incorrect.");
        assertEquals(5.0, port.bridge2Transform.y(), 0.001, "Bridge2Transform Y is incorrect.");
        assertEquals(Math.atan2(10, -10), port.bridge2Transform.rotation(), 0.001, "Bridge2Transform Rotation is incorrect.");

        assertArrayEquals(new double[]{10.0, 10.0}, port.getCoordinates(), 0.001, "getCoordinates returned incorrect values.");
    }

    @Test
    void calculatePositionWithVerticalSettlementsShouldCalculatePositions() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{0.0, 20.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(-10.0, port.portStructureTransform.x(), 0.001, "PortTransform X is incorrect.");
        assertEquals(10.0, port.portStructureTransform.y(), 0.001, "PortTransform Y is incorrect.");
        assertEquals(Math.PI / 2, port.portStructureTransform.rotation(), 0.001, "PortTransform Rotation is incorrect.");

        assertEquals(-5.0, port.bridge1Transform.x(), 0.001);
        assertEquals(5.0, port.bridge1Transform.y(), 0.001);
        assertEquals(Math.atan2(10, -10), port.bridge1Transform.rotation(), 0.001);

        assertEquals(-5.0, port.bridge2Transform.x(), 0.001);
        assertEquals(15.0, port.bridge2Transform.y(), 0.001);
        assertEquals(Math.atan2(-10, -10), port.bridge2Transform.rotation(), 0.001);
    }

    @Test
    void calculatePositionNormalVectorFlipCheck() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{-20.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{0.0, -20.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
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

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portStructureTransform.x(), 0.001, "PortTransform X indicates unit normal was used for offset.");
        assertEquals(10.0, port.portStructureTransform.y(), 0.001, "PortTransform Y indicates unit normal was used for offset.");
        assertArrayEquals(new double[]{10.0, 10.0}, port.getCoordinates(), 0.001);
    }


    @Test
    void calculatePositionWhenSettlementsAreAtSameLocationShouldPlacePortAtMidpoint() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{10.0, 15.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{10.0, 15.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portStructureTransform.x(), 0.001, "PortTransform X should be midX as unitNormalX is 0.");
        assertEquals(15.0, port.portStructureTransform.y(), 0.001, "PortTransform Y should be midY as unitNormalY is 0.");
        assertEquals(0.0, port.portStructureTransform.rotation(), 0.001, "PortTransform Rotation should be 0 for identical points.");
        assertArrayEquals(new double[]{10.0, 15.0}, port.getCoordinates(), 0.001, "Port should be at the common settlement location.");

        assertEquals(10.0, port.bridge1Transform.x(), 0.001); // (x1 + portX)/2 = (10+10)/2 = 10
        assertEquals(15.0, port.bridge1Transform.y(), 0.001); // (y1 + portY)/2 = (15+15)/2 = 15
        assertEquals(0.0, port.bridge1Transform.rotation(), 0.001); // atan2(portY-y1, portX-x1) = atan2(0,0) = 0
    }

    @Test
    void toJsonWhenSettlement1IsNullShouldNotIncludeIds() {
        when(mockSettlement2.getId()).thenReturn(2);
        port.setAssociatedSettlements(null, mockSettlement2);

        // Manually set transforms to ensure portVisuals content is predictable beyond defaults
        port.portStructureTransform = new Transform(1.0, 2.0, 0.1);
        port.bridge1Transform = new Transform(0.11, 0.22, 0.33);
        port.bridge2Transform = new Transform(0.44, 0.55, 0.66);

        ObjectNode json = port.toJson();
        JsonNode portVisuals = json.get("portVisuals");

        assertNotNull(portVisuals, "portVisuals node should always exist.");
        assertFalse(portVisuals.has("settlementPosition1Id"), "Should NOT have settlementPosition1Id when s1 is null.");
        assertFalse(portVisuals.has("settlementPosition2Id"), "Should NOT have settlementPosition2Id when s1 is null (due to s1 && s2 condition).");

        assertTrue(portVisuals.has("portTransform"));
        assertEquals(1.0, portVisuals.get("portTransform").get("x").asDouble(), 0.001);
        assertTrue(portVisuals.has("bridge1Transform")); // Bridge transforms are always present
        assertEquals(0.11, portVisuals.get("bridge1Transform").get("x").asDouble(), 0.001);
    }

    @Test
    void toJsonWhenSettlement2IsNullShouldNotIncludeIds() {
        when(mockSettlement1.getId()).thenReturn(1);
        port.setAssociatedSettlements(mockSettlement1, null);

        port.portStructureTransform = new Transform(3.0, 4.0, 0.2);
        port.bridge1Transform = new Transform(0.11, 0.22, 0.33);
        port.bridge2Transform = new Transform(0.44, 0.55, 0.66);

        ObjectNode json = port.toJson();
        JsonNode portVisuals = json.get("portVisuals");

        assertNotNull(portVisuals, "portVisuals node should always exist.");
        assertFalse(portVisuals.has("settlementPosition1Id"), "Should NOT have settlementPosition1Id when s2 is null (due to s1 && s2 condition).");
        assertFalse(portVisuals.has("settlementPosition2Id"), "Should NOT have settlementPosition2Id when s2 is null.");

        assertTrue(portVisuals.has("portTransform"));
        assertEquals(3.0, portVisuals.get("portTransform").get("x").asDouble(), 0.001);
        assertTrue(portVisuals.has("bridge2Transform")); // Bridge transforms are always present
        assertEquals(0.44, portVisuals.get("bridge2Transform").get("x").asDouble(), 0.001);
    }
}