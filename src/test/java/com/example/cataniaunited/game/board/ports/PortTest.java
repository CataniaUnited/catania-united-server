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
    void getCoordinatesShouldReturnCalculatedPortCenter() {
        port.portCenterX = 12.3;
        port.portCenterY = 45.6;
        assertArrayEquals(new double[]{12.3, 45.6}, port.getCoordinates(), "getCoordinates did not return the set port center.");
    }

    @Test
    void getCoordinatesBeforeCalculationShouldReturnDefaultZeros() {
        assertArrayEquals(new double[]{0.0, 0.0}, port.getCoordinates(), "getCoordinates should return [0,0] before calculation.");
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
    void toJsonShouldContainPortStructureWhenCalculated() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{20.0, 0.0});
        when(mockSettlement1.getId()).thenReturn(1);
        when(mockSettlement2.getId()).thenReturn(2);

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();
        ObjectNode json = port.toJson();

        assertTrue(json.has("portStructure"), "JSON should have portStructure node.");
        JsonNode portStructure = json.get("portStructure");

        assertTrue(portStructure.has("port"), "PortStructure should have port node.");
        JsonNode portSubNode = portStructure.get("port");
        assertEquals(port.portCenterX, portSubNode.get("x").asDouble(), 0.001);
        assertEquals(port.portCenterY, portSubNode.get("y").asDouble(), 0.001);
        assertEquals(port.portRotation, portSubNode.get("rotation").asDouble(), 0.001);

        assertTrue(portStructure.has("bridge1"), "PortStructure should have bridge1 node.");
        JsonNode bridge1Node = portStructure.get("bridge1");
        assertEquals(port.bridgeX1, bridge1Node.get("x").asDouble(), 0.001);
        assertEquals(port.bridgeY1, bridge1Node.get("y").asDouble(), 0.001);
        assertEquals(port.bridge1Rotation, bridge1Node.get("rotation").asDouble(), 0.001);

        assertTrue(portStructure.has("bridge2"), "PortStructure should have bridge2 node.");
        JsonNode bridge2Node = portStructure.get("bridge2");
        assertEquals(port.bridgeX2, bridge2Node.get("x").asDouble(), 0.001);
        assertEquals(port.bridgeY2, bridge2Node.get("y").asDouble(), 0.001);
        assertEquals(port.bridge2Rotation, bridge2Node.get("rotation").asDouble(), 0.001);

        assertTrue(portStructure.has("settlementPosition1Id"), "PortStructure should have settlementPosition1Id.");
        assertEquals(1, portStructure.get("settlementPosition1Id").asInt());
        assertTrue(portStructure.has("settlementPosition2Id"), "PortStructure should have settlementPosition2Id.");
        assertEquals(2, portStructure.get("settlementPosition2Id").asInt());
    }

    @Test
    void toJsonShouldNotContainSettlementIdsIfSettlementsAreNull() {
        ObjectNode json = port.toJson();
        JsonNode portStructure = json.get("portStructure");

        assertFalse(portStructure.has("settlementPosition1Id"), "PortStructure should not have settlementPosition1Id if null.");
        assertFalse(portStructure.has("settlementPosition2Id"), "PortStructure should not have settlementPosition2Id if null.");
    }

    @Test
    void toJsonShouldStillHavePortStructureEvenIfSettlementsAreNullButCoordinatesWereManuallySet() {
        port.portCenterX = 1.0;
        port.portCenterY = 2.0;
        port.portRotation = 0.5;
        port.bridgeX1 = 0.1;
        port.bridgeY1 = 0.2;
        port.bridge1Rotation = 0.3;
        port.bridgeX2 = 0.4;
        port.bridgeY2 = 0.5;
        port.bridge2Rotation = 0.6;

        ObjectNode json = port.toJson();
        assertTrue(json.has("portStructure"), "JSON should have portStructure node.");
        JsonNode portStructure = json.get("portStructure");

        assertEquals(1.0, portStructure.get("port").get("x").asDouble(), 0.001);
        assertEquals(0.1, portStructure.get("bridge1").get("x").asDouble(), 0.001);
        assertEquals(0.4, portStructure.get("bridge2").get("x").asDouble(), 0.001);

        assertFalse(portStructure.has("settlementPosition1Id"));
        assertFalse(portStructure.has("settlementPosition2Id"));
    }

    @Test
    void calculatePositionWhenSettlementsAreNullShouldNotThrowExceptionAndCoordinatesRemainZero() {
        port.calculatePosition();
        assertArrayEquals(new double[]{0.0, 0.0}, port.getCoordinates(), "Coordinates should be default [0,0] when settlements are null.");
        assertEquals(0.0, port.portRotation);
        assertEquals(0.0, port.bridgeX1);
        assertEquals(0.0, port.bridgeY1);
        assertEquals(0.0, port.bridge1Rotation);
        assertEquals(0.0, port.bridgeX2);
        assertEquals(0.0, port.bridgeY2);
        assertEquals(0.0, port.bridge2Rotation);
    }

    @Test
    void calculatePositionWhenOneSettlementIsNullShouldNotThrowExceptionAndCoordinatesRemainZero() {
        port.setAssociatedSettlements(mockSettlement1, null);
        port.calculatePosition();
        assertArrayEquals(new double[]{0.0, 0.0}, port.getCoordinates(), "Coordinates should be default [0,0] when one settlement is null.");
    }

    @Test
    void calculatePositionWithValidSettlementsShouldCalculatePositions() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{20.0, 0.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();


        assertEquals(10.0, port.portCenterX, 0.001, "PortCenterX is incorrect.");
        assertEquals(10.0, port.portCenterY, 0.001, "PortCenterY is incorrect.");
        assertEquals(0.0, port.portRotation, 0.001, "PortRotation is incorrect.");

        assertEquals(5.0, port.bridgeX1, 0.001, "Bridge1X is incorrect.");
        assertEquals(5.0, port.bridgeY1, 0.001, "Bridge1Y is incorrect.");
        assertEquals(Math.atan2(10,10), port.bridge1Rotation, 0.001, "Bridge1Rotation is incorrect.");

        assertEquals(15.0, port.bridgeX2, 0.001, "Bridge2X is incorrect.");
        assertEquals(5.0, port.bridgeY2, 0.001, "Bridge2Y is incorrect.");
        assertEquals(Math.atan2(10, -10), port.bridge2Rotation, 0.001, "Bridge2Rotation is incorrect.");

        assertArrayEquals(new double[]{10.0, 10.0}, port.getCoordinates(), 0.001, "getCoordinates returned incorrect values.");
    }

    @Test
    void calculatePositionWithVerticalSettlementsShouldCalculatePositions() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{0.0, 20.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();


        assertEquals(-10.0, port.portCenterX, 0.001, "PortCenterX is incorrect.");
        assertEquals(10.0, port.portCenterY, 0.001, "PortCenterY is incorrect.");
        assertEquals(Math.PI / 2, port.portRotation, 0.001, "PortRotation is incorrect.");

        assertEquals(-5.0, port.bridgeX1, 0.001);
        assertEquals(5.0, port.bridgeY1, 0.001);
        assertEquals(Math.atan2(10, -10), port.bridge1Rotation, 0.001);

        assertEquals(-5.0, port.bridgeX2, 0.001);
        assertEquals(15.0, port.bridgeY2, 0.001);
        assertEquals(Math.atan2(-10, -10), port.bridge2Rotation, 0.001);
    }

    @Test
    void calculatePositionNormalVectorFlipCheck() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{-20.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{0.0, -20.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();


        double expectedPortX = -10.0 - (10.0 / Math.sqrt(2));
        double expectedPortY = -10.0 - (10.0 / Math.sqrt(2));

        assertEquals(expectedPortX, port.portCenterX, 0.001, "PortCenterX after normal flip is incorrect.");
        assertEquals(expectedPortY, port.portCenterY, 0.001, "PortCenterY after normal flip is incorrect.");
        assertEquals(Math.atan2(-20, 20), port.portRotation, 0.001, "PortRotation is incorrect.");
    }

    @Test
    void calculatePositionWithValidSettlementsEnsuresUnitNormalCalculated() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{0.0, 0.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{20.0, 0.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portCenterX, 0.001, "PortCenterX indicates unit normal was used for offset.");
        assertEquals(10.0, port.portCenterY, 0.001, "PortCenterY indicates unit normal was used for offset.");
        assertArrayEquals(new double[]{10.0, 10.0}, port.getCoordinates(), 0.001);
    }


    @Test
    void calculatePositionWhenSettlementsAreAtSameLocationShouldPlacePortAtMidpoint() {
        when(mockSettlement1.getCoordinates()).thenReturn(new double[]{10.0, 15.0});
        when(mockSettlement2.getCoordinates()).thenReturn(new double[]{10.0, 15.0});

        port.setAssociatedSettlements(mockSettlement1, mockSettlement2);
        port.calculatePosition();

        assertEquals(10.0, port.portCenterX, 0.001, "PortCenterX should be midX as unitNormalX is 0.");
        assertEquals(15.0, port.portCenterY, 0.001, "PortCenterY should be midY as unitNormalY is 0.");
        assertArrayEquals(new double[]{10.0, 15.0}, port.getCoordinates(), 0.001, "Port should be at the common settlement location.");

        assertEquals(0.0, port.portRotation, 0.001, "PortRotation should be 0 for identical points.");
        assertEquals(10.0, port.bridgeX1, 0.001);
        assertEquals(15.0, port.bridgeY1, 0.001);
        assertEquals(0.0, port.bridge1Rotation, 0.001);
    }

    @Test
    void toJsonWhenSettlement1IsNullShouldNotIncludeIds() {
        when(mockSettlement2.getId()).thenReturn(2);
        port.setAssociatedSettlements(null, mockSettlement2);

        port.portCenterX = 1.0; port.portCenterY = 2.0; port.portRotation = 0.1;
        port.bridgeX1 = 0.11; port.bridgeY1 = 0.22; port.bridge1Rotation = 0.33;
        port.bridgeX2 = 0.44; port.bridgeY2 = 0.55; port.bridge2Rotation = 0.66;

        ObjectNode json = port.toJson();
        JsonNode portStructure = json.get("portStructure");

        assertNotNull(portStructure, "PortStructure node should always exist.");
        assertFalse(portStructure.has("settlementPosition1Id"), "Should NOT have settlementPosition1Id when s1 is null.");
        assertFalse(portStructure.has("settlementPosition2Id"), "Should NOT have settlementPosition2Id when s1 is null (condition is s1 && s2).");

        assertTrue(portStructure.has("port"));
        assertEquals(1.0, portStructure.get("port").get("x").asDouble(), 0.001);
        assertTrue(portStructure.has("bridge1"));
        assertEquals(0.11, portStructure.get("bridge1").get("x").asDouble(), 0.001);
    }

    @Test
    void toJsonWhenSettlement2IsNullShouldNotIncludeIds() {
        when(mockSettlement1.getId()).thenReturn(1);
        port.setAssociatedSettlements(mockSettlement1, null);

        port.portCenterX = 3.0; port.portCenterY = 4.0; port.portRotation = 0.2;
        port.bridgeX1 = 0.11; port.bridgeY1 = 0.22; port.bridge1Rotation = 0.33;
        port.bridgeX2 = 0.44; port.bridgeY2 = 0.55; port.bridge2Rotation = 0.66;

        ObjectNode json = port.toJson();
        JsonNode portStructure = json.get("portStructure");

        assertNotNull(portStructure, "PortStructure node should always exist.");
        assertFalse(portStructure.has("settlementPosition1Id"), "Should NOT have settlementPosition1Id when s2 is null (condition is s1 && s2).");
        assertFalse(portStructure.has("settlementPosition2Id"), "Should NOT have settlementPosition2Id when s2 is null.");

        assertTrue(portStructure.has("port"));
        assertEquals(3.0, portStructure.get("port").get("x").asDouble(), 0.001);
        assertTrue(portStructure.has("bridge2"));
        assertEquals(0.44, portStructure.get("bridge2").get("x").asDouble(), 0.001);
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