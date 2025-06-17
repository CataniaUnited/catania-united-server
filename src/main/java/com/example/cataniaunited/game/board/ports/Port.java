package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.BuildingSite;
import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.board.Transform;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    protected BuildingSite buildingSite1;
    protected BuildingSite buildingSite2;
    private static final double PORT_DISTANCE = 10.0;
    protected Transform portStructureTransform = Transform.ORIGIN;

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
     * Determines if a proposed trade is valid for this port, using Maps.
     *
     * @param offeredResources A map of resource types and quantities the player is offering.
     * @param desiredResources A map of resource types and quantities the player wishes to receive.
     * @return {@code true} if the trade is valid according to all rules, {@code false} otherwise.
     */
    public boolean canTrade(Map<TileType, Integer> offeredResources, Map<TileType, Integer> desiredResources) {
        // 1. Basic trade ratios (includes check for empty maps)
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
     * Abstract method for subclasses to validate trade rules specific to that port type.
     *
     * @param offeredResources A map of resource types and quantities being offered.
     * @param desiredResources A map of resource types and quantities being desired.
     * @return {@code true} if the port-specific rules are satisfied, {@code false} otherwise.
     */
    public abstract boolean arePortSpecificRulesSatisfied(Map<TileType, Integer> offeredResources, Map<TileType, Integer> desiredResources);

    /**
     * Checks if the fundamental trade ratios are invalid using maps.
     */
    protected boolean tradeRatioIsInvalid(Map<TileType, Integer> offeredResources, Map<TileType, Integer> desiredResources) {
        if (Util.isEmpty(offeredResources) || Util.isEmpty(desiredResources)) {
            return true; // Invalid if either map is empty
        }

        // Calculate total number of resources offered and desired
        int totalOffered = offeredResources.values().stream().mapToInt(Integer::intValue).sum();
        int totalDesired = desiredResources.values().stream().mapToInt(Integer::intValue).sum();

        if (totalOffered % this.inputResourceAmount != 0) {
            return true; // Total offered must be a multiple of the port's input amount
        }
        // Expected number of desired items must match the actual number of desired items
        return (totalOffered / this.inputResourceAmount) != totalDesired;
    }

    /**
     * Checks if any desired resource types are also being offered using map keys.
     */
    protected boolean isNotTradingForOfferedResources(Map<TileType, Integer> offeredResources, Map<TileType, Integer> desiredResources) {
        if (Util.isEmpty(offeredResources) || Util.isEmpty(desiredResources)) {
            return false; // An empty trade is invalid
        }
        // Use Collections.disjoint to check for any overlap in the keys (resource types).
        // It returns true if there are NO common elements, which is what we want.
        return Collections.disjoint(offeredResources.keySet(), desiredResources.keySet());
    }

    public void setAssociatedBuildingSites(BuildingSite s1, BuildingSite s2) {
        this.buildingSite1 = s1;
        this.buildingSite2 = s2;
    }

    public void calculatePosition() {
        if (buildingSite1 == null || buildingSite2 == null) {
            return;
        }

        double[] sp1CoordsArray = buildingSite1.getCoordinates();
        double[] sp2CoordsArray = buildingSite2.getCoordinates();

        double x1 = sp1CoordsArray[0];
        double y1 = sp1CoordsArray[1];
        double x2 = sp2CoordsArray[0];
        double y2 = sp2CoordsArray[1];

        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;

        double coastVecX = x2 - x1;
        double coastVecY = y2 - y1;

        double calculatedPortRotation = Math.atan2(coastVecY, coastVecX);

        double normalX = -coastVecY;
        double normalY = coastVecX;

        if ((normalX * midX + normalY * midY) < 0) {
            normalX = -normalX;
            normalY = -normalY;
        }

        double lengthNormal = Math.sqrt(normalX * normalX + normalY * normalY);
        double unitNormalX = 0;
        double unitNormalY = 0;
        if (lengthNormal > 0.0001) {
            unitNormalX = normalX / lengthNormal;
            unitNormalY = normalY / lengthNormal;
        }

        double currentPortCenterX = midX + unitNormalX * PORT_DISTANCE;
        double currentPortCenterY = midY + unitNormalY * PORT_DISTANCE;

        this.portStructureTransform = new Transform(currentPortCenterX, currentPortCenterY, calculatedPortRotation);
    }

    @Override
    public double[] getCoordinates() {
        return Objects.requireNonNullElse(this.portStructureTransform, Transform.ORIGIN).getCoordinatesArray();
    }

    @Override
    public ObjectNode toJson() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        ArrayNode coordinates1Node = factory.arrayNode(2);
        ArrayNode coordinates2Node = factory.arrayNode(2);
        node.put("inputResourceAmount", this.inputResourceAmount);

        ObjectNode portNode = node.putObject("portVisuals");

        portNode.set("portTransform", this.portStructureTransform.toJson(factory));

        if (buildingSite1 != null && buildingSite2 != null) {
            portNode.put("settlementPosition1Id", buildingSite1.getId());
            portNode.put("settlementPosition2Id", buildingSite2.getId());

            for (double val : this.buildingSite1.getCoordinates()) {
                coordinates1Node.add(val);
            }

            portNode.set("buildingSite1Position", coordinates1Node);


            for (double val : this.buildingSite2.getCoordinates()) {
                coordinates2Node.add(val);
            }

            portNode.set("buildingSite2Position", coordinates2Node);
        }

        return node;
    }

    public List<BuildingSite> getBuildingSites() {
        if (buildingSite1 == null || buildingSite2 == null) {
            return List.of();
        }
        return List.of(buildingSite1, buildingSite2);
    }
}