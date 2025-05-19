package com.example.cataniaunited.game.board.ports;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public class GeneralPort extends Port {

    public GeneralPort() {
        super(3); // A general Port is 3:1
    }

    @Override
    public boolean canTrade(List<TileType> offeredResources, List<TileType> desiredResources) {
        // trade ratio must be valid
        if (tradeRatioIsInvalid(offeredResources, desiredResources)) {
            return false;
        }

        // For generic ports, each offered resource type must be bundled correctly.
        if (!offeredResourcesComeInValidBundles(offeredResources, desiredResources.size())) {
            return false;
        }

        // Check if player is trying to trade for resources they are offering
        return !isTradingForOfferedResources(offeredResources, desiredResources);
    }

    private boolean offeredResourcesComeInValidBundles(List<TileType> offeredResources, int expectedOutputBundleCount) {
        Map<TileType, Long> offeredResourceMap = offeredResources.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        int calculatedOutputBundles = 0;
        for (Map.Entry<TileType, Long> entry : offeredResourceMap.entrySet()) {
            long countOfEntryType = entry.getValue();
            if (countOfEntryType % this.inputResourceAmount != 0) {
                return false; // This specific resource type is not bundled correctly (i.e. can't trade 1 sheep + 2 wheat for one Ore)
            }
            calculatedOutputBundles += (int) (countOfEntryType / this.inputResourceAmount);
        }
        
        // Sum of trade bundles equals the total expected bundles.
        return calculatedOutputBundles == expectedOutputBundleCount;
    }
}