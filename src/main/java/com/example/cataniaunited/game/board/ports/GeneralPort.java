package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * Validates port-specific rules for a General Port.
     * For a general port, this means checking if each type of offered resource
     * is provided in quantities that are multiples of this port's {@link #inputResourceAmount}.
     * For example, if the port is 3:1, a player must offer 3 Wood or 3 Sheep, etc.,
     * not a mix like 2 Wood and 1 Sheep to make up a bundle of 3.
     *
     * @param offeredResources A list of {@link TileType} representing the resources the player is offering.
     * @param desiredResources A list of {@link TileType} representing the resources the player wishes to receive (not used by this specific check).
     * @return {@code true} if all offered resource types are bundled correctly according to the port's ratio, {@code false} otherwise.
     */
    @Override
    public boolean arePortSpecificRulesSatisfied(List<TileType> offeredResources, List<TileType> desiredResources) {
        // The offeredResourcesComeInValidBundles method already checks the core logic for GeneralPort.
        // It returns true if bundles are valid, false otherwise. This matches the expected output.
        return offeredResourcesComeInValidBundles(offeredResources);
    }

    /**
     * Checks if each type of offered resource is provided in quantities that are
     * multiples of this port's {@link #inputResourceAmount}.
     *
     * @param offeredResources The list of resources being offered.
     * @return {@code true} if all offered resource types are offered in a correct distribution (valid bundles).
     */
    private boolean offeredResourcesComeInValidBundles(List<TileType> offeredResources) {
        // Group offered resources by type and count them
        Map<TileType, Long> offeredResourceMap = offeredResources.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // For each type of resource offered, check if its count is a multiple of the port's inputResourceAmount
        for (Map.Entry<TileType, Long> entry : offeredResourceMap.entrySet()) {
            long countOfEntryType = entry.getValue();
            if (countOfEntryType % this.inputResourceAmount != 0) {
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