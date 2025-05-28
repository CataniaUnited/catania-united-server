package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.board.SettlementPosition;
import com.example.cataniaunited.game.board.Transform;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;

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
    protected Transform portStructureTransform = Transform.ORIGIN;

    protected Transform bridge1Transform = Transform.ORIGIN;
    protected Transform bridge2Transform = Transform.ORIGIN;

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

    /**
     * Determines if a proposed trade is valid for this port, considering both general
     * trading rules and rules specific to the port type (e.g., general vs. specific resource).
     * <p>
     * This method validates:
     * <ol>
     *     <li>Basic trade ratios:</li>
     *     <li>Port-specific rules: Handled by the {@link #arePortSpecificRulesSatisfied(List, List)} method,
     *         which varies between {@link GeneralPort} and {@link SpecificResourcePort}. This includes checks like:
     *     </li>
     *     <li>No self-trading: The player is not trying to trade for resource types they are also offering.</li>
     * </ol>
     *
     * @param offeredResources A list of {@link TileType} representing the resources the player is offering.
     * @param desiredResources A list of {@link TileType} representing the resources the player wishes to receive.
     * @return {@code true} if the trade is valid according to all rules, {@code false} otherwise.
     */
    public boolean canTrade(List<TileType> offeredResources, List<TileType> desiredResources) {
        // 1. Basic trade ratios (includes check for empty lists)
        if (tradeRatioIsInvalid(offeredResources, desiredResources)) {
            return false;
        }

        // 2. Port-specific rules
        if (!arePortSpecificRulesSatisfied(offeredResources, desiredResources)) {
            return false;
        }

        // 3. No self-trading (offering and desiring the same resource type)
        return isNotTradingForOfferedResources(offeredResources, desiredResources);
    }

    /**
     * Abstract method to be implemented by subclasses ({@link GeneralPort}, {@link SpecificResourcePort})
     * to validate trade rules that are specific to that type of port.
     * <br>
     * This method is called by {@link #canTrade(List, List)} after general ratio checks
     * but before checking for self-trading.
     *
     * @param offeredResources A list of {@link TileType} representing the resources the player is offering.
     * @param desiredResources A list of {@link TileType} representing the resources the player wishes to receive
     *                         (often not directly used by this specific check but provided for consistency).
     * @return {@code true} if the port-specific rules are satisfied for the given trade, {@code false} otherwise.
     */
    public abstract boolean arePortSpecificRulesSatisfied(List<TileType> offeredResources, List<TileType> desiredResources);

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

    /**
     * Checks if any of the desired resource types are also present in the offered resources.
     * A trade is generally invalid if an entity attempts to exchange a resource for the same type of resource
     * through a port (e.g., offering Wood to receive Wood).
     *
     * @param offeredResources The list of resources being offered.
     * @param desiredResources The list of resources being desired.
     * @return {@code true} if there is no overlap between offered and desired resource types (i.e., not trading for offered resources),
     * {@code false} if an overlap exists (attempting to trade a resource for itself) or if either list is empty.
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

        // It's good practice to check if positions have valid coordinates.
        double[] sp1CoordsArray = settlementPosition1.getCoordinates();
        double[] sp2CoordsArray = settlementPosition2.getCoordinates();

        // --- Existing Coordinate Extraction ---
        double x1 = sp1CoordsArray[0];
        double y1 = sp1CoordsArray[1];
        double x2 = sp2CoordsArray[0];
        double y2 = sp2CoordsArray[1];

        // --- Step 1: Midpoint of the two settlement positions ---
        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;

        // --- Step 2: Vector representing the coastline segment between settlements ---
        double coastVecX = x2 - x1;
        double coastVecY = y2 - y1;

        // --- Step 3: Port Structure Rotation ---
        // The port structure itself (e.g., the dock building) aligns with the coastline.
        double calculatedPortRotation = Math.atan2(coastVecY, coastVecX);

        // --- Step 4 & 5: Outward Normal Vector ---
        // This vector points perpendicularly outwards from the coastline,
        // determining the direction in which the port structure extends from the coast.
        double normalX = -coastVecY; // Initial perpendicular vector (rotated 90 degrees counter-clockwise)
        double normalY = coastVecX;

        // Ensure the normal vector points "outward" from the general center of the board. (0, 0)
        if ((normalX * midX + normalY * midY) < 0) { // Dot product of normal with midpoint's position vector
            normalX = -normalX; // Flip normal if it points inward towards the origin
            normalY = -normalY;
        }

        // Normalize the outward normal vector
        double lengthNormal = Math.sqrt(normalX * normalX + normalY * normalY);
        double unitNormalX = 0;
        double unitNormalY = 0;
        if (lengthNormal > 0.0001) { // Avoid division by zero for very short (or zero length) coastlines
            unitNormalX = normalX / lengthNormal;
            unitNormalY = normalY / lengthNormal;
        }

        // --- Step 6: Port Structure Position (Center of the port building) ---
        // Position the port structure along the unit normal vector, at PORT_DISTANCE from the midpoint.
        double currentPortCenterX = midX + unitNormalX * PORT_DISTANCE;
        double currentPortCenterY = midY + unitNormalY * PORT_DISTANCE;

        // Assign the calculated transform to the port structure
        this.portStructureTransform = new Transform(currentPortCenterX, currentPortCenterY, calculatedPortRotation);

        // --- Step 7: Bridge 1 Transform (Connecting settlementPosition1 to port structure) ---
        // Vector from settlement 1 to the port structure's center
        double vecSp1ToPortX = currentPortCenterX - x1;
        double vecSp1ToPortY = currentPortCenterY - y1;
        // Rotation of bridge 1 aligns with this vector
        double calculatedBridge1Rotation = Math.atan2(vecSp1ToPortY, vecSp1ToPortX);
        // Midpoint of bridge 1
        double currentBridge1X = (x1 + currentPortCenterX) / 2.0;
        double currentBridge1Y = (y1 + currentPortCenterY) / 2.0;

        // Assign the calculated transform to bridge 1
        this.bridge1Transform = new Transform(currentBridge1X, currentBridge1Y, calculatedBridge1Rotation);

        // --- Step 8: Bridge 2 Transform (Connecting settlementPosition2 to port structure) ---
        // Vector from settlement 2 to the port structure's center
        double vecSp2ToPortX = currentPortCenterX - x2;
        double vecSp2ToPortY = currentPortCenterY - y2;
        // Rotation of bridge 2 aligns with this vector
        double calculatedBridge2Rotation = Math.atan2(vecSp2ToPortY, vecSp2ToPortX);
        // Midpoint of bridge 2
        double currentBridge2X = (x2 + currentPortCenterX) / 2.0;
        double currentBridge2Y = (y2 + currentPortCenterY) / 2.0;

        // Assign the calculated transform to bridge 2
        this.bridge2Transform = new Transform(currentBridge2X, currentBridge2Y, calculatedBridge2Rotation);
    }

    /**
     * Gets the 2D coordinates of this port on the game board.
     *
     * @return A double array `[x, y]`;
     */
    @Override
    public double[] getCoordinates() {
        return Objects.requireNonNullElse(this.portStructureTransform, Transform.ORIGIN).getCoordinatesArray();
    }

    /**
     * Converts this port's state to a JSON representation.
     * Needs to be called by subClasses to add specific Information
     *
     * @return An {@link ObjectNode} representing the port.
     */
    @Override
    public ObjectNode toJson() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("inputResourceAmount", this.inputResourceAmount); // e.g. 3 for 3:1

        // Create a parent node for all visual/transformational aspects of the port
        ObjectNode portNode = node.putObject("portVisuals");

        // Add the transform for the main port structure
        // The toJson method in the Transform record will handle its own serialization
        portNode.set("portTransform", this.portStructureTransform.toJson(factory));

        // Add the transform for the first bridge
        portNode.set("bridge1Transform", this.bridge1Transform.toJson(factory));

        // Add the transform for the second bridge
        portNode.set("bridge2Transform", this.bridge2Transform.toJson(factory));

        // Include IDs of associated settlement positions if they exist
        if (settlementPosition1 != null && settlementPosition2 != null) {
            portNode.put("settlementPosition1Id", settlementPosition1.getId());
            portNode.put("settlementPosition2Id", settlementPosition2.getId());
        }


        if (settlementPosition1 != null && settlementPosition2 != null) {
            portNode.put("settlementPosition1Id", settlementPosition1.getId());
            portNode.put("settlementPosition2Id", settlementPosition2.getId());
        }

        return node;
    }

    public List<SettlementPosition> getSettlementPositions() {
        if (settlementPosition1 == null || settlementPosition2 == null) {
            return List.of();
        }
        return List.of(settlementPosition1, settlementPosition2);
    }
}