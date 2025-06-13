package com.example.cataniaunited.game.trade;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;

import java.util.List;

/**
 * A record to represent a player's trade request.
 * This is an immutable data carrier used to pass trade information
 * from the API/message handling layer to the business logic layer.
 *
 * @param offeredResources The list of resources the player is offering.
 * @param targetResources  The list of resources the player wishes to receive.
 */
public record TradeRequest(
        List<TileType> offeredResources,
        List<TileType> targetResources
) {
}