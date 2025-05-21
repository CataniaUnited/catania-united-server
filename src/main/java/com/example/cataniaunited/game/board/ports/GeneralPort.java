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
     * Determines if a proposed trade is valid for this general port.
     * Validates:
     * <ol>
     *     <li>Basic trade ratios (total amounts offered vs. desired).</li>
     *     <li>Correct bundling of each offered resource type (e.g., must offer 3 of Wood, not 2 Wood and 1 Sheep for a bundle).</li>
     *     <li>That the player is not trying to trade for resources they are also offering.</li>
     * </ol>
     *
     * @param offeredResources A list of {@link TileType} representing the resources the player is offering.
     * @param desiredResources A list of {@link TileType} representing the resources the player wishes to receive.
     * @return {@code true} if the trade is valid, {@code false} otherwise.
     */
    @Override
    public boolean canTrade(List<TileType> offeredResources, List<TileType> desiredResources) {
        // trade ratio must be valid
        if (tradeRatioIsInvalid(offeredResources, desiredResources)) {
            return false;
        }

        // For generic ports, each offered resource type must be bundled correctly.
        if (!offeredResourcesComeInValidBundles(offeredResources)) {
            return false;
        }

        // Check if player is trying to trade for resources they are offering
        return isNotTradingForOfferedResources(offeredResources, desiredResources);
    }

    /**
     * Checks if each type of offered resource is provided in quantities that are
     * multiples of this port's {@link #inputResourceAmount}.
     *
     * @param offeredResources The list of resources being offered.
     * @return {@code true} if all offered resource types are offered in a correct distribution.
     */
    private boolean offeredResourcesComeInValidBundles(List<TileType> offeredResources) {
        Map<TileType, Long> offeredResourceMap = offeredResources.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (Map.Entry<TileType, Long> entry : offeredResourceMap.entrySet()) {
            long countOfEntryType = entry.getValue();
            if (countOfEntryType % this.inputResourceAmount != 0) {
                return false; // This specific resource type is not bundled correctly (i.e. can't trade 1 sheep + 2 wheat for one Ore)
            }
        }

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