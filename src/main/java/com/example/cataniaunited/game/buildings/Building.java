package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class Building {
    protected final Player player;
    protected final PlayerColor color;

    protected Building(Player player, PlayerColor color) throws GameException {
        if (player == null || player.getUniqueId() == null || player.getUniqueId().isEmpty()) {
            throw new GameException("Owner of building must not be empty");
        }
        if (color == null) {
            throw new GameException("Color of building must not be null");
        }

        this.player = player;
        this.color = color;
    }

    public Player getPlayer() {
        return player;
    }

    public ObjectNode toJson() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("owner", this.player.getUniqueId());
        result.put("color", this.color.getHexCode());
        result.put("type", this.getClass().getSimpleName());
        return result;
    }

    public abstract void distributeResourcesToPlayer(TileType type);

}
