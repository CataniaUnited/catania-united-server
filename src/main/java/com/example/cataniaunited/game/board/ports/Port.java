package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.board.SettlementPosition;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Abstract class representing a trading port on the game board.
 * Ports allow players to exchange resources at specific ratios.
 * Implementing {@link Placable} to ensure Methods to extract the necessary Data to the Frontend
 */
public abstract class Port implements Placable {
    /**
     * The number of identical input resources required to receive one desired resource (N in an N:1 trade).
     */
    protected final int inputResourceAmount;
    protected SettlementPosition settlementPosition1;
    protected SettlementPosition settlementPosition2;
    private static final double PORT_DISTANCE = 10.0;

    // fixme introduce data structure for the transform / placement information (x,y,rot)
    protected double portCenterX;
    protected double portCenterY;
    protected double portRotation;

    protected double bridgeX1;
    protected double bridgeY1;
    protected double bridge1Rotation;

    protected double bridgeX2;
    protected double bridgeY2;
    protected double bridge2Rotation;

    /**
     * Constructs a Port with a specified trade-in ratio.
     *
     * @param inputResourceAmount The number of input resources required for one output resource (e.g., 3 for a 3:1 port).
     * @throws IllegalArgumentException if inputResourceAmount is not positive.
     */
    protected Port(int inputResourceAmount) {
        if (inputResourceAmount <= 0) {
            throw new IllegalArgumentException("Input resource amount must be positive.");
        }
        this.inputResourceAmount = inputResourceAmount;
    }

    // fixme add generic trade checks to this class (1st and 3rd in the javadocs)
    /**
     * Determines if a proposed trade is valid according to this port's specific rules.
     *
     * @param offeredResources A list of {@link TileType} representing the resources the player is offering.
     * @param desiredResources A list of {@link TileType} representing the resources the player wishes to receive.
     * @return {@code true} if the trade is valid according to this port's rules, {@code false} otherwise.
     */
    public abstract boolean canTrade(List<TileType> offeredResources, List<TileType> desiredResources);

    /**
     * Checks if the fundamental trade ratios of a proposed trade are invalid.
     * This includes checks for:
     * <ul>
     *     <li>Empty offered or desired resource lists.</li>
     *     <li>The total number of offered resources not being a multiple of {@link #inputResourceAmount}.</li>
     *     <li>The number of desired resources not matching the expected output based on the offered amount and ratio.</li>
     * </ul>
     *
     * @param offeredResources The list of resources being offered.
     * @param desiredResources The list of resources being desired.
     * @return {@code true} if any of the basic ratio rules are violated, {@code false} otherwise.
     */
    protected boolean tradeRatioIsInvalid(List<TileType> offeredResources, List<TileType> desiredResources) {
        if (Util.isEmpty(offeredResources) || Util.isEmpty(desiredResources)) {
            return true; // Invalid if either list is empty
        }
        if (offeredResources.size() % this.inputResourceAmount != 0) {
            return true; // Total offered must be a multiple of the port's input amount (else invalid)
        }
        // Expected number of desired items based on offered items and ratio (must match, else invalid)
        return (offeredResources.size() / this.inputResourceAmount) != desiredResources.size();
    }

    // fixme why are trading checks part of the port? and why is the description focused on players not part of the impl
    //  extract trading logic into trades and dont focus on players if they are not part of the concepts here
    //  eg, "checks if the offered and desired resources share the same type which makes a trade invalid"
    //  avoid negation in checks for readability, generally improve the naming of trading checks
    /**
     * Checks if the player is attempting to acquire any resource types that they are also offering.
     * A player cannot trade a resource for the same type of resource at a port.
     *
     * @param offeredResources The list of resources being offered.
     * @param desiredResources The list of resources being desired.
     * @return {@code true} if the player is not trying to obtain a resource type they are also offering,
     * {@code false} otherwise.
     */
    protected boolean isNotTradingForOfferedResources(List<TileType> offeredResources, List<TileType> desiredResources) {
        if (Util.isEmpty(offeredResources) || Util.isEmpty(desiredResources)) {
            return false; // An empty trade is invalid
        }
        for (TileType desired : desiredResources) {
            if (offeredResources.contains(desired)) {
                return false; // Attempting to acquire a resource type also being offered
            }
        }
        return true; // No trying to get a resource that's also offered
    }

