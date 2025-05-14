package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.Buildable;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Abstract base class for buildings in the Catan game, such as settlements and cities.
 * Implements {@link Buildable} to define resource costs.
 * Manages owner, color, and resource distribution.
 */
public abstract class Building implements Buildable {
    protected final Player player;
    protected final PlayerColor color;

    /**
     * Constructs a new Building.
     *
     * @param player The {@link Player} who owns this building.
     * @param color  The {@link PlayerColor} of this building.
     * @throws GameException if the player or color is null, or if the player's ID is empty.
     */
    protected Building(Player player, PlayerColor color) throws GameException {
        if (player == null || Util.isEmpty(player.getUniqueId())) {
            throw new GameException("Owner of building must not be empty");
        }
        if (color == null) {
            throw new GameException("Color of building must not be null");
        }

        this.player = player;
        this.color = color;
    }

    /**
     * Gets the player who owns this building.
     *
     * @return The owning {@link Player}.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Converts the building's state to a JSON representation.
     * Includes owner ID, color hex code, and building type (class name).
     *
     * @return An {@link ObjectNode} representing the building in JSON format.
     */
    public ObjectNode toJson() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("owner", this.player.getUniqueId());
        result.put("color", this.color.getHexCode());
        result.put("type", this.getClass().getSimpleName());
        return result;
    }

    /**
     * Distributes resources of a specific type to the owner of this building.
     * The amount distributed is determined by {@link #getResourceDistributionAmount()}.
     *
     * @param type The {@link TileType} of the resource to distribute.
     */
    public void distributeResourcesToPlayer(TileType type){
        this.player.receiveResource(type, getResourceDistributionAmount());
    }

    /**
     * Abstract method to get the amount of resources this building distributes
     * when an adjacent tile produces.
     *
     * @return The number of resources to distribute (e.g., 1 for settlement, 2 for city).
     */
    public abstract int getResourceDistributionAmount();

}