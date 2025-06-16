package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;

import java.util.Map;

/**
 * Represents a City building in the Catan game.
 * A City is an upgrade from a Settlement and provides more resources and victory points.
 */
public class City extends Building {

    /**
     * Constructs a new City.
     *
     * @param player The {@link Player} who owns this city.
     * @param color  The {@link PlayerColor} of this city.
     * @throws GameException if the player or color is invalid.
     */
    public City(Player player, PlayerColor color) throws GameException {
        super(player, color);
    }

    /**
     * Gets the amount of resources a City distributes when an adjacent tile produces.
     * A City distributes 2 resources.
     *
     * @return The resource distribution amount (2).
     */
    @Override
    public int getResourceDistributionAmount() {
        return 2;
    }

    /**
     * Gets the map of resource types and amounts required to upgrade a Settlement to a City.
     *
     * @return A map where keys are {@link TileType} (WHEAT, ORE) and values are their respective amounts.
     */
    @Override
    public Map<TileType, Integer> getRequiredResources() {
        return Map.of(
                TileType.WHEAT, 2,
                TileType.ORE, 3
        );
    }

    @Override
    public int getBuildLimit() {
        return 4;
    }
}