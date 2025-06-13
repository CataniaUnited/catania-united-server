package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;

import java.util.Map;

/**
 * Represents a Settlement building in the Catan game.
 * A Settlement provides resources from adjacent tiles and victory points.
 */
public class Settlement extends Building {
    /**
     * Constructs a new Settlement.
     *
     * @param player The {@link Player} who owns this settlement.
     * @param color  The {@link PlayerColor} of this settlement.
     * @throws GameException if the player or color is invalid.
     */
    public Settlement(Player player, PlayerColor color) throws GameException {
        super(player, color);
    }

    /**
     * Gets the amount of resources a Settlement distributes when an adjacent tile produces.
     * A Settlement distributes 1 resource.
     *
     * @return The resource distribution amount (1).
     */
    @Override
    public int getResourceDistributionAmount() {
        return 1;
    }


    /**
     * Gets the map of resource types and amounts required to build a Settlement.
     *
     * @return A map where keys are {@link TileType} (WOOD, CLAY, WHEAT, SHEEP) and values are 1.
     */
    @Override
    public Map<TileType, Integer> getRequiredResources() {
        return Map.of(
                TileType.WOOD, 1,
                TileType.CLAY, 1,
                TileType.WHEAT, 1,
                TileType.SHEEP, 1
        );
    }

    @Override
    public int getBuildLimit() {
        return 5;
    }
}