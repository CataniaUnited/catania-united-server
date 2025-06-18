package com.example.cataniaunited.game.trade;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.Map;

/**
 * A record to represent a player's trade request.
 *
 * @param offeredResources A map where the key is the resource type the player is offering,
 *                         and the value is the quantity.
 * @param targetResources  A map where the key is the resource type the player wishes to receive,
 *                         and the value is the quantity.
 */
public record TradeRequest(
        Map<TileType, Integer> offeredResources,
        Map<TileType, Integer> targetResources
) {
}