    public void setAssociatedSettlements(SettlementPosition s1, SettlementPosition s2) {
        this.settlementPosition1 = s1;
        this.settlementPosition2 = s2;
    }

    public void calculatePosition() {
        if (settlementPosition1 == null || settlementPosition2 == null) {
            return;
        }

        double[] sp1Coords = settlementPosition1.getCoordinates();
        double[] settlementPosition2Coords = settlementPosition2.getCoordinates();
        // fixme reuse custom transform data types here
        //  consider https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/geometry/euclidean/twod/Vector2D.html
        //  for your computations
        double x1 = sp1Coords[0];
        double y1 = sp1Coords[1];
        double x2 = settlementPosition2Coords[0];
        double y2 = settlementPosition2Coords[1];

        // Step 1: Midpoint
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;

        // Step 2: Coastline Vector
        double coastVecX = x2 - x1;
        double coastVecY = y2 - y1;

        // Step 3: Port Rotation
        this.portRotation = Math.atan2(coastVecY, coastVecX);

        // Step 4 & 5: Outward Normal Vector
        double normalX = -coastVecY; // Initial perpendicular
        double normalY = coastVecX;

        if ((normalX * midX + normalY * midY) < 0) { // Dot product
            normalX = -normalX; // Flip if pointing inward
            normalY = -normalY;
        }

        double lengthNormal = Math.sqrt(normalX * normalX + normalY * normalY);
        double unitNormalX = 0;
        double unitNormalY = 0;
        if (lengthNormal > 0.0001) {
            unitNormalX = normalX / lengthNormal;
            unitNormalY = normalY / lengthNormal;
        }

        // Step 6: Port Structure Position
        this.portCenterX = midX + unitNormalX * PORT_DISTANCE;
        this.portCenterY = midY + unitNormalY * PORT_DISTANCE;

        // Step 7: Bridge 1 (from settlementPosition1 to portCenter)
        double vecSp1ToPortX = this.portCenterX - x1;
        double vecSp1ToPortY = this.portCenterY - y1;
        this.bridge1Rotation = Math.atan2(vecSp1ToPortY, vecSp1ToPortX);
        this.bridgeX1 = (x1 + this.portCenterX) / 2;
        this.bridgeY1 = (y1 + this.portCenterY) / 2;

        // Step 7: Bridge 2 (from settlementPosition2 to portCenter)
        double vecSettlementPosition2ToPortX = this.portCenterX - x2;
        double vecSettlementPosition2ToPortY = this.portCenterY - y2;
        this.bridge2Rotation = Math.atan2(vecSettlementPosition2ToPortY, vecSettlementPosition2ToPortX);
        this.bridgeX2 = (x2 + this.portCenterX) / 2;
        this.bridgeY2 = (y2 + this.portCenterY) / 2;
    }

    /**
     * Gets the 2D coordinates of this port on the game board.
     *
     * @return A double array `[x, y]`;
     */
    @Override
    public double[] getCoordinates() {
        return new double[]{portCenterX, portCenterY};
    }

    // fixme the port game object shouldnt know about how to serialize itself
    /**
     * Converts this port's state to a JSON representation.
     * Needs to be called by subClasses to add specific Information
     *
     * @return An {@link ObjectNode} representing the port.
     */
    @Override
    public ObjectNode toJson() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("inputResourceAmount", this.inputResourceAmount); // e.g. 3 for 3:1


        ObjectNode portNode = node.putObject("portStructure");
        portNode.putObject("port")
                .put("x", this.portCenterX)
                .put("y", this.portCenterY)
                .put("rotation", this.portRotation);
        portNode.putObject("bridge1")
                .put("x", this.bridgeX1)
                .put("y", this.bridgeY1)
                .put("rotation", this.bridge1Rotation);
        portNode.putObject("bridge2")
                .put("x", this.bridgeX2)
                .put("y", this.bridgeY2)
                .put("rotation", this.bridge2Rotation);

        
        if (settlementPosition1 != null && settlementPosition2 != null) {
            portNode.put("settlementPosition1Id", settlementPosition1.getId());
            portNode.put("settlementPosition2Id", settlementPosition2.getId());
        }
        
        return node;
    }

    public List<SettlementPosition> getSettlementPositions() {
        return List.of(settlementPosition1, settlementPosition2);
    }
}