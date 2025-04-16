package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.util.Util;

public abstract class Building {
    protected final String ownerPlayerId;

    public Building(String playerId) throws GameException {
        if(Util.isEmpty(playerId)) {
            throw new GameException("Owner Id of building must not be empty");
        }
        this.ownerPlayerId = playerId;
    }

    public String getOwnerPlayerId() {
        return ownerPlayerId;
    }
}
