package com.example.cataniaunited.game.board.ports;


import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.List;

class TestablePort extends Port {
    protected TestablePort(int inputResourceAmount) {
        super(inputResourceAmount);
    }

    @Override
    public boolean canTrade(List<TileType> offeredResources, List<TileType> desiredResources) {
        return false;
    }

    @Override
    public boolean arePortSpecificRulesSatisfied(List<TileType> offeredResources, List<TileType> desiredResources) {
        return false;
    }
}
