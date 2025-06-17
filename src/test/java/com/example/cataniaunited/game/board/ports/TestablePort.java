package com.example.cataniaunited.game.board.ports;


import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.Map;

class TestablePort extends Port {
    protected TestablePort(int inputResourceAmount) {
        super(inputResourceAmount);
    }

    @Override
    public boolean arePortSpecificRulesSatisfied(Map<TileType, Integer> offeredResources, Map<TileType, Integer> desiredResources) {
        return false;
    }
}
