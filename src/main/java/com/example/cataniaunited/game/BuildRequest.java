package com.example.cataniaunited.game;

import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;

import java.util.Optional;

public record BuildRequest(
        Player player,
        PlayerColor color,
        int positionId,
        boolean isSetupRound,
        Optional<Integer> maximumStructureCount
) {

    public BuildRequest(Player player, PlayerColor color, int positionId, boolean isSetupRound, int maximumStructureCount) {
        this(player, color, positionId, isSetupRound, Optional.of(maximumStructureCount));
    }
}
