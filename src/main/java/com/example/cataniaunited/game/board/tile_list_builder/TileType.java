package com.example.cataniaunited.game.board.tile_list_builder;

public enum TileType {
    WHEAT(2),
    SHEEP(2),
    WOOD(4),
    CLAY(4),
    ORE(0),
    WASTE(0); // WASTE NEEDS TO BE AT THE LAST INDEX (for StandardTileListBuilder logic)!!! // fixme then definitely change this constraint in the builder

    private final int initialAmount;

    TileType(int initialAmount) {
        this.initialAmount = initialAmount;
    }

    /**
     * Returns the initial amount of Resources a Player gets when starting the game,
     * to allow him to build two Settlements and two roads.
     *
     * @return The initial amount.
     */
    public int getInitialAmount() {
        return initialAmount;
    }
}