package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;

import java.util.Map;

public class Settlement extends Building {
    public Settlement(Player player, PlayerColor color) throws GameException {
        super(player, color);
    }

    @Override
    public void distributeResourcesToPlayer(TileType type) {
        super.player.getResource(type, 1);
    }

    @Override
    public Map<TileType, Integer> getRequiredResources() {
        return Map.of(
                TileType.WOOD, 1,
                TileType.CLAY, 1,
                TileType.WHEAT, 1,
                TileType.SHEEP, 1
        );
    }
}
