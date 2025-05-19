package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.List;


public class SpecificResourcePort extends Port {

    final TileType tradeAbleResource;

    public SpecificResourcePort(TileType tradeAbleResource) {
        super(2); // Resource Ports are 2:1
        if (tradeAbleResource == null || tradeAbleResource == TileType.WASTE) {
            throw new IllegalArgumentException("Specific port must trade a valid resource type.");
        }
        this.tradeAbleResource = tradeAbleResource;
    }

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
        return !isTradingForOfferedResources(offeredResources, desiredResources);
    }

    public TileType getTradeAbleResource() {
        return tradeAbleResource;
    }
}