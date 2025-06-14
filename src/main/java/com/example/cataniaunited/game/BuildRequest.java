package com.example.cataniaunited.game;

import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;

public record BuildRequest(
        Player player,
        PlayerColor color,
        int positionId,
        boolean requiresResourceCheck
) {
}
