package com.example.cataniaunited.game;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.Map;

/**
 * Interface for game elements that can be built by players,
 * such as roads, settlements, and cities.
 * Defines a method to get the resources required for building.
 */
public interface Buildable {
    /**
     * Gets the map of resource types and amounts required to build this item.
     *
     * @return A map where keys are {@link TileType} representing the resource,
     *         and values are integers representing the amount required.
     */
    Map<TileType, Integer> getRequiredResources();

    int getBuildLimit();
}