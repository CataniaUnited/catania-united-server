package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.player.PlayerColor;

public class Settlement extends Building {

    public Settlement(String playerId, PlayerColor color) throws GameException {
        super(playerId, color);
    }
}
