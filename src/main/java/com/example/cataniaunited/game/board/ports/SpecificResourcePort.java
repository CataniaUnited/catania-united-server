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
     *                          Cannot be null or {@link TileType#DESERT}.
     * @throws IllegalArgumentException if tradeAbleResource is null or {@link TileType#DESERT}.
     */
    public SpecificResourcePort(TileType tradeAbleResource) {
        super(2); // Resource Ports are 2:1
        if (tradeAbleResource == null || tradeAbleResource == TileType.DESERT) {
            throw new IllegalArgumentException("Specific port must trade a valid resource type.");
        }
        this.tradeAbleResource = tradeAbleResource;
    }

    /**
     * Validates port-specific rules for a Specific Resource Port.
     * For this type of port, it means checking if all offered resources
     * are of the port's specific {@link #tradeAbleResource} type.
     *
     * @param offeredResources A list of {@link TileType} representing the resources the player is offering.
     * @param desiredResources A list of {@link TileType} representing the resources the player wishes to receive (not used by this specific check).
     * @return {@code true} if all offered resources match the port's {@code tradeAbleResource}, {@code false} otherwise.
     */
    @Override
    public boolean arePortSpecificRulesSatisfied(List<TileType> offeredResources, List<TileType> desiredResources) {
        // For specific ports, all offered resources must be of the tradeAbleResource type.
        for (TileType offered : offeredResources) {
            if (offered != this.tradeAbleResource) {
                return false; // Found an offered resource that is not the port's specific tradeable type
            }
        }
        // All offered resources are of the correct type
        return true;
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