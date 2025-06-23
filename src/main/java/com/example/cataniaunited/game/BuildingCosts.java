package com.example.cataniaunited.game;


import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.HashMap;
import java.util.Map;
public class BuildingCosts {
    public static Map<String, Map<TileType, Integer>> getAllCosts() {
        Map<String, Map<TileType, Integer>> costs = new HashMap<>();

        costs.put("ROAD", Map.of(
                TileType.CLAY, 1,
                TileType.WOOD, 1
        ));

        costs.put("SETTLEMENT", Map.of(
                TileType.CLAY, 1,
                TileType.WOOD, 1,
                TileType.SHEEP, 1,
                TileType.WHEAT, 1
        ));

        costs.put("CITY", Map.of(
                TileType.WHEAT, 2,
                TileType.ORE, 3
        ));

        costs.put("DEVELOPMENT_CARD", Map.of(
                TileType.SHEEP, 1,
                TileType.WHEAT, 1,
                TileType.ORE, 1
        ));

        return costs;
    }

    public static Map<TileType, Integer> getCost(String type) {
        return getAllCosts().getOrDefault(type.toUpperCase(), Map.of());
    }
}
