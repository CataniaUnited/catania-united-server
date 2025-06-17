package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;

import java.util.Map;

public class TestBuilding extends Building {
    protected TestBuilding(Player player, PlayerColor color) throws GameException {
        super(player, color);
    }

    @Override
    public int getResourceDistributionAmount() {
        return 1;
    }

    @Override
    public Map<TileType, Integer> getRequiredResources() {
        return Map.of();
    }

    @Override
    public int getBuildLimit() {
        return 1;
    }
}