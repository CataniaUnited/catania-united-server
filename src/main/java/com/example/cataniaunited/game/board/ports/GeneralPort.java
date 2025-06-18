package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Represents a general trading port, allowing a 3:1 exchange of any resource type
 * for any other resource type.
 * For example, a player can trade 3 Wood for 1 Sheep, or 3 Clay for 1 Ore.
 */
public class GeneralPort extends Port {

    /**
     * Constructs a new GeneralPort with a 3:1 trade ratio.
     */
    public GeneralPort() {
        super(3); // A general Port is 3:1
    }

    /**
     * Validates port-specific rules for a General Port using maps.
     * This means checking if each offered resource type is provided in a quantity
     * that is a multiple of this port's trade ratio (e.g., 3 Wood, or 6 Sheep).
     *
     * @param offeredResources A map of resource types and quantities being offered.
     * @param desiredResources A map of resource types and quantities being desired (not used by this check).
     * @return {@code true} if all offered resource quantities are valid multiples, {@code false} otherwise.
     */
    @Override
    public boolean arePortSpecificRulesSatisfied(Map<TileType, Integer> offeredResources, Map<TileType, Integer> desiredResources) {
        // For each type of resource offered, check if its count is a multiple of the port's inputResourceAmount
        for (Integer offeredAmount : offeredResources.values()) {
            if (offeredAmount % this.inputResourceAmount != 0) {
                // This specific resource type is not bundled correctly (e.g., offering 2 WOOD at a 3:1 port)
                return false;
            }
        }
        // All offered resource types are in valid bundle sizes
        return true;
    }

    /**
     * Converts this GeneralPort's state to a JSON representation.
     * Includes abstract port visual data and specific type information.
     *
     * @return An {@link ObjectNode} representing the GeneralPort.
     */
    @Override
    public ObjectNode toJson() {
        ObjectNode node = super.toJson();
        node.put("portType", "GeneralPort");
        return node;
    }
}