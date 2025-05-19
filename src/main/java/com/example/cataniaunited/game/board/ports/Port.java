package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.Util;
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

    /**
     * Gets the 2D coordinates of this port on the game board.

     * @return A double array `[x, y]`;
     */
    @Override
    public double[] getCoordinates() {
        return new double[2];
    }

    /**
     * Converts this port's state to a JSON representation.
     *
     * @return An {@link ObjectNode} representing the port.
     */
    @Override
    public ObjectNode toJson() {
        return null;
    }
}