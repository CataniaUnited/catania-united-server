package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Represents a specific resource trading port, allowing a 2:1 exchange
 * of a particular resource type for any other resource type.
 * For example, a 2:1 Wood port allows trading 2 Wood for 1 of any other resource.
 */
public class SpecificResourcePort extends Port {
    /**
     * The specific {@link TileType} that this port accepts for trade.
     */
    final TileType tradeAbleResource;

    /**
     * Constructs a new SpecificResourcePort.
     *
     * @param tradeAbleResource The specific {@link TileType} this port trades (e.g., WOOD, SHEEP).
     *                          Cannot be null or {@link TileType#WASTE}.
     * @throws IllegalArgumentException if tradeAbleResource is null or {@link TileType#WASTE}..
     */
    public SpecificResourcePort(TileType tradeAbleResource) {
        super(2); // Resource Ports are 2:1
        if (tradeAbleResource == null || tradeAbleResource == TileType.WASTE) {
            throw new IllegalArgumentException("Specific port must trade a valid resource type.");
        }
        this.tradeAbleResource = tradeAbleResource;
    }

    /**
     * Determines if a proposed trade is valid for this specific resource port.
     * Validates:
     * <ol>
     *     <li>Basic trade ratios (total amounts offered vs. desired).</li>
     *     <li>That all offered resources are of the port's specific {@link #tradeAbleResource} type.</li>
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

        // For specific ports, all offered resources must be of the tradeAble type.
        for (TileType offered : offeredResources) {
            if (offered != this.tradeAbleResource) {
                return false; // Wrong type offered
            }
        }

        // Check if player is trying to trade for resources they are offering
        return isNotTradingForOfferedResources(offeredResources, desiredResources);
    }

    /**
     * Gets the specific {@link TileType} that this port specializes in trading.
     *
     * @return The {@link TileType} this port accepts.
     */
    public TileType getTradeAbleResource() {
        return tradeAbleResource;
    }

    /**
     * Converts this SpecificResourcePort's state to a JSON representation.
     * Includes abstract port visual data and specific type information.
     *
     * @return An {@link ObjectNode} representing the SpecificResourcePort.
     */
    @Override
    public ObjectNode toJson() {
        ObjectNode node = super.toJson();
        node.put("portType", "SpecificResourcePort");
        node.put("resource", this.tradeAbleResource.name());
        node.put("ratio", this.inputResourceAmount + ":1 " + this.tradeAbleResource.name());
        return node;
    }
}