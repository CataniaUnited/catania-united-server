package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class Building {
    protected final String ownerPlayerId;
    protected final PlayerColor color;

    protected Building(String playerId, PlayerColor color) throws GameException {
        if (Util.isEmpty(playerId)) {
            throw new GameException("Owner Id of building must not be empty");
        }

        if (color == null) {
            throw new GameException("Color of building must not be null");
        }

        this.ownerPlayerId = playerId;
        this.color = color;
    }

    public String getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public ObjectNode toJson() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("owner", this.ownerPlayerId);
        result.put("color", this.color.getHexCode());
        result.put("type", this.getClass().getSimpleName());
        return result;
    }

    public abstract void distributeResourcesToPlayer(TileType type);

}
