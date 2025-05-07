package com.example.cataniaunited.game.buildings;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import jakarta.inject.Inject;

public class City extends Building {

    @Inject
    PlayerService playerService;

    public City(String playerId, PlayerColor color) throws GameException {
        super(playerId, color);
    }


    @Override
    public void distributeResourcesToPlayer(TileType type) {
        playerService.getPlayerById(super.ownerPlayerId).getResource(type, 2);
    }
}
