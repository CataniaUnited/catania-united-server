package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

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
     * Cannot be null or {@link TileType#WASTE}.
     * @throws IllegalArgumentException if tradeAbleResource is null or {@link TileType#WASTE}.
     */
    public SpecificResourcePort(TileType tradeAbleResource) {
        super(2); // Resource Ports are 2:1
        if (tradeAbleResource == null || tradeAbleResource == TileType.WASTE) {
            throw new IllegalArgumentException("Specific port must trade a valid resource type.");
        }
        this.tradeAbleResource = tradeAbleResource;
    }

    /**
     * Validates port-specific rules for a Specific Resource Port.
     * For this type of port, it means checking if all offered resources
     * are of the port's specific {@link #tradeAbleResource} type.
     *
     * @param offeredResources A map of resource types and quantities being offered.
     * @param desiredResources A map of resource types and quantities being desired (not used by this check).
     * @return {@code true} if the offered resources consist of a single, correct type, {@code false} otherwise.
     */
    @Override
    public boolean arePortSpecificRulesSatisfied(Map<TileType, Integer> offeredResources, Map<TileType, Integer> desiredResources) {
        // For a specific port, the trade is only valid if:
        // 1. Exactly one type of resource is being offered.
        // 2. That one type is the specific resource this port accepts.
        return offeredResources.size() == 1 && offeredResources.containsKey(this.tradeAbleResource);
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
        return node;
    }
}