package com.example.cataniaunited.game;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.Map;

public interface Buildable {
    Map<TileType, Integer> getRequiredResources();
}
