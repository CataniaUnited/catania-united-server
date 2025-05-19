package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public abstract class Port implements Placable {
    protected final int inputResourceAmount;

    protected Port(int inputResourceAmount) {
        if (inputResourceAmount <= 0) {
            throw new IllegalArgumentException("Input resource amount must be positive.");
        }
        this.inputResourceAmount = inputResourceAmount;
    }

    public abstract boolean canTrade(List<TileType> offeredResources, List<TileType> desiredResources);


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


    protected boolean isTradingForOfferedResources(List<TileType> offeredResources, List<TileType> desiredResources) {
        if (Util.isEmpty(offeredResources) || Util.isEmpty(desiredResources)) {
            return false; // Cannot be trading for offered if lists are empty
        }
        for (TileType desired : desiredResources) {
            if (offeredResources.contains(desired)) {
                return true; // Attempting to acquire a resource type also being offered
            }
        }
        return false; // No trying to get a resource that's also offered
    }


    @Override
    public double[] getCoordinates() {
        return new double[2];
    }


    @Override
    public ObjectNode toJson() {
        return null;
    }
}