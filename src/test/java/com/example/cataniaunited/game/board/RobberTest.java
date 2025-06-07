package com.example.cataniaunited.game.board;

import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.PlayerService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RobberTest {

    @Inject
    GameService gameService;

    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    @Test
    void testRobberStartsOnDesert() {
        GameBoard board = new GameBoard(4);
        Tile desert = board.getTileList().stream()
                .filter(t -> t.getType() == TileType.DESERT)
                .findFirst()
                .orElseThrow();

        assertTrue(desert.hasRobber());
        assertEquals(desert.getId(), board.getRobberTileId());
    }

}
