package com.example.cataniaunited.game.board.tile_list_builder;

public enum TileType {
    WHEAT(2),
    SHEEP(2),
    WOOD(4),
    CLAY(4),
    ORE(0),
    WASTE(0); // WASTE NEEDS TO BE AT THE LAST INDEX!!!

    private final int initialAmount;

    TileType(int initialAmount) {
        this.initialAmount = initialAmount;
    }

    public int getInitialAmount() {
        return initialAmount;
    }
}
